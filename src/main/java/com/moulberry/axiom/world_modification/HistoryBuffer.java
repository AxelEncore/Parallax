package com.moulberry.axiom.world_modification;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import org.jetbrains.annotations.Nullable;

public class HistoryBuffer<T> {
   private final List<HistoryEntry<T>> entries = new ArrayList<>();
   private int position = -1;
   private long sizeInBytes = 0L;

   public int getPosition() {
      return this.position;
   }

   public long getSizeInBytes() {
      return this.sizeInBytes;
   }

   @Deprecated
   public void unsafeSetPosition(int position) {
      if (position >= -1 && position <= this.entries.size()) {
         this.position = position;
      } else {
         throw new FaultyImplementationError();
      }
   }

   public int getSize() {
      return this.entries.size();
   }

   @Nullable
   public HistoryEntry<T> getHistoryEntry(int index) {
      return this.entries.get(index);
   }

   public void clear() {
      this.entries.clear();
      this.position = -1;
      this.sizeInBytes = 0L;
   }

   public void push(HistoryEntry<T> entry) {
      while (this.entries.size() - 1 > this.position) {
         HistoryEntry<T> removed = this.entries.remove(this.entries.size() - 1);
         this.sizeInBytes = this.sizeInBytes - removed.bytes();
      }

      this.entries.add(entry);
      this.sizeInBytes = this.sizeInBytes + entry.bytes();
      this.position++;
   }

   public void pushOrMergeIf(HistoryEntry<T> entry, BiPredicate<HistoryEntry<T>, HistoryEntry<T>> condition) {
      while (this.entries.size() - 1 > this.position) {
         HistoryEntry<T> removed = this.entries.remove(this.entries.size() - 1);
         this.sizeInBytes = this.sizeInBytes - removed.bytes();
      }

      if (this.position > 0 && !this.entries.isEmpty()) {
         HistoryEntry<T> last = this.entries.get(this.position);
         if (last.modifiers() == entry.modifiers() && entry.additionalUndoOperation() == null && condition.test(last, entry)) {
            this.entries.remove(this.position);
            this.position--;
            this.sizeInBytes = this.sizeInBytes - last.bytes();
            entry = new HistoryEntry<>(
               entry.forwards(), last.backwards(), entry.origin(), entry.description(), entry.bytes(), entry.modifiers(), last.additionalUndoOperation()
            );
         }
      }

      this.entries.add(entry);
      this.sizeInBytes = this.sizeInBytes + entry.bytes();
      this.position++;
   }

   public boolean canUndo() {
      return this.position >= 0;
   }

   @Nullable
   public HistoryEntry<T> undo(int requiredModifiers) {
      if (!this.canUndo()) {
         return null;
      } else {
         HistoryEntry<T> entry = this.entries.get(this.position);
         if (requiredModifiers != 0 && !entry.hasModifier(requiredModifiers)) {
            return null;
         } else {
            this.position--;
            return entry;
         }
      }
   }

   public boolean canRedo() {
      return this.position < this.entries.size() - 1;
   }

   @Nullable
   public HistoryEntry<T> redo(int requiredModifiers) {
      if (!this.canRedo()) {
         return null;
      } else {
         HistoryEntry<T> entry = this.entries.get(this.position + 1);
         if (requiredModifiers != 0 && !entry.hasModifier(requiredModifiers)) {
            return null;
         } else {
            this.position++;
            return entry;
         }
      }
   }
}
