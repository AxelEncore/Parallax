package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.funcinterfaces.BiFloatPredicate;
import com.moulberry.axiom.funcinterfaces.TriIntPredicate;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.shape.TerrainDistanceField;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class RasterizationHelper {
   public static float finiteOrMaxValue(float input) {
      return !Float.isFinite(input) ? Float.MAX_VALUE : input;
   }

   public static float calcInvRadiusSq(float radius) {
      float invRadiusSq = 1.0F / ((radius + 0.5F) * (radius + 0.5F));
      if (!Float.isFinite(invRadiusSq)) {
         invRadiusSq = Float.MAX_VALUE;
      }

      return invRadiusSq;
   }

   public static void planeCondition(
      Vector3i center,
      float diameterX,
      float diameterZ,
      boolean centerEvenDiameters,
      Quaternionf quaternionf,
      BiFloatPredicate condition,
      TriIntConsumer accept
   ) {
      Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
      Vector3f side = new Vector3f(1.0F, 0.0F, 0.0F);
      Vector3f up = new Vector3f(0.0F, 0.0F, 1.0F);
      quaternionf.transformInverse(normal);
      quaternionf.transformInverse(side);
      quaternionf.transformInverse(up);
      int centerX = center.x;
      int centerY = center.y;
      int centerZ = center.z;
      double normalX = Math.abs(normal.x());
      double normalY = Math.abs(normal.y());
      double normalZ = Math.abs(normal.z());
      float radiusX = (diameterX - 1.0F) / 2.0F;
      float radiusZ = (diameterZ - 1.0F) / 2.0F;
      float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) * side.x - radiusZ % 1.0F * up.x : 0.0F;
      float offsetY = centerEvenDiameters ? -(radiusX % 1.0F) * side.y - radiusZ % 1.0F * up.y : 0.0F;
      float offsetZ = centerEvenDiameters ? -(radiusX % 1.0F) * side.z - radiusZ % 1.0F * up.z : 0.0F;
      float planarK = -(offsetX * normal.x + offsetY * normal.y + offsetZ * normal.z);
      Vector3f minimum = new Vector3f();
      Vector3f maximum = new Vector3f();

      for (int x = -1; x <= 1; x += 2) {
         for (int z = -1; z <= 1; z += 2) {
            Vector3f pos = new Vector3f(radiusX * x, 0.0F, radiusZ * z);
            quaternionf.transformInverse(pos);
            if (pos.x < minimum.x) {
               minimum.x = pos.x;
            }

            if (pos.y < minimum.y) {
               minimum.y = pos.y;
            }

            if (pos.z < minimum.z) {
               minimum.z = pos.z;
            }

            if (pos.x > maximum.x) {
               maximum.x = pos.x;
            }

            if (pos.y > maximum.y) {
               maximum.y = pos.y;
            }

            if (pos.z > maximum.z) {
               maximum.z = pos.z;
            }
         }
      }

      if (normalX > normalY && normalX > normalZ) {
         int minY = (int)Math.floor(minimum.y);
         int maxY = (int)Math.ceil(maximum.y);
         int minZ = (int)Math.floor(minimum.z);
         int maxZ = (int)Math.ceil(maximum.z);
         Vector3f vec = new Vector3f();

         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               float x = -((y + offsetY) * normal.y + (z + offsetZ) * normal.z + planarK) / normal.x;
               vec.set(x, y + offsetY, z + offsetZ);
               float rx = side.dot(vec);
               float rz = up.dot(vec);
               if (condition.test(rx, rz)) {
                  int roundedX = Math.round(x);
                  accept.accept(roundedX + centerX, y + centerY, z + centerZ);
               }
            }
         }
      } else if (normalY > normalZ) {
         int minX = (int)Math.floor(minimum.x);
         int maxX = (int)Math.ceil(maximum.x);
         int minZ = (int)Math.floor(minimum.z);
         int maxZ = (int)Math.ceil(maximum.z);
         Vector3f vec = new Vector3f();

         for (int x = minX; x <= maxX; x++) {
            for (int zx = minZ; zx <= maxZ; zx++) {
               float y = -((x + offsetX) * normal.x + (zx + offsetZ) * normal.z + planarK) / normal.y;
               vec.set(x + offsetX, y, zx + offsetZ);
               float rx = side.dot(vec);
               float rz = up.dot(vec);
               if (condition.test(rx, rz)) {
                  int roundedY = Math.round(y);
                  accept.accept(x + centerX, roundedY + centerY, zx + centerZ);
               }
            }
         }
      } else {
         int minX = (int)Math.floor(minimum.x);
         int maxX = (int)Math.ceil(maximum.x);
         int minY = (int)Math.floor(minimum.y);
         int maxY = (int)Math.ceil(maximum.y);
         Vector3f vec = new Vector3f();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               float zxx = -((x + offsetX) * normal.x + (y + offsetY) * normal.y + planarK) / normal.z;
               vec.set(x + offsetX, y + offsetY, zxx);
               float rx = side.dot(vec);
               float rz = up.dot(vec);
               if (condition.test(rx, rz)) {
                  int roundedZ = Math.round(zxx);
                  accept.accept(x + centerX, y + centerY, roundedZ + centerZ);
               }
            }
         }
      }
   }

   public static void metaball(
      Level level,
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      int maxRadiusX,
      int maxRadiusY,
      int maxRadiusZ,
      TriIntPredicate insideShape
   ) {
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      int centerX = center.getX();
      int centerY = center.getY();
      int centerZ = center.getZ();
      TerrainDistanceField.calculateChamferEuclideanTwo(
         (x, y, z) -> level.getBlockState(mutableBlockPos.set(x + centerX, y + centerY, z + centerZ)).blocksMotion(),
         insideShape,
         -maxRadiusX,
         -maxRadiusY,
         -maxRadiusZ,
         maxRadiusX,
         maxRadiusY,
         maxRadiusZ,
         (x, y, z, terrainDistance, shapeDistance) -> {
            if (shapeDistance <= 0.0F) {
               region.addBlock(x + centerX, y + centerY, z + centerZ, block);
            } else if (!(terrainDistance <= 0.0F)) {
               float terrainAmount = metaballRange / (metaballRange + terrainDistance * terrainDistance) - 1.0F / (1 + metaballRange);
               float shapeAmount = metaballRange / (metaballRange + shapeDistance * shapeDistance) - 1.0F / (1 + metaballRange);
               if (terrainAmount + shapeAmount >= 1.0F) {
                  region.addBlock(x + centerX, y + centerY, z + centerZ, block);
               }
            }
         }
      );
   }

   private static float sqr(float x) {
      return x * x;
   }

   private static float getRootEllipse(float r0, float z0, float z1, float g) {
      float n0 = r0 * z0;
      float s0 = z1 - 1.0F;
      float s1 = g < 0.0F ? 0.0F : (float)Math.sqrt(n0 * n0 + z1 * z1) - 1.0F;
      float s = 0.0F;

      for (int i = 0; i < 100; i++) {
         s = (s0 + s1) / 2.0F;
         if (s1 - s0 < 0.01F) {
            break;
         }

         float ratio0 = n0 / (s + r0);
         float ratio1 = z1 / (s + 1.0F);
         g = sqr(ratio0) + sqr(ratio1) - 1.0F;
         if (g > 0.0F) {
            s0 = s;
         } else {
            if (!(g < 0.0F)) {
               break;
            }

            s1 = s;
         }
      }

      return s;
   }

   public static float distancePointEllipse(float e0, float e1, float y0, float y1, @Nullable Vector2f intersection) {
      boolean swapped = false;
      if (e0 < e1) {
         float temp = e1;
         e1 = e0;
         e0 = temp;
         temp = y1;
         y1 = y0;
         y0 = temp;
         swapped = true;
      }

      boolean negate0 = false;
      if (y0 < 0.0F) {
         y0 = -y0;
         negate0 = true;
      }

      boolean negate1 = false;
      if (y1 < 0.0F) {
         y1 = -y1;
         negate1 = true;
      }

      float distance = distancePointEllipseInner(e0, e1, y0, y1, intersection);
      if (intersection != null) {
         if (negate1) {
            intersection.y *= -1.0F;
         }

         if (negate0) {
            intersection.x *= -1.0F;
         }

         if (swapped) {
            float temp = intersection.x;
            intersection.x = intersection.y;
            intersection.y = temp;
         }
      }

      return distance;
   }

   private static float distancePointEllipseInner(float e0, float e1, float p0, float p1, @Nullable Vector2f intersection) {
      if (p1 > 0.0F) {
         if (p0 > 0.0F) {
            float z0 = p0 / e0;
            float z1 = p1 / e1;
            float g = sqr(z0) + sqr(z1) - 1.0F;
            if (g != 0.0F) {
               float r0 = sqr(e0 / e1);
               float sbar = getRootEllipse(r0, z0, z1, g);
               float c0 = r0 * p0 / (sbar + r0);
               float c1 = p1 / (sbar + 1.0F);
               if (intersection != null) {
                  intersection.set(c0, c1);
               }

               return (float)Math.sqrt(sqr(c0 - p0) + sqr(c1 - p1));
            } else {
               if (intersection != null) {
                  intersection.set(p0, p1);
               }

               return 0.0F;
            }
         } else {
            if (intersection != null) {
               intersection.set(0.0F, e1);
            }

            return Math.abs(p1 - e1);
         }
      } else {
         float numer0 = e0 * p0;
         float denom0 = sqr(e0) - sqr(e1);
         if (numer0 < denom0) {
            float xde0 = numer0 / denom0;
            float c0 = e0 * xde0;
            float c1 = e1 * (float)Math.sqrt(1.0F - xde0 * xde0);
            if (intersection != null) {
               intersection.set(c0, c1);
            }

            return (float)Math.sqrt(sqr(c0 - p0) + sqr(c1));
         } else {
            if (intersection != null) {
               intersection.set(e0, 0.0F);
            }

            return Math.abs(p0 - e0);
         }
      }
   }

   private static float getRootEllipsoid(float r0, float r1, float z0, float z1, float z2, float g) {
      float n0 = r0 * z0;
      float n1 = r1 * z1;
      float s0 = z2 - 1.0F;
      float s1 = g < 0.0F ? 0.0F : (float)Math.sqrt(n0 * n0 + n1 * n1 + z2 * z2) - 1.0F;
      float s = 0.0F;

      for (int i = 0; i < 100; i++) {
         s = (s0 + s1) / 2.0F;
         if (s1 - s0 < 0.01F) {
            break;
         }

         float ratio0 = n0 / (s + r0);
         float ratio1 = n1 / (s + r1);
         float ratio2 = z2 / (s + 1.0F);
         g = sqr(ratio0) + sqr(ratio1) + sqr(ratio2) - 1.0F;
         if (g > 0.0F) {
            s0 = s;
         } else {
            if (!(g < 0.0F)) {
               break;
            }

            s1 = s;
         }
      }

      return s;
   }

   public static float distancePointEllipsoid(float e0, float e1, float e2, float y0, float y1, float y2, @Nullable Vector3f intersection) {
      boolean swapped01 = false;
      if (e0 < e1) {
         float temp = e1;
         e1 = e0;
         e0 = temp;
         temp = y1;
         y1 = y0;
         y0 = temp;
         swapped01 = true;
      }

      boolean swapped02 = false;
      if (e0 < e2) {
         float temp = e2;
         e2 = e0;
         e0 = temp;
         temp = y2;
         y2 = y0;
         y0 = temp;
         swapped02 = true;
      }

      boolean swapped12 = false;
      if (e1 < e2) {
         float temp = e2;
         e2 = e1;
         e1 = temp;
         temp = y2;
         y2 = y1;
         y1 = temp;
         swapped12 = true;
      }

      boolean negate0 = false;
      if (y0 < 0.0F) {
         y0 = -y0;
         negate0 = true;
      }

      boolean negate1 = false;
      if (y1 < 0.0F) {
         y1 = -y1;
         negate1 = true;
      }

      boolean negate2 = false;
      if (y2 < 0.0F) {
         y2 = -y2;
         negate2 = true;
      }

      float distance = distancePointEllipsoidInner(e0, e1, e2, y0, y1, y2, intersection);
      if (intersection != null) {
         if (negate2) {
            intersection.z *= -1.0F;
         }

         if (negate1) {
            intersection.y *= -1.0F;
         }

         if (negate0) {
            intersection.x *= -1.0F;
         }

         if (swapped12) {
            float temp = intersection.y;
            intersection.y = intersection.z;
            intersection.z = temp;
         }

         if (swapped02) {
            float temp = intersection.x;
            intersection.x = intersection.z;
            intersection.z = temp;
         }

         if (swapped01) {
            float temp = intersection.x;
            intersection.x = intersection.y;
            intersection.y = temp;
         }
      }

      return distance;
   }

   private static float distancePointEllipsoidInner(float e0, float e1, float e2, float y0, float y1, float y2, @Nullable Vector3f intersection) {
      if (y2 > 0.0F) {
         if (y1 > 0.0F) {
            if (y0 > 0.0F) {
               float z0 = y0 / e0;
               float z1 = y1 / e1;
               float z2 = y2 / e2;
               float g = sqr(z0) + sqr(z1) + sqr(z2) - 1.0F;
               if (g != 0.0F) {
                  float r0 = sqr(e0 / e2);
                  float r1 = sqr(e1 / e2);
                  float sbar = getRootEllipsoid(r0, r1, z0, z1, z2, g);
                  float x0 = r0 * y0 / (sbar + r0);
                  float x1 = r1 * y1 / (sbar + r1);
                  float x2 = y2 / (sbar + 1.0F);
                  if (intersection != null) {
                     intersection.set(x0, x1, x2);
                  }

                  return (float)Math.sqrt(sqr(x0 - y0) + sqr(x1 - y1) + sqr(x2 - y2));
               } else {
                  if (intersection != null) {
                     intersection.set(y0, y1, y2);
                  }

                  return 0.0F;
               }
            } else if (intersection == null) {
               return distancePointEllipse(e1, e2, y1, y2, null);
            } else {
               Vector2f vector2f = new Vector2f();
               float distance = distancePointEllipse(e1, e2, y1, y2, vector2f);
               intersection.set(0.0F, vector2f.x, vector2f.y);
               return distance;
            }
         } else if (y0 > 0.0F) {
            if (intersection == null) {
               return distancePointEllipse(e0, e2, y0, y2, null);
            } else {
               Vector2f vector2f = new Vector2f();
               float distance = distancePointEllipse(e0, e2, y0, y2, vector2f);
               intersection.set(vector2f.x, 0.0F, vector2f.y);
               return distance;
            }
         } else {
            if (intersection != null) {
               intersection.set(0.0F, 0.0F, e2);
            }

            return Math.abs(y2 - e2);
         }
      } else {
         float denom0 = e0 * 0.0F - e2 * e2;
         float denom1 = e1 * e1 - e2 * e2;
         float numer0 = e0 * y0;
         float numer1 = e1 * y1;
         if (numer0 < denom0 && numer1 < denom1) {
            float xde0 = numer0 / denom0;
            float xde1 = numer1 / denom1;
            float xde0sqr = xde0 * xde0;
            float xde1sqr = xde1 * xde1;
            float discr = 1.0F - xde0sqr - xde1sqr;
            if (discr > 0.0F) {
               float x0 = e0 * xde0;
               float x1 = e1 * xde1;
               float x2 = e2 * (float)Math.sqrt(discr);
               float distance = (float)Math.sqrt((x0 - y0) * (x0 - y0) + (x1 - y1) * (x1 - y1) + x2 * x2);
               if (intersection != null) {
                  intersection.set(x0, x1, x2);
               }

               return distance;
            }
         }

         if (intersection == null) {
            return distancePointEllipse(e0, e1, y0, y1, null);
         } else {
            Vector2f vector2f = new Vector2f();
            float distance = distancePointEllipse(e0, e1, y0, y1, vector2f);
            intersection.set(vector2f.x, vector2f.y, 0.0F);
            return distance;
         }
      }
   }
}
