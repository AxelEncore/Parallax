package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class NotMaskElement implements MaskElement {
   private final MaskElement child;

   public NotMaskElement(MaskElement child) {
      this.child = child;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return !this.child.test(context, x, y, z);
   }

   public MaskElement getChild() {
      return this.child;
   }
}
