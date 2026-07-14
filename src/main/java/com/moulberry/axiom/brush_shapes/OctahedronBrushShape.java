package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;

public interface OctahedronBrushShape extends BrushShape {
   static OctahedronBrushShape create(int radius) {
      return new OctahedronBrushShape.SimpleOctahedronBrushShape(radius);
   }

   public static final class SimpleOctahedronBrushShape implements OctahedronBrushShape {
      private final int size;
      private final float invSize;
      private final Box boundingBox;

      public SimpleOctahedronBrushShape(int size) {
         this.size = size;
         this.invSize = 1.0F / size;
         this.boundingBox = new Box(-size, -size, -size, size, size, size);
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return Math.abs(x) + Math.abs(y) + Math.abs(z) <= this.size;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         float sdf = this.sdf(x, y, z);
         return sdf * sdf;
      }

      @Override
      public float sdf(int x, int y, int z) {
         return (Math.abs(x) + Math.abs(y) + Math.abs(z)) * this.invSize;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            OctahedronBrushShape.SimpleOctahedronBrushShape that = (OctahedronBrushShape.SimpleOctahedronBrushShape)o;
            return this.size == that.size;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.size;
      }
   }
}
