package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class OffsetMaskElement implements MaskElement {
   private final MaskElement child;
   private final int offsetX;
   private final int offsetY;
   private final int offsetZ;

   public OffsetMaskElement(MaskElement child, int offsetX, int offsetY, int offsetZ) {
      this.child = child;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      context.reset();
      boolean result = this.child.test(context, x + this.offsetX, y + this.offsetY, z + this.offsetZ);
      context.reset();
      return result;
   }

   public MaskElement getChild() {
      return this.child;
   }

   public int getOffsetX() {
      return this.offsetX;
   }

   public int getOffsetY() {
      return this.offsetY;
   }

   public int getOffsetZ() {
      return this.offsetZ;
   }
}
