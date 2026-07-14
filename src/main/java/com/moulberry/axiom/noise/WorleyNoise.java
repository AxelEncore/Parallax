package com.moulberry.axiom.noise;

public class WorleyNoise implements NoiseInterface {
   private final int seed;
   private final float jitter;
   private final float firstFactor;
   private final float secondFactor;
   private final float thirdFactor;

   public WorleyNoise(long seed, float jitter, float firstFactor, float secondFactor, float thirdFactor) {
      this.seed = Long.hashCode(seed);
      this.jitter = jitter;
      this.firstFactor = firstFactor;
      this.secondFactor = secondFactor;
      this.thirdFactor = thirdFactor;
   }

   @Override
   public float evaluate(double x, double y) {
      double floorX = Math.floor(x);
      double floorY = Math.floor(y);
      int cellX = (int)floorX;
      int cellY = (int)floorY;
      float fracX = (float)(x - floorX);
      float fracY = (float)(y - floorY);
      int xPrimed = (cellX - 2) * 501125321;
      int yPrimedBase = (cellY - 2) * 1136930381;
      float closestSq = Float.MAX_VALUE;
      float secondClosestSq = Float.MAX_VALUE;
      float thirdClosestSq = Float.MAX_VALUE;

      for (int xo = -2; xo <= 2; xo++) {
         int yPrimed = yPrimedBase;

         for (int yo = -2; yo <= 2; yo++) {
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
            float distanceSq = offsetX * offsetX + offsetY * offsetY;
            distanceSq *= distanceSq;
            if (distanceSq < closestSq) {
               thirdClosestSq = secondClosestSq;
               secondClosestSq = closestSq;
               closestSq = distanceSq;
            } else if (distanceSq < secondClosestSq) {
               thirdClosestSq = secondClosestSq;
               secondClosestSq = distanceSq;
            } else if (distanceSq < thirdClosestSq) {
               thirdClosestSq = distanceSq;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      float closest = (float)Math.sqrt(closestSq);
      float secondClosest = (float)Math.sqrt(secondClosestSq);
      float thirdClosest = (float)Math.sqrt(thirdClosestSq);
      float result = thirdClosest * this.thirdFactor + secondClosest * this.secondFactor + closest * this.firstFactor;
      return Math.max(0.0F, Math.min(1.0F, result));
   }

   @Override
   public float evaluate(double x, double y, double z) {
      double floorX = Math.floor(x);
      double floorY = Math.floor(y);
      double floorZ = Math.floor(z);
      int cellX = (int)floorX;
      int cellY = (int)floorY;
      int cellZ = (int)floorZ;
      float fracX = (float)(x - floorX);
      float fracY = (float)(y - floorY);
      float fracZ = (float)(z - floorZ);
      float closestSq = Float.MAX_VALUE;
      float secondClosestSq = Float.MAX_VALUE;
      float thirdClosestSq = Float.MAX_VALUE;
      int xPrimed = (cellX - 2) * 501125321;
      int yPrimedBase = (cellY - 2) * 1136930381;
      int zPrimedBase = (cellZ - 2) * 1720413743;

      for (int xo = -2; xo <= 2; xo++) {
         int yPrimed = yPrimedBase;

         for (int yo = -2; yo <= 2; yo++) {
            int zPrimed = zPrimedBase;

            for (int zo = -2; zo <= 2; zo++) {
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
               float distanceSq = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
               if (distanceSq < closestSq) {
                  thirdClosestSq = secondClosestSq;
                  secondClosestSq = closestSq;
                  closestSq = distanceSq;
               } else if (distanceSq < secondClosestSq) {
                  thirdClosestSq = secondClosestSq;
                  secondClosestSq = distanceSq;
               } else if (distanceSq < thirdClosestSq) {
                  thirdClosestSq = distanceSq;
               }

               zPrimed += 1720413743;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      float closest = (float)Math.sqrt(closestSq);
      float secondClosest = (float)Math.sqrt(secondClosestSq);
      float thirdClosest = (float)Math.sqrt(thirdClosestSq);
      float result = thirdClosest * this.thirdFactor + secondClosest * this.secondFactor + closest * this.firstFactor;
      return Math.max(0.0F, Math.min(1.0F, result));
   }
}
