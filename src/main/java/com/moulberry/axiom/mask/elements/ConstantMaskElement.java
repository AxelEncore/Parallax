package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class ConstantMaskElement implements MaskElement {
   private final boolean constant;

   public ConstantMaskElement(boolean constant) {
      this.constant = constant;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return this.constant;
   }

   public boolean getConstant() {
      return this.constant;
   }
}
