package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;

public interface CylinderBrushShape extends BrushShape {
   static BrushShape create(int radius, int height) {
      return (BrushShape)(height <= 0 ? FlatDiskBrushShape.create(radius) : new CylinderBrushShape.SimpleCylinderBrushShape(radius, height));
   }

   public static final class SimpleCylinderBrushShape implements CylinderBrushShape {
      private final int maxRadiusSq;
      private final float invMaxRadiusSq;
      private final int heightSq;
      private final float invHeightSq;
      private final Box boundingBox;

      public SimpleCylinderBrushShape(int radius, int height) {
         this.maxRadiusSq = radius * radius + radius;
         this.heightSq = height * height;
         this.boundingBox = new Box(-radius, -height, -radius, radius, height, radius);
         float invMaxRadiusSq = 1.0F / this.maxRadiusSq;
         float invHeightSq = 1.0F / this.heightSq;
         if (!Float.isFinite(invMaxRadiusSq)) {
            invMaxRadiusSq = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invHeightSq)) {
            invHeightSq = Float.MAX_VALUE;
         }

         this.invMaxRadiusSq = invMaxRadiusSq;
         this.invHeightSq = invHeightSq;
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return Math.max(y * y * this.maxRadiusSq, (x * x + z * z) * this.heightSq) <= this.maxRadiusSq * this.heightSq;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         return Math.max(y * y * this.invHeightSq, (x * x + z * z) * this.invMaxRadiusSq);
      }
   }
}
