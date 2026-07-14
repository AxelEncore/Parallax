package com.moulberry.axiom.collections;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FlowCache<T, U> {
   private final int minCapacity;
   private final int minAge;
   private final int inflowPerTick;
   private final int outflowPerTick;
   private final boolean cleanAutoCloseables;
   private final Function<T, U> function;
   private final Map<T, FlowCache<T, U>.Slot> map = new HashMap<>();
   private final Set<T> pending = new HashSet<>();
   private FlowCache<T, U>.Slot head;
   private FlowCache<T, U>.Slot tail;
   private long currentTick = 0L;

   public FlowCache(int minCapacity, int minAge, int inflowPerTick, int outflowPerTick, boolean cleanAutoCloseables, Function<T, U> function) {
      this.minCapacity = minCapacity;
      this.minAge = minAge;
      this.inflowPerTick = inflowPerTick;
      this.outflowPerTick = outflowPerTick;
      this.cleanAutoCloseables = cleanAutoCloseables;
      this.function = function;
   }

   public void clear() {
      this.map.clear();
      this.pending.clear();
      this.head = null;
      this.tail = null;
      this.currentTick = 0L;
   }

   public U remove(T t) {
      this.pending.remove(t);
      FlowCache<T, U>.Slot slot = this.map.remove(t);
      if (slot != null) {
         slot.unlink();
         return slot.value;
      } else {
         return null;
      }
   }

   public U get(T t) {
      FlowCache<T, U>.Slot slot = this.map.get(t);
      if (slot == null) {
         if (this.pending.size() >= this.inflowPerTick) {
            return null;
         } else {
            this.pending.add(t);
            return null;
         }
      } else {
         slot.lastRequestedTick = this.currentTick;
         if (slot != this.head) {
            slot.unlink();
            slot.next = this.head;
            this.head.previous = slot;
            this.head = slot;
         }

         return slot.value;
      }
   }

   public void tick() {
      this.currentTick++;
      this.pending.forEach(key -> {
         U value = this.function.apply((T)key);
         FlowCache<T, U>.Slot slot = new FlowCache.Slot((T)key, value, this.currentTick);
         if (this.head == null) {
            this.head = slot;
            this.tail = slot;
         } else {
            this.head.previous = slot;
            slot.next = this.head;
            this.head = slot;
         }

         this.map.put((T)key, slot);
      });
      this.pending.clear();
      int outflow = 0;

      while (outflow < this.outflowPerTick && this.map.size() > this.minCapacity) {
         outflow++;
         FlowCache<T, U>.Slot toRemove = this.tail;
         long age = this.currentTick - toRemove.lastRequestedTick;
         if (age >= -10L && age <= this.minAge) {
            break;
         }

         toRemove.unlink();
         FlowCache<T, U>.Slot removedSlot = this.map.remove(toRemove.key);
         if (removedSlot != toRemove) {
            throw new FaultyImplementationError();
         }

         if (this.cleanAutoCloseables && removedSlot.value instanceof AutoCloseable autoCloseable) {
            try {
               autoCloseable.close();
            } catch (Exception var8) {
               throw new RuntimeException(var8);
            }
         }
      }
   }

   private class Slot {
      private FlowCache<T, U>.Slot previous;
      private FlowCache<T, U>.Slot next;
      private final T key;
      private final U value;
      private long lastRequestedTick;

      public Slot(T key, U value, long lastRequestedTick) {
         this.key = key;
         this.value = value;
         this.lastRequestedTick = lastRequestedTick;
      }

      private void unlink() {
         if (this == FlowCache.this.head && this == FlowCache.this.tail) {
            FlowCache.this.head = null;
            FlowCache.this.tail = null;
         } else if (this == FlowCache.this.head) {
            this.next.previous = null;
            FlowCache.this.head = this.next;
         } else if (this == FlowCache.this.tail) {
            this.previous.next = null;
            FlowCache.this.tail = this.previous;
         } else {
            this.next.previous = this.previous;
            this.previous.next = this.next;
         }

         this.previous = null;
         this.next = null;
      }
   }
}
