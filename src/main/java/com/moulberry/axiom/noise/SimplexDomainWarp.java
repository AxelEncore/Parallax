package com.moulberry.axiom.noise;

import org.joml.Vector2d;
import org.joml.Vector3d;

public class SimplexDomainWarp {
   public static void SingleDomainWarpOpenSimplex2DGradient(int seed, double amplitudeX, double amplitudeY, double frequency, Vector2d coord) {
      double x = coord.x;
      double y = coord.y;
      double SQRT3 = 1.7320508F;
      double G2 = 0.21132487F;
      x *= frequency;
      y *= frequency;
      double F2 = 0.3660254F;
      double s = (x + y) * 0.3660254F;
      x += s;
      y += s;
      int i = (int)Math.floor(x);
      int j = (int)Math.floor(y);
      double xi = x - i;
      double yi = y - j;
      double t = (xi + yi) * 0.21132487F;
      double x0 = xi - t;
      double y0 = yi - t;
      i *= 501125321;
      j *= 1136930381;
      double vy = 0.0;
      double vx = 0.0;
      double a = 0.5 - x0 * x0 - y0 * y0;
      if (a > 0.0) {
         double aaaa = a * a * (a * a);
         int hash = NoiseHelper.hash(seed, i, j);
         int index1 = hash & 254;
         int index2 = hash >> 7 & 510;
         double xg = NoiseHelper.GRADIENTS_2D[index1];
         double yg = NoiseHelper.GRADIENTS_2D[index1 | 1];
         double value = x0 * xg + y0 * yg;
         double xgo = NoiseHelper.RAND_VECS_2D[index2];
         double ygo = NoiseHelper.RAND_VECS_2D[index2 | 1];
         double xo = value * xgo;
         double yo = value * ygo;
         vx += aaaa * xo;
         vy += aaaa * yo;
      }

      double c = 3.1547003477905378 * t + (-0.6666666427356915 + a);
      if (c > 0.0) {
         double x2 = x0 + -0.57735026F;
         double y2 = y0 + -0.57735026F;
         double cccc = c * c * (c * c);
         int hash = NoiseHelper.hash(seed, i + 501125321, j + 1136930381);
         int index1 = hash & 254;
         int index2 = hash >> 7 & 510;
         double xg = NoiseHelper.GRADIENTS_2D[index1];
         double yg = NoiseHelper.GRADIENTS_2D[index1 | 1];
         double value = x2 * xg + y2 * yg;
         double xgo = NoiseHelper.RAND_VECS_2D[index2];
         double ygo = NoiseHelper.RAND_VECS_2D[index2 | 1];
         double xo = value * xgo;
         double yo = value * ygo;
         vx += cccc * xo;
         vy += cccc * yo;
      }

      if (y0 > x0) {
         double x1 = x0 + 0.21132487F;
         double y1 = y0 + -0.7886751F;
         double b = 0.5 - x1 * x1 - y1 * y1;
         if (b > 0.0) {
            double bbbb = b * b * (b * b);
            int hash = NoiseHelper.hash(seed, i, j + 1136930381);
            int index1 = hash & 254;
            int index2 = hash >> 7 & 510;
            double xg = NoiseHelper.GRADIENTS_2D[index1];
            double yg = NoiseHelper.GRADIENTS_2D[index1 | 1];
            double value = x1 * xg + y1 * yg;
            double xgo = NoiseHelper.RAND_VECS_2D[index2];
            double ygo = NoiseHelper.RAND_VECS_2D[index2 | 1];
            double xo = value * xgo;
            double yo = value * ygo;
            vx += bbbb * xo;
            vy += bbbb * yo;
         }
      } else {
         double x1 = x0 + -0.7886751F;
         double y1 = y0 + 0.21132487F;
         double b = 0.5 - x1 * x1 - y1 * y1;
         if (b > 0.0) {
            double bbbb = b * b * (b * b);
            int hash = NoiseHelper.hash(seed, i + 501125321, j);
            int index1 = hash & 254;
            int index2 = hash >> 7 & 510;
            double xg = NoiseHelper.GRADIENTS_2D[index1];
            double yg = NoiseHelper.GRADIENTS_2D[index1 | 1];
            double value = x1 * xg + y1 * yg;
            double xgo = NoiseHelper.RAND_VECS_2D[index2];
            double ygo = NoiseHelper.RAND_VECS_2D[index2 | 1];
            double xo = value * xgo;
            double yo = value * ygo;
            vx += bbbb * xo;
            vy += bbbb * yo;
         }
      }

      coord.x += vx * amplitudeX;
      coord.y += vy * amplitudeY;
   }

   public static void SingleDomainWarpOpenSimplex3DGradient(int seed, double amplitudeX, double amplitudeY, double amplitudeZ, double frequency, Vector3d coord) {
      double x = coord.x;
      double y = coord.y;
      double z = coord.z;
      double xz = x + z;
      double s2 = xz * -0.211324865405187;
      y *= 0.577350269189626;
      x += s2 - y;
      z += s2 - y;
      y += xz * 0.577350269189626;
      x *= frequency;
      y *= frequency;
      z *= frequency;
      double R3 = 0.6666666666666666;
      double r = (x + y + z) * 0.6666666666666666;
      x = r - x;
      y = r - y;
      z = r - z;
      int i = (int)Math.round(x);
      int j = (int)Math.round(y);
      int k = (int)Math.round(z);
      double x0 = x - i;
      double y0 = y - j;
      double z0 = z - k;
      int xNSign = (int)(-x0 - 1.0) | 1;
      int yNSign = (int)(-y0 - 1.0) | 1;
      int zNSign = (int)(-z0 - 1.0) | 1;
      double ax0 = xNSign * -x0;
      double ay0 = yNSign * -y0;
      double az0 = zNSign * -z0;
      i *= 501125321;
      j *= 1136930381;
      k *= 1720413743;
      double vz = 0.0;
      double vy = 0.0;
      double vx = 0.0;
      double a = 0.6F - x0 * x0 - (y0 * y0 + z0 * z0);
      int l = 0;

      while (true) {
         if (a > 0.0) {
            double aaaa = a * a * (a * a);
            int hash = NoiseHelper.hash(seed, i, j, k);
            int index1 = hash & 252;
            int index2 = hash >> 6 & 1020;
            double xg = NoiseHelper.GRADIENTS_3D[index1];
            double yg = NoiseHelper.GRADIENTS_3D[index1 | 1];
            double zg = NoiseHelper.GRADIENTS_3D[index1 | 2];
            double value = x0 * xg + y0 * yg + z0 * zg;
            double xgo = NoiseHelper.RAND_VECS_3D[index2];
            double ygo = NoiseHelper.RAND_VECS_3D[index2 | 1];
            double zgo = NoiseHelper.RAND_VECS_3D[index2 | 2];
            double xo = value * xgo;
            double yo = value * ygo;
            double zo = value * zgo;
            vx += aaaa * xo;
            vy += aaaa * yo;
            vz += aaaa * zo;
         }

         int i1 = i;
         int j1 = j;
         int k1 = k;
         double x1 = x0;
         double y1 = y0;
         double z1 = z0;
         double var100;
         if (ax0 >= ay0 && ax0 >= az0) {
            x1 = x0 + xNSign;
            var100 = a + ax0 + ax0;
            i1 = i - xNSign * 501125321;
         } else if (ay0 > ax0 && ay0 >= az0) {
            y1 = y0 + yNSign;
            var100 = a + ay0 + ay0;
            j1 = j - yNSign * 1136930381;
         } else {
            z1 = z0 + zNSign;
            var100 = a + az0 + az0;
            k1 = k - zNSign * 1720413743;
         }

         if (var100 > 1.0) {
            double bbbb = --var100 * var100 * (var100 * var100);
            int hash = NoiseHelper.hash(seed, i1, j1, k1);
            int index1 = hash & 252;
            int index2 = hash >> 6 & 1020;
            double xg = NoiseHelper.GRADIENTS_3D[index1];
            double yg = NoiseHelper.GRADIENTS_3D[index1 | 1];
            double zg = NoiseHelper.GRADIENTS_3D[index1 | 2];
            double value = x1 * xg + y1 * yg + z1 * zg;
            double xgo = NoiseHelper.RAND_VECS_3D[index2];
            double ygo = NoiseHelper.RAND_VECS_3D[index2 | 1];
            double zgo = NoiseHelper.RAND_VECS_3D[index2 | 2];
            double xo = value * xgo;
            double yo = value * ygo;
            double zo = value * zgo;
            vx += bbbb * xo;
            vy += bbbb * yo;
            vz += bbbb * zo;
         }

         if (l == 1) {
            coord.x += vx * amplitudeX;
            coord.y += vy * amplitudeY;
            coord.z += vz * amplitudeZ;
            return;
         }

         ax0 = 0.5 - ax0;
         ay0 = 0.5 - ay0;
         az0 = 0.5 - az0;
         x0 = xNSign * ax0;
         y0 = yNSign * ay0;
         z0 = zNSign * az0;
         a += 0.75 - ax0 - (ay0 + az0);
         i += xNSign >> 1 & 501125321;
         j += yNSign >> 1 & 1136930381;
         k += zNSign >> 1 & 1720413743;
         xNSign = -xNSign;
         yNSign = -yNSign;
         zNSign = -zNSign;
         seed += 1293373;
         l++;
      }
   }
}
