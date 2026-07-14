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

public class TubeRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void tube(
      ChunkedBlockRegion region,
      BlockState block,
      BlockPos center,
      float diameterX,
      float height,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float thickness,
      @Nullable Quaternionf quaternionf
   ) {
      if (!(thickness * 2.0F >= diameterX) && !(thickness * 2.0F >= diameterZ)) {
         if (!(diameterX <= 0.0F) && !(height <= 0.0F) && !(diameterZ <= 0.0F)) {
            if (quaternionf == null) {
               quaternionf = EMPTY_QUATERNION;
            }

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

            float invRadiusSqX = RasterizationHelper.calcInvRadiusSq(radiusX);
            float invRadiusSqZ = RasterizationHelper.calcInvRadiusSq(radiusZ);
            float invInnerRadiusSqX = RasterizationHelper.calcInvRadiusSq(radiusX - thickness);
            float invInnerRadiusSqZ = RasterizationHelper.calcInvRadiusSq(radiusZ - thickness);
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
                     if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ <= 1.0F
                        && rx * rx * invInnerRadiusSqX + rz * rz * invInnerRadiusSqZ > 1.0F
                        && Math.abs(ry) <= radiusY + 0.5F) {
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
                              if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ > 1.0F
                                 || rx * rx * invInnerRadiusSqX + rz * rz * invInnerRadiusSqZ <= 1.0F
                                 || Math.abs(ry) > radiusY + 0.5F) {
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
      } else {
         CylinderRasterization.cylinder(region, block, center, diameterX, height, diameterZ, false, centerEvenDiameters, quaternionf);
      }
   }

   public static void tubeMetaball(
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      float diameterX,
      float height,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float thickness,
      @Nullable Quaternionf quaternionf
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (metaballRange > 1 && level != null) {
         int extra = (int)Math.ceil(Math.sqrt((metaballRange * metaballRange - metaballRange) / 2.0F)) + 1;
         if (!(thickness * 2.0F >= diameterX) && !(thickness * 2.0F >= diameterZ)) {
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
               float invInnerRadiusSqX = RasterizationHelper.calcInvRadiusSq(radiusX - thickness);
               float invInnerRadiusSqZ = RasterizationHelper.calcInvRadiusSq(radiusZ - thickness);
               float invExpandedRadiusSqX = RasterizationHelper.calcInvRadiusSq(radiusX + extra);
               float invExpandedRadiusSqZ = RasterizationHelper.calcInvRadiusSq(radiusZ + extra);
               float invContractedInnerRadiusSqX = RasterizationHelper.calcInvRadiusSq(Math.max(0.0F, radiusX - thickness - extra));
               float invContractedInnerRadiusSqZ = RasterizationHelper.calcInvRadiusSq(Math.max(0.0F, radiusZ - thickness - extra));
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
                        float outer = rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ;
                        float inner = rx * rx * invInnerRadiusSqX + rz * rz * invInnerRadiusSqZ;
                        if (!(outer <= 1.0F) || !(inner > 1.0F) || !(Math.abs(ry) <= radiusY + 0.5F)) {
                           if (rx * rx * invExpandedRadiusSqX + rz * rz * invExpandedRadiusSqZ <= 1.0F
                              && rx * rx * invContractedInnerRadiusSqX + rz * rz * invContractedInnerRadiusSqZ > 1.0F
                              && Math.abs(ry) <= radiusY + 0.5F + extra) {
                              int index = (x + maxRadiusX + 1) * xIndexOffset + (y + maxRadiusY + 1) * yIndexOffset + z + maxRadiusZ + 1;
                              float distance = distances[index];
                              if (Float.isFinite(distance) && distance != 0.0F) {
                                 float terrainAmount = metaballRange / (metaballRange + distance * distance);
                                 if (!(terrainAmount < 2.0F / (1 + metaballRange))) {
                                    float surfaceDistanceSq = 0.0F;
                                    if (outer > 1.0F) {
                                       float xzDistance = RasterizationHelper.distancePointEllipse(radiusX + 0.5F, radiusZ + 0.5F, rx, rz, null);
                                       surfaceDistanceSq += xzDistance * xzDistance;
                                    } else if (inner <= 1.0F) {
                                       float xzDistance = RasterizationHelper.distancePointEllipse(
                                          radiusX + 0.5F - thickness, radiusZ + 0.5F - thickness, rx, rz, null
                                       );
                                       surfaceDistanceSq += xzDistance * xzDistance;
                                    }

                                    float verticalDelta = Math.abs(ry) - (radiusY + 0.5F);
                                    if (verticalDelta > 0.0F) {
                                       surfaceDistanceSq += verticalDelta * verticalDelta;
                                    }

                                    float amount = metaballRange / (metaballRange + surfaceDistanceSq);
                                    if (terrainAmount + amount >= 1.0F + 2.0F / (1 + metaballRange)) {
                                       region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                                    }
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
                              if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ > 1.0F
                                 || rx * rx * invInnerRadiusSqX + rz * rz * invInnerRadiusSqZ <= 1.0F
                                 || Math.abs(ry) > radiusY + 0.5F) {
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
            CylinderRasterization.cylinderMetaball(region, metaballRange, block, center, diameterX, height, diameterZ, false, centerEvenDiameters, quaternionf);
         }
      } else {
         tube(region, block, center, diameterX, height, diameterZ, hollow, centerEvenDiameters, thickness, quaternionf);
      }
   }
}
