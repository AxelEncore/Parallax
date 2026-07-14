package com.moulberry.axiom.collections.list;

public class IntrusiveLinkedElement<E extends IntrusiveLinkedElement<E>> {
   E prev;
   E next;
}
