package com.moulberry.axiom.noise;

public class SimplexNoise implements NoiseInterface {
   private static final float SQRT3 = 1.7320508F;
   private static final float F2 = 0.3660254F;
   private static final float G2 = 0.21132487F;
   private final int seed;

   public SimplexNoise(long seed) {
      this.seed = Long.hashCode(seed);
   }

   @Override
   public float evaluate(double x, double y) {
      double s = (x + y) * 0.3660254F;
      x += s;
      y += s;
      double floorX = Math.floor(x);
      double floorY = Math.floor(y);
      int cellX = (int)floorX;
      int cellY = (int)floorY;
      float fracX = (float)(x - floorX);
      float fracY = (float)(y - floorY);
      cellX *= 501125321;
      cellY *= 1136930381;
      int cellXPlusOne = cellX + 501125321;
      int cellYPlusOne = cellY + 1136930381;
      float t = (fracX + fracY) * 0.21132487F;
      float x0 = fracX - t;
      float y0 = fracY - t;
      float a0 = 0.6666667F - x0 * x0 - y0 * y0;
      float value = a0 * a0 * (a0 * a0) * NoiseHelper.gradCoord(this.seed, cellX, cellY, x0, y0);
      float a1 = 3.1547005F * t + (-0.6666666F + a0);
      float x1 = x0 - 0.57735026F;
      float y1 = y0 - 0.57735026F;
      value += a1 * a1 * (a1 * a1) * NoiseHelper.gradCoord(this.seed, cellXPlusOne, cellYPlusOne, x1, y1);
      float xmyi = fracX - fracY;
      if (t > 0.21132487F) {
         if (fracX + xmyi > 1.0F) {
            float x2 = x0 + -1.3660254F;
            float y2 = y0 + -0.3660254F;
            float a2 = 0.6666667F - x2 * x2 - y2 * y2;
            if (a2 > 0.0F) {
               value += a2 * a2 * (a2 * a2) * NoiseHelper.gradCoord(this.seed, cellX + 1002250642, cellY + 1136930381, x2, y2);
            }
         } else {
            float x2 = x0 + 0.21132487F;
            float y2 = y0 + -0.7886751F;
            float a2 = 0.6666667F - x2 * x2 - y2 * y2;
            if (a2 > 0.0F) {
               value += a2 * a2 * (a2 * a2) * NoiseHelper.gradCoord(this.seed, cellX, cellY + 1136930381, x2, y2);
            }
         }

         if (fracY - xmyi > 1.0F) {
            float x3 = x0 + -0.3660254F;
            float y3 = y0 + -1.3660254F;
            float a3 = 0.6666667F - x3 * x3 - y3 * y3;
            if (a3 > 0.0F) {
               value += a3 * a3 * (a3 * a3) * NoiseHelper.gradCoord(this.seed, cellX + 501125321, cellY + -2021106534, x3, y3);
            }
         } else {
            float x3 = x0 + -0.7886751F;
            float y3 = y0 + 0.21132487F;
            float a3 = 0.6666667F - x3 * x3 - y3 * y3;
            if (a3 > 0.0F) {
               value += a3 * a3 * (a3 * a3) * NoiseHelper.gradCoord(this.seed, cellX + 501125321, cellY, x3, y3);
            }
         }
      } else {
         if (fracX + xmyi < 0.0F) {
            float x2 = x0 + 0.7886751F;
            float y2 = y0 - 0.21132487F;
            float a2 = 0.6666667F - x2 * x2 - y2 * y2;
            if (a2 > 0.0F) {
               value += a2 * a2 * (a2 * a2) * NoiseHelper.gradCoord(this.seed, cellX - 501125321, cellY, x2, y2);
            }
         } else {
            float x2 = x0 + -0.7886751F;
            float y2 = y0 + 0.21132487F;
            float a2 = 0.6666667F - x2 * x2 - y2 * y2;
            if (a2 > 0.0F) {
               value += a2 * a2 * (a2 * a2) * NoiseHelper.gradCoord(this.seed, cellX + 501125321, cellY, x2, y2);
            }
         }

         if (fracY < xmyi) {
            float x2x = x0 - 0.21132487F;
            float y2x = y0 - -0.7886751F;
            float a2x = 0.6666667F - x2x * x2x - y2x * y2x;
            if (a2x > 0.0F) {
               value += a2x * a2x * (a2x * a2x) * NoiseHelper.gradCoord(this.seed, cellX, cellY - 1136930381, x2x, y2x);
            }
         } else {
            float x2x = x0 + 0.21132487F;
            float y2x = y0 + -0.7886751F;
            float a2x = 0.6666667F - x2x * x2x - y2x * y2x;
            if (a2x > 0.0F) {
               value += a2x * a2x * (a2x * a2x) * NoiseHelper.gradCoord(this.seed, cellX, cellY + 1136930381, x2x, y2x);
            }
         }
      }

      return value * 9.120981F + 0.5F;
   }

   @Override
   public float evaluate(double x, double y, double z) {
      return evaluateStatic(x, y, z, this.seed);
   }

   public static float evaluateStatic(double x, double y, double z, int seed) {
      double xz = x + z;
      double s2 = xz * -0.211324865405187;
      y *= 0.577350269189626;
      x += s2 - y;
      z += s2 - y;
      y += xz * 0.577350269189626;
      double floorX = Math.floor(x);
      double floorY = Math.floor(y);
      double floorZ = Math.floor(z);
      int cellX = (int)floorX;
      int cellY = (int)floorY;
      int cellZ = (int)floorZ;
      float fracX = (float)(x - floorX);
      float fracY = (float)(y - floorY);
      float fracZ = (float)(z - floorZ);
      cellX *= 501125321;
      cellY *= 1136930381;
      cellZ *= 1720413743;
      int seed2 = seed + 1293373;
      int xNMask = (int)(-0.5F - fracX);
      int yNMask = (int)(-0.5F - fracY);
      int zNMask = (int)(-0.5F - fracZ);
      float x0 = fracX + xNMask;
      float y0 = fracY + yNMask;
      float z0 = fracZ + zNMask;
      float a0 = 0.75F - x0 * x0 - y0 * y0 - z0 * z0;
      float value = a0
         * a0
         * (a0 * a0)
         * NoiseHelper.gradCoord(seed, cellX + (xNMask & 501125321), cellY + (yNMask & 1136930381), cellZ + (zNMask & 1720413743), x0, y0, z0);
      float x1 = fracX - 0.5F;
      float y1 = fracY - 0.5F;
      float z1 = fracZ - 0.5F;
      float a1 = 0.75F - x1 * x1 - y1 * y1 - z1 * z1;
      value += a1 * a1 * (a1 * a1) * NoiseHelper.gradCoord(seed2, cellX + 501125321, cellY + 1136930381, cellZ + 1720413743, x1, y1, z1);
      float xAFlipMask0 = ((xNMask | 1) << 1) * x1;
      float yAFlipMask0 = ((yNMask | 1) << 1) * y1;
      float zAFlipMask0 = ((zNMask | 1) << 1) * z1;
      float xAFlipMask1 = (-2 - (xNMask << 2)) * x1 - 1.0F;
      float yAFlipMask1 = (-2 - (yNMask << 2)) * y1 - 1.0F;
      float zAFlipMask1 = (-2 - (zNMask << 2)) * z1 - 1.0F;
      boolean skip5 = false;
      float a2 = xAFlipMask0 + a0;
      if (a2 > 0.0F) {
         float x2 = x0 - (xNMask | 1);
         value += a2
            * a2
            * (a2 * a2)
            * NoiseHelper.gradCoord(seed, cellX + (~xNMask & 501125321), cellY + (yNMask & 1136930381), cellZ + (zNMask & 1720413743), x2, y0, z0);
      } else {
         float a3 = yAFlipMask0 + zAFlipMask0 + a0;
         if (a3 > 0.0F) {
            float y3 = y0 - (yNMask | 1);
            float z3 = z0 - (zNMask | 1);
            value += a3
               * a3
               * (a3 * a3)
               * NoiseHelper.gradCoord(seed, cellX + (xNMask & 501125321), cellY + (~yNMask & 1136930381), cellZ + (~zNMask & 1720413743), x0, y3, z3);
         }

         float a4 = xAFlipMask1 + a1;
         if (a4 > 0.0F) {
            float x4 = (xNMask | 1) + x1;
            value += a4 * a4 * (a4 * a4) * NoiseHelper.gradCoord(seed2, cellX + (xNMask & 1002250642), cellY + 1136930381, cellZ + 1720413743, x4, y1, z1);
            skip5 = true;
         }
      }

      boolean skip9 = false;
      float a6 = yAFlipMask0 + a0;
      if (a6 > 0.0F) {
         float y6 = y0 - (yNMask | 1);
         value += a6
            * a6
            * (a6 * a6)
            * NoiseHelper.gradCoord(seed, cellX + (xNMask & 501125321), cellY + (~yNMask & 1136930381), cellZ + (zNMask & 1720413743), x0, y6, z0);
      } else {
         float a7 = xAFlipMask0 + zAFlipMask0 + a0;
         if (a7 > 0.0F) {
            float x7 = x0 - (xNMask | 1);
            float z7 = z0 - (zNMask | 1);
            value += a7
               * a7
               * (a7 * a7)
               * NoiseHelper.gradCoord(seed, cellX + (~xNMask & 501125321), cellY + (yNMask & 1136930381), cellZ + (~zNMask & 1720413743), x7, y0, z7);
         }

         float a8 = yAFlipMask1 + a1;
         if (a8 > 0.0F) {
            float y8 = (yNMask | 1) + y1;
            value += a8 * a8 * (a8 * a8) * NoiseHelper.gradCoord(seed2, cellX + 501125321, cellY + (yNMask & -2021106534), cellZ + 1720413743, x1, y8, z1);
            skip9 = true;
         }
      }

      boolean skipD = false;
      float aA = zAFlipMask0 + a0;
      if (aA > 0.0F) {
         float zA = z0 - (zNMask | 1);
         value += aA
            * aA
            * (aA * aA)
            * NoiseHelper.gradCoord(seed, cellX + (xNMask & 501125321), cellY + (yNMask & 1136930381), cellZ + (~zNMask & 1720413743), x0, y0, zA);
      } else {
         float aB = xAFlipMask0 + yAFlipMask0 + a0;
         if (aB > 0.0F) {
            float xB = x0 - (xNMask | 1);
            float yB = y0 - (yNMask | 1);
            value += aB
               * aB
               * (aB * aB)
               * NoiseHelper.gradCoord(seed, cellX + (~xNMask & 501125321), cellY + (~yNMask & 1136930381), cellZ + (zNMask & 1720413743), xB, yB, z0);
         }

         float aC = zAFlipMask1 + a1;
         if (aC > 0.0F) {
            float zC = (zNMask | 1) + z1;
            value += aC * aC * (aC * aC) * NoiseHelper.gradCoord(seed2, cellX + 501125321, cellY + 1136930381, cellZ + (zNMask & -854139810), x1, y1, zC);
            skipD = true;
         }
      }

      if (!skip5) {
         float a5 = yAFlipMask1 + zAFlipMask1 + a1;
         if (a5 > 0.0F) {
            float y5 = (yNMask | 1) + y1;
            float z5 = (zNMask | 1) + z1;
            value += a5
               * a5
               * (a5 * a5)
               * NoiseHelper.gradCoord(seed2, cellX + 501125321, cellY + (yNMask & -2021106534), cellZ + (zNMask & -854139810), x1, y5, z5);
         }
      }

      if (!skip9) {
         float a9 = xAFlipMask1 + zAFlipMask1 + a1;
         if (a9 > 0.0F) {
            float x9 = (xNMask | 1) + x1;
            float z9 = (zNMask | 1) + z1;
            value += a9
               * a9
               * (a9 * a9)
               * NoiseHelper.gradCoord(seed2, cellX + (xNMask & 1002250642), cellY + 1136930381, cellZ + (zNMask & -854139810), x9, y1, z9);
         }
      }

      if (!skipD) {
         float aD = xAFlipMask1 + yAFlipMask1 + a1;
         if (aD > 0.0F) {
            float xD = (xNMask | 1) + x1;
            float yD = (yNMask | 1) + y1;
            value += aD
               * aD
               * (aD * aD)
               * NoiseHelper.gradCoord(seed2, cellX + (xNMask & 1002250642), cellY + (yNMask & -2021106534), cellZ + 1720413743, xD, yD, z1);
         }
      }

      return value * 4.523013F + 0.5F;
   }
}
