package com.moulberry.axiom.noise;

public class VoronoiEdgesNoise implements NoiseInterface {
   private final int seed;
   private final float jitter;

   public VoronoiEdgesNoise(long seed, float jitter) {
      this.seed = Long.hashCode(seed);
      this.jitter = jitter;
   }

   @Override
   public float evaluate(double x, double y) {
      double floorX = Math.floor(x);
      double floorY = Math.floor(y);
      int cellX = (int)floorX;
      int cellY = (int)floorY;
      float fracX = (float)(x - floorX);
      float fracY = (float)(y - floorY);
      float closestSq = Float.MAX_VALUE;
      int closestCellOffsetX = 0;
      int closestCellOffsetY = 0;
      float closestOffsetX = 0.0F;
      float closestOffsetY = 0.0F;
      int xPrimed = (cellX - 1) * 501125321;
      int yPrimedBase = (cellY - 1) * 1136930381;

      for (int xo = -1; xo <= 1; xo++) {
         int yPrimed = yPrimedBase;

         for (int yo = -1; yo <= 1; yo++) {
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
            if (distanceSq < closestSq) {
               closestSq = distanceSq;
               closestCellOffsetX = xo;
               closestCellOffsetY = yo;
               closestOffsetX = offsetX;
               closestOffsetY = offsetY;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      xPrimed = (cellX + closestCellOffsetX - 2) * 501125321;
      yPrimedBase = (cellY + closestCellOffsetY - 2) * 1136930381;
      float result = Float.MAX_VALUE;

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
            float offsetX = closestCellOffsetX + xo + (float)rx - fracX;
            float offsetY = closestCellOffsetY + yo + (float)ry - fracY;
            float dirDiffX = offsetX - closestOffsetX;
            float dirDiffY = offsetY - closestOffsetY;
            float directionScalar = 1.0F / (float)Math.sqrt(dirDiffX * dirDiffX + dirDiffY * dirDiffY);
            float xd = 0.5F * (closestOffsetX + offsetX) * dirDiffX * directionScalar;
            float zd = 0.5F * (closestOffsetY + offsetY) * dirDiffY * directionScalar;
            float d = xd + zd;
            if (d < result) {
               result = d;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      return Math.max(0.0F, Math.min(1.0F, result));
   }

   @Override
   public float evaluate(double x, double y, double z) {
      return evaluateStatic(x, y, z, this.seed, this.jitter);
   }

   public static float evaluateStatic(double x, double y, double z, int seed, float jitter) {
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
      int closestCellOffsetX = 0;
      int closestCellOffsetY = 0;
      int closestCellOffsetZ = 0;
      float closestOffsetX = 0.0F;
      float closestOffsetY = 0.0F;
      float closestOffsetZ = 0.0F;
      int xPrimed = (cellX - 1) * 501125321;
      int yPrimedBase = (cellY - 1) * 1136930381;
      int zPrimedBase = (cellZ - 1) * 1720413743;

      for (int xo = -1; xo <= 1; xo++) {
         int yPrimed = yPrimedBase;

         for (int yo = -1; yo <= 1; yo++) {
            int zPrimed = zPrimedBase;

            for (int zo = -1; zo <= 1; zo++) {
               int hash = NoiseHelper.hash(seed, xPrimed, yPrimed, zPrimed);
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
               rx = (rx - 0.5) * jitter + 0.5;
               ry = (ry - 0.5) * jitter + 0.5;
               rz = (rz - 0.5) * jitter + 0.5;
               float offsetX = xo + (float)rx - fracX;
               float offsetY = yo + (float)ry - fracY;
               float offsetZ = zo + (float)rz - fracZ;
               float distanceSq = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
               if (distanceSq < closestSq) {
                  closestSq = distanceSq;
                  closestCellOffsetX = xo;
                  closestCellOffsetY = yo;
                  closestCellOffsetZ = zo;
                  closestOffsetX = offsetX;
                  closestOffsetY = offsetY;
                  closestOffsetZ = offsetZ;
               }

               zPrimed += 1720413743;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      xPrimed = (cellX + closestCellOffsetX - 2) * 501125321;
      yPrimedBase = (cellY + closestCellOffsetY - 2) * 1136930381;
      zPrimedBase = (cellZ + closestCellOffsetZ - 2) * 1720413743;
      float result = Float.MAX_VALUE;

      for (int xo = -2; xo <= 2; xo++) {
         int yPrimed = yPrimedBase;

         for (int yo = -2; yo <= 2; yo++) {
            int zPrimed = zPrimedBase;

            for (int zo = -2; zo <= 2; zo++) {
               int hash = NoiseHelper.hash(seed, xPrimed, yPrimed, zPrimed);
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
               rx = (rx - 0.5) * jitter + 0.5;
               ry = (ry - 0.5) * jitter + 0.5;
               rz = (rz - 0.5) * jitter + 0.5;
               float offsetX = closestCellOffsetX + xo + (float)rx - fracX;
               float offsetY = closestCellOffsetY + yo + (float)ry - fracY;
               float offsetZ = closestCellOffsetZ + zo + (float)rz - fracZ;
               float dirDiffX = offsetX - closestOffsetX;
               float dirDiffY = offsetY - closestOffsetY;
               float dirDiffZ = offsetZ - closestOffsetZ;
               float directionScalar = 1.0F / (float)Math.sqrt(dirDiffX * dirDiffX + dirDiffY * dirDiffY + dirDiffZ * dirDiffZ);
               float xd = 0.5F * (closestOffsetX + offsetX) * dirDiffX * directionScalar;
               float yd = 0.5F * (closestOffsetY + offsetY) * dirDiffY * directionScalar;
               float zd = 0.5F * (closestOffsetZ + offsetZ) * dirDiffZ * directionScalar;
               float d = xd + yd + zd;
               if (d < result) {
                  result = d;
               }

               zPrimed += 1720413743;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      return Math.max(0.0F, Math.min(1.0F, result));
   }
}
