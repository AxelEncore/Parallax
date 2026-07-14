package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class SurfaceMaskElement implements MaskElement, GenericSingleMaskElement {
   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      if (!context.getBlockState(x, y, z, 0, 0, 0).blocksMotion()) {
         return false;
      } else if (!context.getBlockState(x, y, z, -1, 0, 0).blocksMotion()) {
         return true;
      } else if (!context.getBlockState(x, y, z, 1, 0, 0).blocksMotion()) {
         return true;
      } else if (!context.getBlockState(x, y, z, 0, -1, 0).blocksMotion()) {
         return true;
      } else if (!context.getBlockState(x, y, z, 0, 1, 0).blocksMotion()) {
         return true;
      } else {
         return !context.getBlockState(x, y, z, 0, 0, -1).blocksMotion() ? true : !context.getBlockState(x, y, z, 0, 0, 1).blocksMotion();
      }
   }

   @Override
   public String cmdStringName() {
      return "surface";
   }
}
