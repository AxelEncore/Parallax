package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.shape.TerrainDistanceField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectionf;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class ConeRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void cone(
      ChunkedBlockRegion region,
      BlockState block,
      BlockPos center,
      float diameterX,
      float height,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float rounding,
      @Nullable Quaternionf quaternionf
   ) {
      if (!(diameterX <= 0.0F) && !(height <= 0.0F) && !(diameterZ <= 0.0F)) {
         if (quaternionf == null) {
            quaternionf = EMPTY_QUATERNION;
         }

         int centerX = center.getX();
         int centerY = center.getY();
         int centerZ = center.getZ();
         float radiusX = (diameterX - 1.0F) / 2.0F;
         float radiusY = (height - 1.0F) / 2.0F;
         float radiusZ = (diameterZ - 1.0F) / 2.0F;
         int ceilRadiusX = (int)Math.ceil(radiusX);
         int ceilRadiusY = (int)Math.ceil(radiusY);
         int ceilRadiusZ = (int)Math.ceil(radiusZ);
         int maxRadiusX = 0;
         int maxRadiusY = 0;
         int maxRadiusZ = 0;
         Vector3f vector3f = new Vector3f();

         for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
               for (int z = -1; z <= 1; z += 2) {
                  vector3f.set(ceilRadiusX * x, ceilRadiusY * y, ceilRadiusZ * z);
                  quaternionf.transformInverse(vector3f);
                  maxRadiusX = Math.max(maxRadiusX, (int)Math.ceil(Math.abs(vector3f.x)));
                  maxRadiusY = Math.max(maxRadiusY, (int)Math.ceil(Math.abs(vector3f.y)));
                  maxRadiusZ = Math.max(maxRadiusZ, (int)Math.ceil(Math.abs(vector3f.z)));
               }
            }
         }

         float invRadiusSqX = 1.0F / ((radiusX + 0.5F) * (radiusX + 0.5F));
         float invRadiusSqZ = 1.0F / ((radiusZ + 0.5F) * (radiusZ + 0.5F));
         if (!Float.isFinite(invRadiusSqX)) {
            invRadiusSqX = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusSqZ)) {
            invRadiusSqZ = Float.MAX_VALUE;
         }

         float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
         float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
         float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
         float s = 2.0F;
         float mNumer = rounding * rounding * 12.0F;
         float mDenom = 8.0F;
         float m = (float)Math.sqrt(mNumer / mDenom);
         float v = rounding * 2.0F - 2.0F + (float)Math.sqrt(2.0F * m * m - 2.0F * rounding * rounding);
         v = v * (radiusY + 0.5F) + (radiusY + 0.5F);
         float ellipsoidRadiusY = height / 2.0F * (float)Math.sqrt(2.0) - 0.5F;
         float ellipsoidInvRadiusSqY = RasterizationHelper.calcInvRadiusSq(ellipsoidRadiusY);

         for (int x = -maxRadiusX; x <= maxRadiusX; x++) {
            for (int y = -maxRadiusY; y <= maxRadiusY; y++) {
               for (int z = -maxRadiusZ; z <= maxRadiusZ; z++) {
                  vector3f.set(x, y, z);
                  quaternionf.transform(vector3f);
                  float rx = vector3f.x + offsetX;
                  float ry = vector3f.y + offsetY;
                  float rz = vector3f.z + offsetZ;
                  float h2 = radiusY - ry + 1.0F;
                  if ((rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ) * height * height <= h2 * h2
                     && Math.abs(ry) <= radiusY
                     && (!(rounding * height > h2) || !(rx * rx * invRadiusSqX + (ry + v) * (ry + v) * ellipsoidInvRadiusSqY + rz * rz * invRadiusSqZ > m * m))
                     )
                   {
                     if (!hollow) {
                        region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                     } else {
                        for (int i = 0; i < 18; i += 3) {
                           int nx = x + CARDINAL_OFFSETS[i];
                           int ny = y + CARDINAL_OFFSETS[i + 1];
                           int nz = z + CARDINAL_OFFSETS[i + 2];
                           vector3f.set(nx, ny, nz);
                           quaternionf.transform(vector3f);
                           rx = vector3f.x + offsetX;
                           ry = vector3f.y + offsetY;
                           rz = vector3f.z + offsetZ;
                           h2 = radiusY - ry + 1.0F;
                           if ((rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ) * height * height > h2 * h2 || Math.abs(ry) > radiusY) {
                              region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                              break;
                           }

                           if (rounding * height > h2 && rx * rx * invRadiusSqX + (ry + v) * (ry + v) * ellipsoidInvRadiusSqY + rz * rz * invRadiusSqZ > m * m) {
                              region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void coneMetaball(
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      float diameterX,
      float height,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float rounding,
      @Nullable Quaternionf quaternionf
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (metaballRange > 1 && level != null) {
         int extra = (int)Math.ceil(Math.sqrt((metaballRange * metaballRange - metaballRange) / 2.0F)) + 1;
         if (!(diameterX <= 0.0F) && !(height <= 0.0F) && !(diameterZ <= 0.0F)) {
            if (quaternionf == null) {
               quaternionf = EMPTY_QUATERNION;
            }

            float radiusX = (diameterX - 1.0F) / 2.0F;
            float radiusY = (height - 1.0F) / 2.0F;
            float radiusZ = (diameterZ - 1.0F) / 2.0F;
            int ceilRadiusX = (int)Math.ceil(radiusX + extra);
            int ceilRadiusY = (int)Math.ceil(radiusY + extra);
            int ceilRadiusZ = (int)Math.ceil(radiusZ + extra);
            int maxRadiusX = 0;
            int maxRadiusY = 0;
            int maxRadiusZ = 0;
            Vector3f vector3f = new Vector3f();

            for (int x = -1; x <= 1; x += 2) {
               for (int y = -1; y <= 1; y += 2) {
                  for (int z = -1; z <= 1; z += 2) {
                     vector3f.set(ceilRadiusX * x, ceilRadiusY * y, ceilRadiusZ * z);
                     quaternionf.transformInverse(vector3f);
                     maxRadiusX = Math.max(maxRadiusX, (int)Math.ceil(Math.abs(vector3f.x)));
                     maxRadiusY = Math.max(maxRadiusY, (int)Math.ceil(Math.abs(vector3f.y)));
                     maxRadiusZ = Math.max(maxRadiusZ, (int)Math.ceil(Math.abs(vector3f.z)));
                  }
               }
            }

            float invRadiusSqX = RasterizationHelper.calcInvRadiusSq(radiusX);
            float invRadiusSqZ = RasterizationHelper.calcInvRadiusSq(radiusZ);
            float invExpandedRadiusSqX = 1.0F / ((radiusX + extra + 0.5F) * (radiusX + extra + 0.5F));
            float invExpandedRadiusSqZ = 1.0F / ((radiusZ + extra + 0.5F) * (radiusZ + extra + 0.5F));
            if (!Float.isFinite(invExpandedRadiusSqX)) {
               invExpandedRadiusSqX = Float.MAX_VALUE;
            }

            if (!Float.isFinite(invExpandedRadiusSqZ)) {
               invExpandedRadiusSqZ = Float.MAX_VALUE;
            }

            int centerX = center.getX();
            int centerY = center.getY();
            int centerZ = center.getZ();
            float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
            float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
            float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
            float s = 2.0F;
            float mNumer = rounding * rounding * 12.0F;
            float mDenom = 8.0F;
            float m = (float)Math.sqrt(mNumer / mDenom);
            float v = rounding * 2.0F - 2.0F + (float)Math.sqrt(2.0F * m * m - 2.0F * rounding * rounding);
            v = v * (radiusY + 0.5F) + (radiusY + 0.5F);
            float ellipsoidRadiusY = height / 2.0F * (float)Math.sqrt(2.0) - 0.5F;
            float ellipsoidInvRadiusSqY = RasterizationHelper.calcInvRadiusSq(ellipsoidRadiusY);
            float[] distances = TerrainDistanceField.calculateChamferEuclidean(
               level, centerX - maxRadiusX, centerY - maxRadiusY, centerZ - maxRadiusZ, centerX + maxRadiusX, centerY + maxRadiusY, centerZ + maxRadiusZ
            );
            int xIndexOffset = (maxRadiusY * 2 + 3) * (maxRadiusZ * 2 + 3);
            int yIndexOffset = maxRadiusZ * 2 + 3;
            Vector2f intersectionPos = new Vector2f();

            for (int x = -maxRadiusX; x <= maxRadiusX; x++) {
               for (int y = -maxRadiusY; y <= maxRadiusY; y++) {
                  for (int z = -maxRadiusZ; z <= maxRadiusZ; z++) {
                     vector3f.set(x, y, z);
                     quaternionf.transform(vector3f);
                     float rx = vector3f.x + offsetX;
                     float ry = vector3f.y + offsetY;
                     float rz = vector3f.z + offsetZ;
                     float h2 = radiusY - ry + 1.0F;
                     float surface = rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ;
                     boolean insideShape = surface * height * height <= h2 * h2 && Math.abs(ry) <= radiusY + 0.5F;
                     if (insideShape
                        && rounding * height > h2
                        && rx * rx * invRadiusSqX + (ry + v) * (ry + v) * ellipsoidInvRadiusSqY + rz * rz * invRadiusSqZ > m * m) {
                        insideShape = false;
                     }

                     if (!insideShape) {
                        if (rx * rx * invExpandedRadiusSqX + rz * rz * invExpandedRadiusSqZ <= 1.0F && Math.abs(ry) <= radiusY + 0.5F + extra) {
                           int index = (x + maxRadiusX + 1) * xIndexOffset + (y + maxRadiusY + 1) * yIndexOffset + z + maxRadiusZ + 1;
                           float distance = distances[index];
                           if (Float.isFinite(distance) && distance != 0.0F) {
                              float terrainAmount = metaballRange / (metaballRange + distance * distance) - 1.0F / (1 + metaballRange);
                              float surfaceDistanceSq;
                              if (surface < 1.0E-5) {
                                 if (ry < -radiusY - 0.5F) {
                                    float verticalDelta = -radiusY - 0.5F - ry;
                                    surfaceDistanceSq = verticalDelta * verticalDelta;
                                 } else {
                                    float surfaceDistance = RasterizationHelper.distancePointEllipsoid(
                                       (radiusX + 0.5F) * m, (ellipsoidRadiusY + 0.5F) * m, (radiusZ + 0.5F) * m, rx, ry + v, rz, null
                                    );
                                    surfaceDistanceSq = surfaceDistance * surfaceDistance;
                                 }
                              } else if (ry < -(radiusY + 0.5F)) {
                                 surfaceDistanceSq = 0.0F;
                                 if (surface > 1.0F) {
                                    float edgeDistance = RasterizationHelper.distancePointEllipse(radiusX + 0.5F, radiusZ + 0.5F, rx, rz, null);
                                    surfaceDistanceSq += edgeDistance * edgeDistance;
                                 }

                                 float verticalDelta = Math.abs(ry) - (radiusY + 0.5F);
                                 surfaceDistanceSq += verticalDelta * verticalDelta;
                              } else {
                                 float factor = h2 / height;
                                 if (factor > 1.0F) {
                                    factor = 1.0F;
                                 }

                                 if (factor < 0.01F) {
                                    factor = 0.01F;
                                 }

                                 RasterizationHelper.distancePointEllipse((radiusX + 0.5F) * factor, (radiusZ + 0.5F) * factor, rx, rz, intersectionPos);
                                 Vector3f point = Intersectionf.findClosestPointOnLineSegment(
                                    0.0F,
                                    radiusY + 0.5F,
                                    0.0F,
                                    intersectionPos.x / factor,
                                    -radiusY - 0.5F,
                                    intersectionPos.y / factor,
                                    rx,
                                    ry,
                                    rz,
                                    new Vector3f()
                                 );
                                 if (rounding * height > radiusY - point.y + 1.0F) {
                                    float surfaceDistance = RasterizationHelper.distancePointEllipsoid(
                                       (radiusX + 0.5F) * m, (ellipsoidRadiusY + 0.5F) * m, (radiusZ + 0.5F) * m, rx, ry + v, rz, null
                                    );
                                    surfaceDistanceSq = surfaceDistance * surfaceDistance;
                                 } else {
                                    float dx = rx - point.x;
                                    float dy = ry - point.y;
                                    float dz = rz - point.z;
                                    surfaceDistanceSq = dx * dx + dy * dy + dz * dz;
                                 }
                              }

                              float amount = metaballRange / (metaballRange + surfaceDistanceSq) - 1.0F / (1 + metaballRange);
                              if (terrainAmount + amount >= 1.0F) {
                                 region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                              }
                           }
                        }
                     } else if (!hollow) {
                        region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                     } else {
                        for (int i = 0; i < 18; i += 3) {
                           int nx = x + CARDINAL_OFFSETS[i];
                           int ny = y + CARDINAL_OFFSETS[i + 1];
                           int nz = z + CARDINAL_OFFSETS[i + 2];
                           vector3f.set(nx, ny, nz);
                           quaternionf.transform(vector3f);
                           rx = vector3f.x + offsetX;
                           ry = vector3f.y + offsetY;
                           rz = vector3f.z + offsetZ;
                           h2 = radiusY - ry + 1.0F;
                           if ((rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ) * height * height > h2 * h2 || Math.abs(ry) > radiusY + 0.5F) {
                              region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                              break;
                           }

                           if (rounding * height > h2 && rx * rx * invRadiusSqX + (ry + v) * (ry + v) * ellipsoidInvRadiusSqY + rz * rz * invRadiusSqZ > m * m) {
                              region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         cone(region, block, center, diameterX, height, diameterZ, hollow, centerEvenDiameters, rounding, quaternionf);
      }
   }
}
