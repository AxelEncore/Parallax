package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import net.minecraft.core.Direction.Axis;

public class CoordMaskElement implements MaskElement {
   private final Axis axis;
   private final int value;
   private final int comparison;

   public CoordMaskElement(Axis axis, int value, int comparison) {
      this.axis = axis;
      this.value = value;
      this.comparison = comparison;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      int coord = this.axis.choose(x, y, z);

      return switch (this.comparison) {
         case 0 -> coord == this.value;
         case 1 -> coord != this.value;
         case 2 -> coord < this.value;
         case 3 -> coord > this.value;
         case 4 -> coord <= this.value;
         case 5 -> coord >= this.value;
         default -> throw new FaultyImplementationError();
      };
   }

   public int getComparison() {
      return this.comparison;
   }

   public int getValue() {
      return this.value;
   }

   public Axis getAxis() {
      return this.axis;
   }
}
