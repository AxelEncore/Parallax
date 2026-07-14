package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;

public interface FlatDiskBrushShape extends com.moulberry.axiom.brush_shapes.BrushShape {
   static FlatDiskBrushShape create(int radius) {
      return new FlatDiskBrushShape.BrushShape(radius);
   }

   public static final class BrushShape implements FlatDiskBrushShape {
      private final int maxRadiusSq;
      private final float invMaxRadiusSq;
      private final Box boundingBox;

      public BrushShape(int radius) {
         this.maxRadiusSq = radius * radius + radius;
         this.invMaxRadiusSq = 1.0F / this.maxRadiusSq;
         this.boundingBox = new Box(-radius, 0, -radius, radius, 0, radius);
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return y == 0 && x * x + z * z <= this.maxRadiusSq;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         return (x * x + z * z) * this.invMaxRadiusSq;
      }
   }
}
