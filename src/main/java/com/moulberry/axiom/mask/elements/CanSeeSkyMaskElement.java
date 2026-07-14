package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class CanSeeSkyMaskElement implements MaskElement, GenericSingleMaskElement {
   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return y >= context.getHighestBlock(x, z);
   }

   @Override
   public String cmdStringName() {
      return "sky";
   }
}
