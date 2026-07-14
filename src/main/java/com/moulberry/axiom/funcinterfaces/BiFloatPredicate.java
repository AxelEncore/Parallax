package com.moulberry.axiom.funcinterfaces;

@FunctionalInterface
public interface BiFloatPredicate {
   BiFloatPredicate TRUE = (x, y) -> true;

   boolean test(float var1, float var2);
}
