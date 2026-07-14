package com.moulberry.axiom.funcinterfaces;

@FunctionalInterface
public interface IntIntObjectConsumer<T> {
   void accept(int var1, int var2, T var3);
}
