package com.moulberry.axiom.tools.distort;

import com.moulberry.axiom.noise.NoiseHelper;
import net.minecraft.world.phys.Vec3;

public class NoiseVectorField {
   private final int seed;

   public NoiseVectorField(long seed) {
      this.seed = Long.hashCode(seed);
   }

   public Vec3 evaluate(double x, double y, double z) {
      double floorX = Math.floor(x);
      double floorY = Math.floor(y);
      double floorZ = Math.floor(z);
      int cellX = (int)floorX;
      int cellY = (int)floorY;
      int cellZ = (int)floorZ;
      float fracX = (float)(x - floorX);
      float fracY = (float)(y - floorY);
      float fracZ = (float)(z - floorZ);
      int xPrimed = (cellX - 1) * 501125321;
      int yPrimedBase = (cellY - 1) * 1136930381;
      int zPrimedBase = (cellZ - 1) * 1720413743;
      float totalX = 0.0F;
      float totalY = 0.0F;
      float totalZ = 0.0F;

      for (int xo = -1; xo <= 1; xo++) {
         int yPrimed = yPrimedBase;

         for (int yo = -1; yo <= 1; yo++) {
            int zPrimed = zPrimedBase;

            for (int zo = -1; zo <= 1; zo++) {
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
               float offsetX = Math.min(1.0F, Math.abs(xo - fracX + 0.5F));
               float offsetY = Math.min(1.0F, Math.abs(yo - fracY + 0.5F));
               float offsetZ = Math.min(1.0F, Math.abs(zo - fracZ + 0.5F));
               float weight = (1.0F - offsetX) * (1.0F - offsetY) * (1.0F - offsetZ);
               totalX = (float)(totalX + (rx * 2.0 - 1.0) * weight);
               totalY = (float)(totalY + (ry * 2.0 - 1.0) * weight);
               totalZ = (float)(totalZ + (rz * 2.0 - 1.0) * weight);
               zPrimed += 1720413743;
            }

            yPrimed += 1136930381;
         }

         xPrimed += 501125321;
      }

      float totalSq = totalX * totalX + totalY * totalY + totalZ * totalZ;
      if (totalSq == 0.0F) {
         return new Vec3(0.0, 1.0, 0.0);
      } else {
         float total = (float)Math.sqrt(totalSq);
         return new Vec3(totalX / total, totalY / total, totalZ / total);
      }
   }
}
