package com.moulberry.axiom.mask;

import com.moulberry.axiom.exceptions.FaultyImplementationError;

public interface MaskElement {
   int EQ = 0;
   int NEQ = 1;
   int LT = 2;
   int GT = 3;
   int LTEQ = 4;
   int GTEQ = 5;

   static int invertComparison(int comparison) {
      return switch (comparison) {
         case 0 -> 1;
         case 1 -> 0;
         case 2 -> 5;
         case 3 -> 4;
         case 4 -> 3;
         case 5 -> 2;
         default -> throw new FaultyImplementationError();
      };
   }

   boolean test(MaskContext var1, int var2, int var3, int var4);
}
