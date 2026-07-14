package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;

public interface CapsuleBrushShape extends BrushShape {
   static BrushShape create(int radius, int height) {
      return (BrushShape)(height <= 0 ? SphereBrushShape.create(radius) : new CapsuleBrushShape.SimpleCapsuleBrushShape(radius, height));
   }

   public static final class SimpleCapsuleBrushShape implements CapsuleBrushShape {
      private final int maxRadiusSq;
      private final float invMaxRadiusSq;
      private final int cylinderHeight;
      private final int cylinderHeightSq;
      private final Box boundingBox;

      public SimpleCapsuleBrushShape(int radius, int height) {
         this.maxRadiusSq = radius * radius + radius;
         this.cylinderHeight = height;
         this.cylinderHeightSq = this.cylinderHeight * this.cylinderHeight;
         this.boundingBox = new Box(-radius, -height - radius, -radius, radius, height + radius, radius);
         float invMaxRadiusSq = 1.0F / this.maxRadiusSq;
         if (!Float.isFinite(invMaxRadiusSq)) {
            invMaxRadiusSq = Float.MAX_VALUE;
         }

         this.invMaxRadiusSq = invMaxRadiusSq;
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         if (y * y <= this.cylinderHeightSq) {
            return Math.max(y * y * this.maxRadiusSq, (x * x + z * z) * this.cylinderHeightSq) <= this.maxRadiusSq * this.cylinderHeightSq;
         } else {
            y = Math.abs(y) - this.cylinderHeight;
            return x * x + y * y + z * z <= this.maxRadiusSq;
         }
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         if (y * y <= this.cylinderHeightSq) {
            return (x * x + z * z) * this.invMaxRadiusSq;
         } else {
            y = Math.abs(y) - this.cylinderHeight;
            return (x * x + y * y + z * z) * this.invMaxRadiusSq;
         }
      }
   }
}
