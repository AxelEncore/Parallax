package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class EitherMaskElement implements MaskElement {
   private final MaskElement child1;
   private final MaskElement child2;

   public EitherMaskElement(MaskElement child1, MaskElement child2) {
      this.child1 = child1;
      this.child2 = child2;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return this.child1.test(context, x, y, z) || this.child2.test(context, x, y, z);
   }

   public MaskElement getChild1() {
      return this.child1;
   }

   public MaskElement getChild2() {
      return this.child2;
   }
}
