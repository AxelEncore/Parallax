package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.shape.TerrainDistanceField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SphereRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void sphere(
      ChunkedBlockRegion region,
      BlockState block,
      BlockPos center,
      float diameterX,
      float diameterY,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float cutoffY,
      @Nullable Quaternionf quaternionf
   ) {
      if (!(diameterX <= 0.0F) && !(diameterY <= 0.0F) && !(diameterZ <= 0.0F)) {
         if (quaternionf == null) {
            quaternionf = EMPTY_QUATERNION;
         }

         float radiusX = (diameterX - 1.0F) / 2.0F;
         float radiusY = (diameterY - 1.0F) / 2.0F;
         float radiusZ = (diameterZ - 1.0F) / 2.0F;
         int ceilRadiusX = (int)Math.ceil(radiusX);
         int ceilRadiusY = (int)Math.ceil(radiusY);
         int ceilRadiusZ = (int)Math.ceil(radiusZ);
         int maxRadiusX = 0;
         int maxRadiusY = 0;
         int maxRadiusZ = 0;
         Vector3f vector3f = new Vector3f();

         for (int x = -1; x <= 1; x += 2) {
            for (float y = -1.0F; y <= 1.0F; y += 2.0F) {
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
         float invRadiusSqY = 1.0F / ((radiusY + 0.5F) * (radiusY + 0.5F));
         float invRadiusSqZ = 1.0F / ((radiusZ + 0.5F) * (radiusZ + 0.5F));
         if (!Float.isFinite(invRadiusSqX)) {
            invRadiusSqX = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusSqY)) {
            invRadiusSqY = Float.MAX_VALUE;
         }

         if (!Float.isFinite(invRadiusSqZ)) {
            invRadiusSqZ = Float.MAX_VALUE;
         }

         int centerX = center.getX();
         int centerY = center.getY();
         int centerZ = center.getZ();
         float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
         float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
         float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;

         for (int x = -maxRadiusX; x <= maxRadiusX; x++) {
            for (int y = -maxRadiusY; y <= maxRadiusY; y++) {
               for (int z = -maxRadiusZ; z <= maxRadiusZ; z++) {
                  vector3f.set(x, y, z);
                  quaternionf.transform(vector3f);
                  float rx = vector3f.x + offsetX;
                  float ry = vector3f.y + offsetY;
                  float rz = vector3f.z + offsetZ;
                  if (!(ry < cutoffY) && rx * rx * invRadiusSqX + ry * ry * invRadiusSqY + rz * rz * invRadiusSqZ <= 1.0F) {
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
                           if (rx * rx * invRadiusSqX + ry * ry * invRadiusSqY + rz * rz * invRadiusSqZ > 1.0F) {
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

   public static void sphereMetaball(
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      float diameterX,
      float diameterY,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      @Nullable Quaternionf quaternionf
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (metaballRange > 1 && level != null) {
         int extra = (int)Math.ceil(Math.sqrt((metaballRange * metaballRange - metaballRange) / 2.0F)) + 1;
         if (!(diameterX <= 0.0F) && !(diameterY <= 0.0F) && !(diameterZ <= 0.0F)) {
            if (quaternionf == null) {
               quaternionf = EMPTY_QUATERNION;
            }

            float radiusX = (diameterX - 1.0F) / 2.0F;
            float radiusY = (diameterY - 1.0F) / 2.0F;
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
            float invRadiusSqY = RasterizationHelper.calcInvRadiusSq(radiusY);
            float invRadiusSqZ = RasterizationHelper.calcInvRadiusSq(radiusZ);
            float invExpandedRadiusSqX = 1.0F / ((radiusX + extra + 0.5F) * (radiusX + extra + 0.5F));
            float invExpandedRadiusSqY = 1.0F / ((radiusY + extra + 0.5F) * (radiusY + extra + 0.5F));
            float invExpandedRadiusSqZ = 1.0F / ((radiusZ + extra + 0.5F) * (radiusZ + extra + 0.5F));
            if (!Float.isFinite(invExpandedRadiusSqX)) {
               invExpandedRadiusSqX = Float.MAX_VALUE;
            }

            if (!Float.isFinite(invExpandedRadiusSqY)) {
               invExpandedRadiusSqY = Float.MAX_VALUE;
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
            float[] distances = TerrainDistanceField.calculateChamferEuclidean(
               level, centerX - maxRadiusX, centerY - maxRadiusY, centerZ - maxRadiusZ, centerX + maxRadiusX, centerY + maxRadiusY, centerZ + maxRadiusZ
            );
            int xIndexOffset = (maxRadiusY * 2 + 3) * (maxRadiusZ * 2 + 3);
            int yIndexOffset = maxRadiusZ * 2 + 3;

            for (int x = -maxRadiusX; x <= maxRadiusX; x++) {
               for (int y = -maxRadiusY; y <= maxRadiusY; y++) {
                  for (int z = -maxRadiusZ; z <= maxRadiusZ; z++) {
                     vector3f.set(x, y, z);
                     quaternionf.transform(vector3f);
                     float rx = vector3f.x + offsetX;
                     float ry = vector3f.y + offsetY;
                     float rz = vector3f.z + offsetZ;
                     float surface = rx * rx * invRadiusSqX + ry * ry * invRadiusSqY + rz * rz * invRadiusSqZ;
                     if (surface <= 1.0F) {
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
                              if (rx * rx * invRadiusSqX + ry * ry * invRadiusSqY + rz * rz * invRadiusSqZ > 1.0F) {
                                 region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                                 break;
                              }
                           }
                        }
                     } else if (rx * rx * invExpandedRadiusSqX + ry * ry * invExpandedRadiusSqY + rz * rz * invExpandedRadiusSqZ <= 1.0F) {
                        int index = (x + maxRadiusX + 1) * xIndexOffset + (y + maxRadiusY + 1) * yIndexOffset + z + maxRadiusZ + 1;
                        float distance = distances[index];
                        if (Float.isFinite(distance) && distance != 0.0F) {
                           float terrainAmount = metaballRange / (metaballRange + distance * distance);
                           if (!(terrainAmount < 2.0F / (1 + metaballRange))) {
                              float surfaceDistance = RasterizationHelper.distancePointEllipsoid(
                                 radiusX + 0.5F, radiusY + 0.5F, radiusZ + 0.5F, rx, ry, rz, null
                              );
                              float amount = metaballRange / (metaballRange + surfaceDistance * surfaceDistance);
                              if (terrainAmount + amount >= 1.0F + 2.0F / (1 + metaballRange)) {
                                 region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         sphere(region, block, center, diameterX, diameterY, diameterZ, hollow, centerEvenDiameters, Float.NEGATIVE_INFINITY, quaternionf);
      }
   }
}
