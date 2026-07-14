package com.moulberry.axiom.collections;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;

public class JoinedList<E> extends AbstractList<E> {
   private final List<E> first;
   private final List<E> second;

   public JoinedList(List<E> first, List<E> second) {
      this.first = first;
      this.second = second;
   }

   public List<E> first() {
      return this.first;
   }

   public List<E> second() {
      return this.second;
   }

   @Override
   public int size() {
      return this.first.size() + this.second.size();
   }

   @Override
   public boolean isEmpty() {
      return this.first.isEmpty() && this.second.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return this.first.contains(o) || this.second.contains(o);
   }

   @NotNull
   @Override
   public Iterator<E> iterator() {
      return new Iterator<E>() {
         private final Iterator<E> firstIterator = JoinedList.this.first.iterator();
         private final Iterator<E> secondIterator = JoinedList.this.second.iterator();
         private boolean iteratingFirst = true;

         @Override
         public boolean hasNext() {
            if (this.iteratingFirst) {
               boolean hasNext = this.firstIterator.hasNext();
               if (hasNext) {
                  return true;
               }

               this.iteratingFirst = false;
            }

            return this.secondIterator.hasNext();
         }

         @Override
         public E next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               return this.iteratingFirst ? this.firstIterator.next() : this.secondIterator.next();
            }
         }

         @Override
         public void remove() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               if (this.iteratingFirst) {
                  this.firstIterator.remove();
               } else {
                  this.secondIterator.remove();
               }
            }
         }
      };
   }

   @NotNull
   @Override
   public Object[] toArray() {
      Object[] array = this.first.toArray(new Object[this.size()]);
      System.arraycopy(this.second.toArray(), 0, array, this.first.size(), this.second.size());
      return array;
   }

   @NotNull
   @Override
   public <T> T[] toArray(@NotNull T[] a) {
      if (a.length < this.size()) {
         a = (T[])Array.newInstance(a.getClass().getComponentType(), this.size());
      }

      Object[] array = this.first.toArray(a);
      System.arraycopy(this.second.toArray(), 0, array, this.first.size(), this.second.size());
      return (T[])array;
   }

   @Override
   public boolean add(E e) {
      return this.second.add(e);
   }

   @Override
   public boolean remove(Object o) {
      return this.first.remove(o) ? true : this.second.remove(o);
   }

   @Override
   public void clear() {
      this.first.clear();
      this.second.clear();
   }

   @Override
   public E get(int index) {
      return index >= this.first.size() ? this.second.get(index - this.first.size()) : this.first.get(index);
   }

   @Override
   public E set(int index, E element) {
      return index >= this.first.size() ? this.second.set(index - this.first.size(), element) : this.first.set(index, element);
   }

   @Override
   public void add(int index, E element) {
      if (index >= this.first.size()) {
         this.second.add(index - this.first.size(), element);
      } else {
         this.first.add(index, element);
      }
   }

   @Override
   public E remove(int index) {
      return index >= this.first.size() ? this.second.remove(index - this.first.size()) : this.first.remove(index);
   }

   @Override
   public int indexOf(Object o) {
      int firstIndex = this.first.indexOf(o);
      return firstIndex != -1 ? firstIndex : this.second.indexOf(o) + this.first.size();
   }

   @Override
   public int lastIndexOf(Object o) {
      int secondIndex = this.second.lastIndexOf(o);
      return secondIndex != -1 ? secondIndex + this.first.size() : this.first.lastIndexOf(o);
   }
}
