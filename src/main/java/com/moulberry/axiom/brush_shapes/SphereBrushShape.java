package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface SphereBrushShape extends BrushShape {
   static SphereBrushShape create(int radius) {
      return new SphereBrushShape.SimpleSphereBrushShape(radius);
   }

   static SphereBrushShape create(int radiusX, int radiusY, int radiusZ) {
      return (SphereBrushShape)(radiusX == radiusY && radiusX == radiusZ
         ? create(radiusX)
         : new SphereBrushShape.SeparateSphereBrushShape(radiusX, radiusY, radiusZ));
   }

   static SphereBrushShape create(int radiusX, int radiusY, int radiusZ, Quaternionf quaternionf) {
      if (radiusX == radiusY && radiusX == radiusZ) {
         return create(radiusX);
      } else {
         return (SphereBrushShape)(BrushShape.isQuaternionIdentity(quaternionf)
            ? create(radiusX, radiusY, radiusZ)
            : new SphereBrushShape.RotatedSphereBrushShape(radiusX, radiusY, radiusZ, quaternionf));
      }
   }

   public static final class RotatedSphereBrushShape implements SphereBrushShape {
      private final float invRadiusSqX;
      private final float invRadiusSqY;
      private final float invRadiusSqZ;
      private final Box boundingBox;
      private final Quaternionf quaternionf;
      private final Vector3f tempVector = new Vector3f();

      public RotatedSphereBrushShape(int radiusX, int radiusY, int radiusZ, Quaternionf quaternionf) {
         int minX = 0;
         int minY = 0;
         int minZ = 0;
         int maxX = 0;
         int maxY = 0;
         int maxZ = 0;
         Vector3f vector3f = new Vector3f();

         for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
               for (int z = -1; z <= 1; z += 2) {
                  vector3f.set(radiusX * x, radiusY * y, radiusZ * z);
                  quaternionf.transformInverse(vector3f);
                  if (vector3f.x < 0.0F) {
                     minX = Math.min(minX, (int)Math.floor(vector3f.x));
                  } else {
                     maxX = Math.max(maxX, (int)Math.ceil(vector3f.x));
                  }

                  if (vector3f.y < 0.0F) {
                     minY = Math.min(minY, (int)Math.floor(vector3f.y));
                  } else {
                     maxY = Math.max(maxY, (int)Math.ceil(vector3f.y));
                  }

                  if (vector3f.z < 0.0F) {
                     minZ = Math.min(minZ, (int)Math.floor(vector3f.z));
                  } else {
                     maxZ = Math.max(maxZ, (int)Math.ceil(vector3f.z));
                  }
               }
            }
         }

         this.boundingBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);
         this.quaternionf = quaternionf;
         float invRadiusSqX = 1.0F / (radiusX * radiusX + radiusX);
         float invRadiusSqY = 1.0F / (radiusY * radiusY + radiusY);
         float invRadiusSqZ = 1.0F / (radiusZ * radiusZ + radiusZ);
         if (!Float.isFinite(invRadiusSqX)) {
            invRadiusSqX = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusSqY)) {
            invRadiusSqY = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusSqZ)) {
            invRadiusSqZ = Float.MAX_VALUE;
         }

         this.invRadiusSqX = invRadiusSqX;
         this.invRadiusSqY = invRadiusSqY;
         this.invRadiusSqZ = invRadiusSqZ;
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         this.tempVector.set(x, y, z);
         this.quaternionf.transform(this.tempVector);
         float rx = this.tempVector.x;
         float ry = this.tempVector.y;
         float rz = this.tempVector.z;
         return rx * rx * this.invRadiusSqX + ry * ry * this.invRadiusSqY + rz * rz * this.invRadiusSqZ <= 1.0F;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         this.tempVector.set(x, y, z);
         this.quaternionf.transform(this.tempVector);
         float rx = this.tempVector.x;
         float ry = this.tempVector.y;
         float rz = this.tempVector.z;
         return rx * rx * this.invRadiusSqX + ry * ry * this.invRadiusSqY + rz * rz * this.invRadiusSqZ;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            SphereBrushShape.RotatedSphereBrushShape that = (SphereBrushShape.RotatedSphereBrushShape)o;
            if (Float.compare(that.invRadiusSqX, this.invRadiusSqX) != 0) {
               return false;
            } else if (Float.compare(that.invRadiusSqY, this.invRadiusSqY) != 0) {
               return false;
            } else {
               return Float.compare(that.invRadiusSqZ, this.invRadiusSqZ) != 0 ? false : this.quaternionf.equals(that.quaternionf);
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.invRadiusSqX != 0.0F ? Float.floatToIntBits(this.invRadiusSqX) : 0;
         result = 31 * result + (this.invRadiusSqY != 0.0F ? Float.floatToIntBits(this.invRadiusSqY) : 0);
         result = 31 * result + (this.invRadiusSqZ != 0.0F ? Float.floatToIntBits(this.invRadiusSqZ) : 0);
         return 31 * result + this.quaternionf.hashCode();
      }
   }

   public static final class SeparateSphereBrushShape implements SphereBrushShape {
      private final float invMaxRadiusSqX;
      private final float invMaxRadiusSqY;
      private final float invMaxRadiusSqZ;
      private final Box boundingBox;

      public SeparateSphereBrushShape(int radiusX, int radiusY, int radiusZ) {
         this.boundingBox = new Box(-radiusX, -radiusY, -radiusZ, radiusX, radiusY, radiusZ);
         float invMaxRadiusSqX = 1.0F / (radiusX * radiusX + radiusX);
         float invMaxRadiusSqY = 1.0F / (radiusY * radiusY + radiusY);
         float invMaxRadiusSqZ = 1.0F / (radiusZ * radiusZ + radiusZ);
         if (!Float.isFinite(invMaxRadiusSqX)) {
            invMaxRadiusSqX = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invMaxRadiusSqY)) {
            invMaxRadiusSqY = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invMaxRadiusSqZ)) {
            invMaxRadiusSqZ = Float.MAX_VALUE;
         }

         this.invMaxRadiusSqX = invMaxRadiusSqX;
         this.invMaxRadiusSqY = invMaxRadiusSqY;
         this.invMaxRadiusSqZ = invMaxRadiusSqZ;
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return x * x * this.invMaxRadiusSqX + y * y * this.invMaxRadiusSqY + z * z * this.invMaxRadiusSqZ <= 1.0F;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         return x * x * this.invMaxRadiusSqX + y * y * this.invMaxRadiusSqY + z * z * this.invMaxRadiusSqZ;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            SphereBrushShape.SeparateSphereBrushShape that = (SphereBrushShape.SeparateSphereBrushShape)o;
            if (Float.compare(that.invMaxRadiusSqX, this.invMaxRadiusSqX) != 0) {
               return false;
            } else {
               return Float.compare(that.invMaxRadiusSqY, this.invMaxRadiusSqY) != 0 ? false : Float.compare(that.invMaxRadiusSqZ, this.invMaxRadiusSqZ) == 0;
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.invMaxRadiusSqX != 0.0F ? Float.floatToIntBits(this.invMaxRadiusSqX) : 0;
         result = 31 * result + (this.invMaxRadiusSqY != 0.0F ? Float.floatToIntBits(this.invMaxRadiusSqY) : 0);
         return 31 * result + (this.invMaxRadiusSqZ != 0.0F ? Float.floatToIntBits(this.invMaxRadiusSqZ) : 0);
      }
   }

   public static final class SimpleSphereBrushShape implements SphereBrushShape {
      private final int maxRadiusSq;
      private final float invMaxRadiusSq;
      private final Box boundingBox;

      public SimpleSphereBrushShape(int radius) {
         this.maxRadiusSq = radius * radius + radius;
         this.invMaxRadiusSq = 1.0F / this.maxRadiusSq;
         this.boundingBox = new Box(-radius, -radius, -radius, radius, radius, radius);
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         return x * x + y * y + z * z <= this.maxRadiusSq;
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         return (x * x + y * y + z * z) * this.invMaxRadiusSq;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            SphereBrushShape.SimpleSphereBrushShape that = (SphereBrushShape.SimpleSphereBrushShape)o;
            return this.maxRadiusSq == that.maxRadiusSq;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.maxRadiusSq;
      }
   }
}
