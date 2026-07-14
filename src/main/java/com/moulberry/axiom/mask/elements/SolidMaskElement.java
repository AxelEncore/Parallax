package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class SolidMaskElement implements MaskElement {
   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return context.getBlockState(x, y, z).blocksMotion();
   }
}
