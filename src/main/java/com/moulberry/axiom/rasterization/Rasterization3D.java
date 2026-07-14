package com.moulberry.axiom.rasterization;

import com.google.common.math.DoubleMath;
import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.funcinterfaces.IntIntIntFloatConsumer;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import java.math.RoundingMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Rasterization3D {
   public static void triangle(BlockPos one, BlockPos two, BlockPos three, TriIntConsumer consumer) {
      one = one.immutable();
      two = two.immutable();
      three = three.immutable();
      if (one.equals(two)) {
         if (two.equals(three)) {
            consumer.accept(one.getX(), one.getY(), one.getZ());
         } else {
            bresenham(one, three, consumer);
         }
      } else if (one.equals(three)) {
         bresenham(one, two, consumer);
      } else if (two.equals(three)) {
         bresenham(one, three, consumer);
      } else {
         Vec3 normal = Vec3.atCenterOf(one).subtract(Vec3.atCenterOf(two)).cross(Vec3.atCenterOf(one).subtract(Vec3.atCenterOf(three)));
         double normalLength = Math.sqrt(normal.lengthSqr());
         if (normalLength < 1.0E-5) {
            bresenham(one, two, consumer);
            bresenham(two, three, consumer);
            bresenham(three, one, consumer);
         } else {
            double normalX = Math.abs(normal.x());
            double normalY = Math.abs(normal.y());
            double normalZ = Math.abs(normal.z());
            double planarK = -(one.getX() * normal.x + one.getY() * normal.y + one.getZ() * normal.z);
            if (BuildConfig.DEBUG) {
               double planarKTwo = -(two.getX() * normal.x + two.getY() * normal.y + two.getZ() * normal.z);
               if (Math.abs(planarK - planarKTwo) > 1.0E-5) {
                  throw new FaultyImplementationError();
               }

               double planarKThree = -(three.getX() * normal.x + three.getY() * normal.y + three.getZ() * normal.z);
               if (Math.abs(planarK - planarKThree) > 1.0E-5) {
                  throw new FaultyImplementationError();
               }
            }

            if (normalX > normalY && normalX > normalZ) {
               if (BuildConfig.DEBUG && normal.x == 0.0) {
                  throw new FaultyImplementationError();
               }

               Rasterization2D.triangle(one.getY(), one.getZ(), two.getY(), two.getZ(), three.getY(), three.getZ(), (y, z) -> {
                  int x = DoubleMath.roundToInt(-(y * normal.y + z * normal.z + planarK) / normal.x, RoundingMode.HALF_DOWN);
                  consumer.accept(x, y, z);
               });
            } else if (normalY > normalZ) {
               if (BuildConfig.DEBUG && normal.y == 0.0) {
                  throw new FaultyImplementationError();
               }

               Rasterization2D.triangle(one.getX(), one.getZ(), two.getX(), two.getZ(), three.getX(), three.getZ(), (x, z) -> {
                  int y = DoubleMath.roundToInt(-(x * normal.x + z * normal.z + planarK) / normal.y, RoundingMode.HALF_DOWN);
                  consumer.accept(x, y, z);
               });
            } else {
               if (BuildConfig.DEBUG && normal.z == 0.0) {
                  throw new FaultyImplementationError();
               }

               Rasterization2D.triangle(one.getX(), one.getY(), two.getX(), two.getY(), three.getX(), three.getY(), (x, y) -> {
                  int z = DoubleMath.roundToInt(-(x * normal.x + y * normal.y + planarK) / normal.z, RoundingMode.HALF_DOWN);
                  consumer.accept(x, y, z);
               });
            }
         }
      }
   }

   public static void bresenham(Vec3i from, Vec3i to, TriIntConsumer consumer) {
      consumer.accept(from.getX(), from.getY(), from.getZ());
      bresenhamSkipFrom(from, to, consumer);
   }

   public static void bresenhamSkipFrom(Vec3i from, Vec3i to, TriIntConsumer consumer) {
      if (!to.equals(from)) {
         from = new Vec3i(from.getX(), from.getY(), from.getZ());
         to = new Vec3i(to.getX(), to.getY(), to.getZ());
         int x = from.getX();
         int y = from.getY();
         int z = from.getZ();
         int dx = Math.abs(to.getX() - x);
         int dy = Math.abs(to.getY() - y);
         int dz = Math.abs(to.getZ() - z);
         int xs = to.getX() > x ? 1 : -1;
         int ys = to.getY() > y ? 1 : -1;
         int zs = to.getZ() > z ? 1 : -1;
         if (dx >= dy && dx >= dz) {
            int p1 = 2 * dy - dx;
            int p2 = 2 * dz - dx;

            while (x != to.getX()) {
               x += xs;
               if (p1 >= 0) {
                  y += ys;
                  p1 -= 2 * dx;
               }

               if (p2 >= 0) {
                  z += zs;
                  p2 -= 2 * dx;
               }

               p1 += 2 * dy;
               p2 += 2 * dz;
               consumer.accept(x, y, z);
            }
         } else if (dy >= dz) {
            int p1 = 2 * dx - dy;
            int p2 = 2 * dz - dy;

            while (y != to.getY()) {
               y += ys;
               if (p1 >= 0) {
                  x += xs;
                  p1 -= 2 * dy;
               }

               if (p2 >= 0) {
                  z += zs;
                  p2 -= 2 * dy;
               }

               p1 += 2 * dx;
               p2 += 2 * dz;
               consumer.accept(x, y, z);
            }
         } else {
            int p1 = 2 * dy - dz;
            int p2 = 2 * dx - dz;

            while (z != to.getZ()) {
               z += zs;
               if (p1 >= 0) {
                  y += ys;
                  p1 -= 2 * dz;
               }

               if (p2 >= 0) {
                  x += xs;
                  p2 -= 2 * dz;
               }

               p1 += 2 * dy;
               p2 += 2 * dx;
               consumer.accept(x, y, z);
            }
         }
      }
   }

   public static void dda(BlockPos from, BlockPos to, TriIntConsumer consumer) {
      consumer.accept(from.getX(), from.getY(), from.getZ());
      ddaSkipFrom(from, to, consumer);
   }

   public static void ddaSkipFrom(BlockPos from, BlockPos to, TriIntConsumer consumer) {
      if (!to.equals(from)) {
         from = from.immutable();
         to = to.immutable();
         Vec3 ray = Vec3.atLowerCornerOf(to.subtract(from)).normalize();
         int mapX = from.getX();
         int mapY = from.getY();
         int mapZ = from.getZ();
         double deltaDistX = Math.abs(1.0 / ray.x());
         double deltaDistY = Math.abs(1.0 / ray.y());
         double deltaDistZ = Math.abs(1.0 / ray.z());
         int stepX = ray.x() < 0.0 ? -1 : 1;
         int stepY = ray.y() < 0.0 ? -1 : 1;
         int stepZ = ray.z() < 0.0 ? -1 : 1;
         double sideDistX = 0.5 * deltaDistX;
         double sideDistY = 0.5 * deltaDistY;
         double sideDistZ = 0.5 * deltaDistZ;
         int limit = 0;

         while (!BuildConfig.DEBUG || limit++ < 100000) {
            if (sideDistZ < sideDistX && sideDistZ < sideDistY) {
               sideDistZ += deltaDistZ;
               mapZ += stepZ;
            } else if (sideDistX < sideDistY) {
               sideDistX += deltaDistX;
               mapX += stepX;
            } else {
               sideDistY += deltaDistY;
               mapY += stepY;
            }

            consumer.accept(mapX, mapY, mapZ);
            if (mapX == to.getX() && mapY == to.getY() && mapZ == to.getZ()) {
               return;
            }
         }

         System.err.println("From: " + from);
         System.err.println("To: " + to);
         throw new FaultyImplementationError("100,000 iterations. Infinite loop?");
      }
   }

   public static void dda(Vector3d from, Vector3d to, TriIntConsumer consumer) {
      dda(new Vec3(from.x, from.y, from.z), new Vec3(to.x, to.y, to.z), consumer);
   }

   public static void dda(Vec3 from, Vec3 to, TriIntConsumer consumer) {
      int fromMapX = (int)Math.floor(from.x);
      int fromMapY = (int)Math.floor(from.y);
      int fromMapZ = (int)Math.floor(from.z);
      consumer.accept(fromMapX, fromMapY, fromMapZ);
      ddaSkipFrom(from, to, consumer);
   }

   public static void ddaSkipFrom(Vec3 from, Vec3 to, TriIntConsumer consumer) {
      int fromMapX = (int)Math.floor(from.x);
      int fromMapY = (int)Math.floor(from.y);
      int fromMapZ = (int)Math.floor(from.z);
      int toMapX = (int)Math.floor(to.x);
      int toMapY = (int)Math.floor(to.y);
      int toMapZ = (int)Math.floor(to.z);
      if (toMapX != fromMapX || toMapY != fromMapY || toMapZ != fromMapZ) {
         Vec3 ray = to.subtract(from).normalize();
         if (!ray.equals(Vec3.ZERO)) {
            int mapX = fromMapX;
            int mapY = fromMapY;
            int mapZ = fromMapZ;
            double deltaDistX = Math.abs(1.0 / ray.x);
            double deltaDistY = Math.abs(1.0 / ray.y);
            double deltaDistZ = Math.abs(1.0 / ray.z);
            int stepX = ray.x() < 0.0 ? -1 : 1;
            int stepY = ray.y() < 0.0 ? -1 : 1;
            int stepZ = ray.z() < 0.0 ? -1 : 1;
            double sideDistX = (ray.x > 0.0 ? 1.0 - from.x + fromMapX : from.x - fromMapX) * deltaDistX;
            double sideDistY = (ray.y > 0.0 ? 1.0 - from.y + fromMapY : from.y - fromMapY) * deltaDistY;
            double sideDistZ = (ray.z > 0.0 ? 1.0 - from.z + fromMapZ : from.z - fromMapZ) * deltaDistZ;
            if (Double.isNaN(sideDistX)) {
               sideDistX = Double.POSITIVE_INFINITY;
            }

            if (Double.isNaN(sideDistY)) {
               sideDistY = Double.POSITIVE_INFINITY;
            }

            if (Double.isNaN(sideDistZ)) {
               sideDistZ = Double.POSITIVE_INFINITY;
            }

            int limit = 0;

            while (!BuildConfig.DEBUG || limit++ < 100000) {
               if (sideDistZ < sideDistX && sideDistZ < sideDistY) {
                  sideDistZ += deltaDistZ;
                  mapZ += stepZ;
               } else if (sideDistX < sideDistY) {
                  sideDistX += deltaDistX;
                  mapX += stepX;
               } else {
                  sideDistY += deltaDistY;
                  mapY += stepY;
               }

               if (mapX * stepX > toMapX * stepX || mapY * stepY > toMapY * stepY || mapZ * stepZ > toMapZ * stepZ) {
                  return;
               }

               consumer.accept(mapX, mapY, mapZ);
            }

            System.err.println("From: " + from);
            System.err.println("To: " + to);
            throw new FaultyImplementationError("100,000 iterations. Infinite loop?");
         }
      }
   }

   public static void ddaPartial(Vector3d from, Vector3d to, IntIntIntFloatConsumer consumer) {
      int fromMapX = (int)Math.floor(from.x);
      int fromMapY = (int)Math.floor(from.y);
      int fromMapZ = (int)Math.floor(from.z);
      int toMapX = (int)Math.floor(to.x);
      int toMapY = (int)Math.floor(to.y);
      int toMapZ = (int)Math.floor(to.z);
      if (to.equals(from)) {
         consumer.accept(fromMapX, fromMapY, fromMapZ, 0.5F);
      } else {
         double invDistance = 1.0 / to.distance(from);
         Vector3d ray = to.sub(from, new Vector3d()).normalize();
         double delta = (fromMapX + 0.5 - from.x) * ray.x + (fromMapY + 0.5 - from.y) * ray.y + (fromMapZ + 0.5 - from.z) * ray.z;
         delta *= invDistance;
         if (delta < 0.0) {
            delta = 0.0;
         } else if (delta > 1.0) {
            delta = 1.0;
         }

         consumer.accept(fromMapX, fromMapY, fromMapZ, (float)delta);
         if (toMapX != fromMapX || toMapY != fromMapY || toMapZ != fromMapZ) {
            int mapX = fromMapX;
            int mapY = fromMapY;
            int mapZ = fromMapZ;
            double deltaDistX = Math.abs(1.0 / ray.x);
            double deltaDistY = Math.abs(1.0 / ray.y);
            double deltaDistZ = Math.abs(1.0 / ray.z);
            int stepX = ray.x() < 0.0 ? -1 : 1;
            int stepY = ray.y() < 0.0 ? -1 : 1;
            int stepZ = ray.z() < 0.0 ? -1 : 1;
            double sideDistX = (ray.x > 0.0 ? 1.0 - from.x + fromMapX : from.x - fromMapX) * deltaDistX;
            double sideDistY = (ray.y > 0.0 ? 1.0 - from.y + fromMapY : from.y - fromMapY) * deltaDistY;
            double sideDistZ = (ray.z > 0.0 ? 1.0 - from.z + fromMapZ : from.z - fromMapZ) * deltaDistZ;
            int limit = 0;

            while (!BuildConfig.DEBUG || limit++ < 100000) {
               if (sideDistZ < sideDistX && sideDistZ < sideDistY) {
                  sideDistZ += deltaDistZ;
                  mapZ += stepZ;
               } else if (sideDistX < sideDistY) {
                  sideDistX += deltaDistX;
                  mapX += stepX;
               } else {
                  sideDistY += deltaDistY;
                  mapY += stepY;
               }

               if (mapX * stepX > toMapX * stepX || mapY * stepY > toMapY * stepY || mapZ * stepZ > toMapZ * stepZ) {
                  return;
               }

               delta = (mapX + 0.5 - from.x) * ray.x + (mapY + 0.5 - from.y) * ray.y + (mapZ + 0.5 - from.z) * ray.z;
               delta *= invDistance;
               if (delta < 0.0) {
                  delta = 0.0;
               } else if (delta > 1.0) {
                  delta = 1.0;
               }

               consumer.accept(mapX, mapY, mapZ, (float)delta);
            }

            System.err.println("From: " + from);
            System.err.println("To: " + to);
            throw new FaultyImplementationError("100,000 iterations. Infinite loop?");
         }
      }
   }

   private static float fpart(float f) {
      return f - (float)Math.floor(f);
   }

   private static float rfpart(float f) {
      return 1.0F - fpart(f);
   }

   public static void xiaolinWu(Vector3f from, Vector3f to, IntIntIntFloatConsumer consumer) {
      float x0 = from.x - 0.5F;
      float y0 = from.y - 0.5F;
      float z0 = from.z - 0.5F;
      float x1 = to.x - 0.5F;
      float y1 = to.y - 0.5F;
      float z1 = to.z - 0.5F;
      int bx0 = Math.round(x0);
      int by0 = Math.round(y0);
      int bz0 = Math.round(z0);
      int bx1 = Math.round(x1);
      int by1 = Math.round(y1);
      int bz1 = Math.round(z1);
      if (bx0 == bx1 && by0 == by1 && bz0 == bz1) {
         consumer.accept(bx0, by0, bz0, Math.max(Math.abs(x0 - x1), Math.max(Math.abs(y0 - y1), Math.abs(z0 - z1))));
      } else {
         boolean steepY = Math.abs(y0 - y1) > Math.abs(x0 - x1) && Math.abs(y0 - y1) > Math.abs(z0 - z1);
         boolean steepZ = !steepY && Math.abs(z0 - z1) > Math.abs(x0 - x1);
         if (steepY) {
            float temp = x0;
            x0 = y0;
            y0 = temp;
            temp = x1;
            x1 = y1;
            y1 = temp;
         } else if (steepZ) {
            float temp = x0;
            x0 = z0;
            z0 = temp;
            temp = x1;
            x1 = z1;
            z1 = temp;
         }

         if (x0 > x1) {
            float temp = x0;
            x0 = x1;
            x1 = temp;
            temp = y0;
            y0 = y1;
            y1 = temp;
            temp = z0;
            z0 = z1;
            z1 = temp;
         }

         bx0 = Math.round(x0);
         bx1 = Math.round(x1);
         if (bx0 == bx1) {
            by0 = Math.round(y0);
            bz0 = Math.round(z0);
            if (steepY) {
               consumer.accept(by0, bx0, bz0, Math.abs(x0 - x1));
            } else if (steepZ) {
               consumer.accept(bz0, by0, bx0, Math.abs(x0 - x1));
            } else {
               consumer.accept(bx0, by0, bz0, Math.abs(x0 - x1));
            }
         } else {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float dz = z1 - z0;
            float gradientY = 1.0F;
            float gradientZ = 1.0F;
            if (dx != 0.0) {
               gradientY = dy / dx;
               gradientZ = dz / dx;
            }

            float xgap = rfpart(x0 + 0.5F);
            float yend = y0 + gradientY * (bx0 - x0);
            float zend = z0 + gradientZ * (bx0 - x0);
            int ypxl = (int)Math.floor(yend);
            int zpxl = (int)Math.floor(zend);
            float fy = fpart(yend);
            float rfy = 1.0F - fy;
            float fz = fpart(zend);
            float rfz = 1.0F - fz;
            if (steepY) {
               consumer.accept(ypxl, bx0, zpxl, rfy * rfz * xgap);
               consumer.accept(ypxl + 1, bx0, zpxl, fy * rfz * xgap);
               consumer.accept(ypxl, bx0, zpxl + 1, rfy * fz * xgap);
               consumer.accept(ypxl + 1, bx0, zpxl + 1, fy * fz * xgap);
            } else if (steepZ) {
               consumer.accept(zpxl, ypxl, bx0, rfy * rfz * xgap);
               consumer.accept(zpxl, ypxl + 1, bx0, fy * rfz * xgap);
               consumer.accept(zpxl + 1, ypxl, bx0, rfy * fz * xgap);
               consumer.accept(zpxl + 1, ypxl + 1, bx0, fy * fz * xgap);
            } else {
               consumer.accept(bx0, ypxl, zpxl, rfy * rfz * xgap);
               consumer.accept(bx0, ypxl + 1, zpxl, fy * rfz * xgap);
               consumer.accept(bx0, ypxl, zpxl + 1, rfy * fz * xgap);
               consumer.accept(bx0, ypxl + 1, zpxl + 1, fy * fz * xgap);
            }

            float intery = yend + gradientY;
            float interz = zend + gradientZ;
            xgap = fpart(x1 + 0.5F);
            yend = y1 + gradientY * (bx1 - x1);
            zend = z1 + gradientZ * (bx1 - x1);
            ypxl = (int)Math.floor(yend);
            zpxl = (int)Math.floor(zend);
            fy = fpart(yend);
            rfy = 1.0F - fy;
            fz = fpart(zend);
            rfz = 1.0F - fz;
            if (steepY) {
               consumer.accept(ypxl, bx1, zpxl, rfy * rfz * xgap);
               consumer.accept(ypxl + 1, bx1, zpxl, fy * rfz * xgap);
               consumer.accept(ypxl, bx1, zpxl + 1, rfy * fz * xgap);
               consumer.accept(ypxl + 1, bx1, zpxl + 1, fy * fz * xgap);
            } else if (steepZ) {
               consumer.accept(zpxl, ypxl, bx1, rfy * rfz * xgap);
               consumer.accept(zpxl, ypxl + 1, bx1, fy * rfz * xgap);
               consumer.accept(zpxl + 1, ypxl, bx1, rfy * fz * xgap);
               consumer.accept(zpxl + 1, ypxl + 1, bx1, fy * fz * xgap);
            } else {
               consumer.accept(bx1, ypxl, zpxl, rfy * rfz * xgap);
               consumer.accept(bx1, ypxl + 1, zpxl, fy * rfz * xgap);
               consumer.accept(bx1, ypxl, zpxl + 1, rfy * fz * xgap);
               consumer.accept(bx1, ypxl + 1, zpxl + 1, fy * fz * xgap);
            }

            if (steepY) {
               for (int x = bx0 + 1; x <= bx1 - 1; x++) {
                  fy = fpart(intery);
                  rfy = 1.0F - fy;
                  fz = fpart(interz);
                  rfz = 1.0F - fz;
                  int y = (int)Math.floor(intery);
                  int z = (int)Math.floor(interz);
                  consumer.accept(y, x, z, rfy * rfz);
                  consumer.accept(y + 1, x, z, fy * rfz);
                  consumer.accept(y, x, z + 1, rfy * fz);
                  consumer.accept(y + 1, x, z + 1, fy * fz);
                  intery += gradientY;
                  interz += gradientZ;
               }
            } else if (steepZ) {
               for (int x = bx0 + 1; x <= bx1 - 1; x++) {
                  fy = fpart(intery);
                  rfy = 1.0F - fy;
                  fz = fpart(interz);
                  rfz = 1.0F - fz;
                  int y = (int)Math.floor(intery);
                  int z = (int)Math.floor(interz);
                  consumer.accept(z, y, x, rfy * rfz);
                  consumer.accept(z, y + 1, x, fy * rfz);
                  consumer.accept(z + 1, y, x, rfy * fz);
                  consumer.accept(z + 1, y + 1, x, fy * fz);
                  intery += gradientY;
                  interz += gradientZ;
               }
            } else {
               for (int x = bx0 + 1; x <= bx1 - 1; x++) {
                  fy = fpart(intery);
                  rfy = 1.0F - fy;
                  fz = fpart(interz);
                  rfz = 1.0F - fz;
                  int y = (int)Math.floor(intery);
                  int z = (int)Math.floor(interz);
                  consumer.accept(x, y, z, rfy * rfz);
                  consumer.accept(x, y + 1, z, fy * rfz);
                  consumer.accept(x, y, z + 1, rfy * fz);
                  consumer.accept(x, y + 1, z + 1, fy * fz);
                  intery += gradientY;
                  interz += gradientZ;
               }
            }
         }
      }
   }
}
