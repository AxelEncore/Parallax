package com.moulberry.axiom.noise;

public class MetaballNoise implements NoiseInterface {
   private final int seed;
   private final float jitter;
   private final float range;

   public MetaballNoise(long seed, float jitter, float range) {
      this.seed = Long.hashCode(seed);
      this.jitter = jitter;
      this.range = range;
   }

   @Override
   public float evaluate(double x, double y) {
      if (this.range == 0.0F) {
         return 1.0F;
      } else {
         double floorX = Math.floor(x);
         double floorY = Math.floor(y);
         int cellX = (int)floorX;
         int cellY = (int)floorY;
         float fracX = (float)(x - floorX);
         float fracY = (float)(y - floorY);
         float value = 1.0F;
         float rangeReciprocal = 1.0F / this.range;
         int rangeI = (int)Math.ceil(this.range);
         int xPrimed = (cellX - rangeI) * 501125321;
         int yPrimedBase = (cellY - rangeI) * 1136930381;

         for (int xo = -rangeI; xo <= rangeI; xo++) {
            int yPrimed = yPrimedBase;

            for (int yo = -rangeI; yo <= rangeI; yo++) {
               int hash = NoiseHelper.hash(this.seed, xPrimed, yPrimed);
               int idx = hash & 510;
               double rx = NoiseHelper.RAND_VECS_2D[idx];
               double ry = NoiseHelper.RAND_VECS_2D[idx | 1];
               rx += (Math.PI / 10) * xPrimed;
               ry += 0.2718281828459045 * yPrimed;
               rx -= Math.floor(rx);
               ry -= Math.floor(ry);
               rx = (rx - 0.5) * this.jitter + 0.5;
               ry = (ry - 0.5) * this.jitter + 0.5;
               float offsetX = xo + (float)rx - fracX;
               float offsetY = yo + (float)ry - fracY;
               float distance = (float)Math.sqrt(offsetX * offsetX + offsetY * offsetY);
               value = Math.min(value, value * distance * rangeReciprocal);
               yPrimed += 1136930381;
            }

            xPrimed += 501125321;
         }

         return value;
      }
   }

   @Override
   public float evaluate(double x, double y, double z) {
      if (this.range == 0.0F) {
         return 1.0F;
      } else {
         double floorX = Math.floor(x);
         double floorY = Math.floor(y);
         double floorZ = Math.floor(z);
         int cellX = (int)floorX;
         int cellY = (int)floorY;
         int cellZ = (int)floorZ;
         float fracX = (float)(x - floorX);
         float fracY = (float)(y - floorY);
         float fracZ = (float)(z - floorZ);
         float value = 1.0F;
         float rangeReciprocal = 1.0F / this.range;
         int rangeI = (int)Math.ceil(this.range);
         int xPrimed = (cellX - rangeI) * 501125321;
         int yPrimedBase = (cellY - rangeI) * 1136930381;
         int zPrimedBase = (cellZ - rangeI) * 1720413743;

         for (int xo = -rangeI; xo <= rangeI; xo++) {
            int yPrimed = yPrimedBase;

            for (int yo = -rangeI; yo <= rangeI; yo++) {
               int zPrimed = zPrimedBase;

               for (int zo = -rangeI; zo <= rangeI; zo++) {
                  int hash = NoiseHelper.hash(this.seed, xPrimed, yPrimed, zPrimed);
                  int idx = hash & 1020;
                  double rx = NoiseHelper.RAND_VECS_3D[idx];
                  double ry = NoiseHelper.RAND_VECS_3D[idx | 1];
                  double rz = NoiseHelper.RAND_VECS_3D[idx | 2];
                  rx += (Math.PI / 10) * xPrimed;
                  ry += 0.2718281828459045 * yPrimed;
                  rz += 0.4142135623730951 * zPrimed;
                  rx -= Math.floor(rx);
                  ry -= Math.floor(ry);
                  rz -= Math.floor(rz);
                  rx = (rx - 0.5) * this.jitter + 0.5;
                  ry = (ry - 0.5) * this.jitter + 0.5;
                  rz = (rz - 0.5) * this.jitter + 0.5;
                  float offsetX = xo + (float)rx - fracX;
                  float offsetY = yo + (float)ry - fracY;
                  float offsetZ = zo + (float)rz - fracZ;
                  float distance = (float)Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
                  value = Math.min(value, value * distance * rangeReciprocal);
                  zPrimed += 1720413743;
               }

               yPrimed += 1136930381;
            }

            xPrimed += 501125321;
         }

         return value;
      }
   }
}
