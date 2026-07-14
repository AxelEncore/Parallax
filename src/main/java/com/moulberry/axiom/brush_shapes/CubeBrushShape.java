package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;

public interface CubeBrushShape extends BrushShape {
   static CubeBrushShape create(int radius) {
      return new CubeBrushShape.SimpleCubeBrushShape(radius);
   }

   static CubeBrushShape create(int radiusX, int radiusY, int radiusZ) {
      return (CubeBrushShape)(radiusX == radiusY && radiusX == radiusZ ? create(radiusX) : new CubeBrushShape.SeparateCubeBrushShape(radiusX, radiusY, radiusZ));
   }

   public static final class SeparateCubeBrushShape implements CubeBrushShape {
      private final int radiusX;
      private final int radiusY;
      private final int radiusZ;
      private final float invRadiusX;
      private final float invRadiusY;
      private final float invRadiusZ;
      private final Box boundingBox;

      public SeparateCubeBrushShape(int radiusX, int radiusY, int radiusZ) {
         this.radiusX = radiusX;
         this.radiusY = radiusY;
         this.radiusZ = radiusZ;
         this.boundingBox = new Box(-radiusX, -radiusY, -radiusZ, radiusX, radiusY, radiusZ);
         float invRadiusX = 1.0F / this.radiusX;
         float invRadiusY = 1.0F / this.radiusY;
         float invRadiusZ = 1.0F / this.radiusZ;
         if (!Float.isFinite(invRadiusX)) {
            invRadiusX = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusY)) {
            invRadiusY = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusZ)) {
            invRadiusZ = Float.MAX_VALUE;
         }

         this.invRadiusX = invRadiusX;
         this.invRadiusY = invRadiusY;
         this.invRadiusZ = invRadiusZ;
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return Math.abs(x) <= this.radiusX && Math.abs(y) <= this.radiusY && Math.abs(z) <= this.radiusZ;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         return Math.max(
            x * x * this.invRadiusX * this.invRadiusX, Math.max(y * y * this.invRadiusY * this.invRadiusY, z * z * this.invRadiusZ * this.invRadiusZ)
         );
      }

      @Override
      public float sdf(int x, int y, int z) {
         return Math.max(Math.abs(x) * this.invRadiusX, Math.max(Math.abs(y) * this.invRadiusY, Math.abs(z) * this.invRadiusZ));
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            CubeBrushShape.SeparateCubeBrushShape that = (CubeBrushShape.SeparateCubeBrushShape)o;
            if (this.radiusX != that.radiusX) {
               return false;
            } else {
               return this.radiusY != that.radiusY ? false : this.radiusZ == that.radiusZ;
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.radiusX;
         result = 31 * result + this.radiusY;
         return 31 * result + this.radiusZ;
      }
   }

   public static final class SimpleCubeBrushShape implements CubeBrushShape {
      private final int size;
      private final float invSize;
      private final float invSizeSq;
      private final Box boundingBox;

      public SimpleCubeBrushShape(int size) {
         this.size = size;
         this.invSize = 1.0F / size;
         this.invSizeSq = 1.0F / (size * size);
         this.boundingBox = new Box(-size, -size, -size, size, size, size);
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return Math.abs(x) <= this.size && Math.abs(y) <= this.size && Math.abs(z) <= this.size;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         return Math.max(x * x, Math.max(y * y, z * z)) * this.invSizeSq;
      }

      @Override
      public float sdf(int x, int y, int z) {
         return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) * this.invSize;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            CubeBrushShape.SimpleCubeBrushShape that = (CubeBrushShape.SimpleCubeBrushShape)o;
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
