package com.moulberry.axiom.collections.list;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class IntrusiveLinkedList<E extends IntrusiveLinkedElement<E>> extends AbstractCollection<E> {
   private int modCount = 0;
   private int size = 0;
   private E head;
   private E tail;

   public E first() {
      return this.head;
   }

   public boolean add(E e) {
      if (e.prev == null && e.next == null) {
         if (BuildConfig.DEBUG && this.head == null != (this.tail == null)) {
            throw new FaultyImplementationError();
         } else {
            if (this.head == null) {
               this.head = this.tail = e;
            } else {
               this.tail.next = e;
               e.prev = this.tail;
               this.tail = e;
            }

            this.size++;
            this.modCount++;
            return true;
         }
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   public boolean remove(Object o) {
      if (!(o instanceof IntrusiveLinkedElement<?> e)) {
         return false;
      } else {
         E next = (E)e.next;
         E prev = (E)e.prev;
         if ((prev != null || e == this.head) && (next != null || e == this.tail)) {
            if (prev == null) {
               this.head = next;
            } else {
               prev.next = next;
               e.prev = null;
            }

            if (next == null) {
               this.tail = prev;
            } else {
               next.prev = prev;
               e.next = null;
            }

            this.size--;
            this.modCount++;
            return true;
         } else {
            throw new IllegalStateException("Tried to remove IntrusiveLinkedElement from list that doesn't contain it");
         }
      }
   }

   @Override
   public boolean contains(Object o) {
      throw new UnsupportedOperationException("Contains is not supported. Too expensive.");
   }

   @Override
   public void clear() {
      this.size = 0;
      this.head = null;
      this.tail = null;
      this.modCount++;
   }

   public void sort(Comparator<E> c) {
      E newHead = this.head;
      int size = 1;

      while (true) {
         int merges = 0;
         E p = newHead;
         E newTail = null;

         while (p != null) {
            merges++;
            E q = p;
            int psize = 0;

            for (int i = 0; i < size; i++) {
               psize++;
               q = q.next;
               if (q == null) {
                  break;
               }
            }

            int qsize = size;

            while (psize > 0 || qsize > 0 && q != null) {
               E e;
               if (psize <= 0 || qsize != 0 && q != null && c.compare(p, q) > 0) {
                  e = q;
                  q = q.next;
                  qsize--;
               } else {
                  e = p;
                  p = p.next;
                  psize--;
               }

               if (newTail != null) {
                  newTail.next = e;
                  e.prev = newTail;
               } else {
                  newHead = e;
                  e.prev = null;
               }

               newTail = e;
            }

            p = q;
         }

         if (newTail != null) {
            newTail.next = null;
         }

         if (merges <= 1) {
            this.head = newHead;
            this.tail = newTail;
            this.modCount++;
            if (BuildConfig.DEBUG) {
            }

            return;
         }

         size *= 2;
      }
   }

   @Override
   public Iterator<E> iterator() {
      return new Iterator<E>() {
         private E lastReturned = (E)null;
         private E next = IntrusiveLinkedList.this.head;
         private int expectedModCount = IntrusiveLinkedList.this.modCount;

         @Override
         public boolean hasNext() {
            return this.next != null;
         }

         public E next() {
            this.checkForComodification();
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               this.lastReturned = this.next;
               this.next = this.next.next;
               return this.lastReturned;
            }
         }

         @Override
         public void remove() {
            this.checkForComodification();
            if (this.lastReturned == null) {
               throw new IllegalStateException();
            } else {
               IntrusiveLinkedList.this.remove(this.lastReturned);
               this.expectedModCount++;
               this.lastReturned = null;
            }
         }

         private void checkForComodification() {
            if (IntrusiveLinkedList.this.modCount != this.expectedModCount) {
               throw new ConcurrentModificationException();
            }
         }
      };
   }

   @Override
   public int size() {
      return this.size;
   }

   public void mergeSplitPosition(IntrusiveLinkedList<E> other, Comparator<E> c, int oldSplit, int newSplit, Consumer<E> headSplit, Consumer<E> tailSplit) {
      E newHead = null;
      E newTail = null;
      int newSize = 0;
      E thisCurrent = this.head;
      E otherCurrent = other.head;
      int psize = this.size;
      int qsize = other.size;

      while (psize > 0 || qsize > 0) {
         E e;
         if (psize > 0 && (qsize == 0 || c.compare(thisCurrent, otherCurrent) <= 0)) {
            boolean headSide = newSize < newSplit;
            if (this.size - psize < oldSplit != headSide) {
               if (headSide) {
                  headSplit.accept(thisCurrent);
               } else {
                  tailSplit.accept(thisCurrent);
               }
            }

            e = thisCurrent;
            thisCurrent = thisCurrent.next;
            psize--;
         } else {
            if (newSize < newSplit) {
               headSplit.accept(otherCurrent);
            } else {
               tailSplit.accept(otherCurrent);
            }

            e = otherCurrent;
            otherCurrent = otherCurrent.next;
            qsize--;
         }

         if (newTail != null) {
            newTail.next = e;
            e.prev = newTail;
         } else {
            newHead = e;
            e.prev = null;
         }

         newSize++;
         newTail = e;
      }

      if (newTail != null) {
         newTail.next = null;
      }

      if (BuildConfig.DEBUG && newSize != this.size + other.size) {
         throw new FaultyImplementationError();
      } else {
         this.head = newHead;
         this.tail = newTail;
         this.size = newSize;
         this.modCount++;
         other.clear();
      }
   }
}
