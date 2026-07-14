package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class AnyMaskElement implements MaskElement {
   private final MaskElement[] children;

   public AnyMaskElement(MaskElement[] children) {
      this.children = children;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      for (MaskElement child : this.children) {
         if (child.test(context, x, y, z)) {
            return true;
         }
      }

      return false;
   }

   public MaskElement[] getChildren() {
      return this.children;
   }
}
