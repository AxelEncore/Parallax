package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.rasterization.RasterizationHelper;
import com.moulberry.axiom.utils.Box;

public interface ConeBrushShape extends BrushShape {
   int CONE_DIRECTION_BOTH = 0;
   int CONE_DIRECTION_UP = 1;
   int CONE_DIRECTION_DOWN = 2;

   static BrushShape create(int radius, int height, float rounding, int coneDirection) {
      if (height <= 0) {
         return FlatDiskBrushShape.create(radius);
      } else {
         if (rounding < 0.0F) {
            rounding = 0.0F;
         } else if (rounding > 1.0F) {
            rounding = 1.0F;
         }

         if (coneDirection < 0) {
            coneDirection = 0;
         } else if (coneDirection > 2) {
            coneDirection = 2;
         }

         return new ConeBrushShape.RoundedConeBrushShape(radius, height, rounding, coneDirection);
      }
   }

   public static final class RoundedConeBrushShape implements ConeBrushShape {
      private final float invMaxRadiusSq;
      private final int radius;
      private final int height;
      private final float invHeight;
      private final Box boundingBox;
      private final float rounding;
      private final float ellipsoidScale;
      private final float ellipsoidOffset;
      private final float ellipsoidRadiusY;
      private final float ellipsoidInvRadiusSqY;
      private final float maxEllipsoidDistance;
      private final int coneDirection;

      public RoundedConeBrushShape(int radius, int height, float rounding, int coneDirection) {
         this.invMaxRadiusSq = 1.0F / ((radius + 0.5F) * (radius + 0.5F));
         this.radius = radius;
         this.height = height;
         this.rounding = rounding;
         this.coneDirection = coneDirection;
         this.invHeight = 1.0F / height;
         this.boundingBox = new Box(-radius, -height, -radius, radius, height, radius);
         float s = 2.0F;
         float mNumer = this.rounding * this.rounding * 12.0F;
         float mDenom = 8.0F;
         this.ellipsoidScale = (float)Math.sqrt(mNumer / mDenom);
         float v = this.rounding * 2.0F - 2.0F + (float)Math.sqrt(2.0F * this.ellipsoidScale * this.ellipsoidScale - 2.0F * this.rounding * this.rounding);
         v = v * this.height / 2.0F + 0.5F;
         this.ellipsoidOffset = v;
         this.ellipsoidRadiusY = this.height / 2.0F * (float)Math.sqrt(2.0) - 0.5F;
         this.ellipsoidInvRadiusSqY = RasterizationHelper.calcInvRadiusSq(this.ellipsoidRadiusY);
         this.maxEllipsoidDistance = RasterizationHelper.distancePointEllipsoid(
            (this.radius + 0.5F) * this.ellipsoidScale,
            (this.ellipsoidRadiusY + 0.5F) * this.ellipsoidScale,
            (this.radius + 0.5F) * this.ellipsoidScale,
            0.0F,
            this.height * (1.0F - this.rounding) + this.ellipsoidOffset,
            0.0F,
            null
         );
      }

      @Override
      public Box boundingBox() {
         return this.boundingBox;
      }

      @Override
      public boolean isInsideShape(int x, int y, int z) {
         if (y != 0 && this.coneDirection != 0 && y > 0 != (this.coneDirection == 1)) {
            return false;
         } else {
            float rhs = 1.0F - Math.abs(y) * this.invHeight;
            if (rhs < 0.0F) {
               return false;
            } else if (rhs < this.rounding) {
               float yo = Math.abs(y) + this.ellipsoidOffset;
               return (x * x + z * z) * this.invMaxRadiusSq <= this.ellipsoidScale * this.ellipsoidScale - yo * yo * this.ellipsoidInvRadiusSqY;
            } else {
               return (x * x + z * z) * this.invMaxRadiusSq <= rhs * rhs;
            }
         }
      }

      @Override
      public float sdfSq(int x, int y, int z) {
         float sdf = this.sdf(x, y, z);
         return sdf * sdf;
      }

      @Override
      public float sdf(int x, int y, int z) {
         float scaledHeight = Math.abs(y) * this.invHeight;
         if (scaledHeight >= 1.0F - this.rounding) {
            float surfaceDistance = RasterizationHelper.distancePointEllipsoid(
               (this.radius + 0.5F) * this.ellipsoidScale,
               (this.ellipsoidRadiusY + 0.5F) * this.ellipsoidScale,
               (this.radius + 0.5F) * this.ellipsoidScale,
               x,
               Math.abs(y) + this.ellipsoidOffset,
               z,
               null
            );
            if (!this.isInsideShape(x, y, z)) {
               surfaceDistance *= -1.0F;
            }

            return surfaceDistance > this.maxEllipsoidDistance
               ? 0.0F
               : 1.0F - this.rounding + this.rounding * (this.maxEllipsoidDistance - surfaceDistance) / this.maxEllipsoidDistance;
         } else {
            return scaledHeight + (float)Math.sqrt((x * x + z * z) * this.invMaxRadiusSq);
         }
      }
   }
}
