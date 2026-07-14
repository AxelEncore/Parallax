package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TorusRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void torus(
      ChunkedBlockRegion region,
      BlockState block,
      BlockPos center,
      float diameterX,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float cutoffY,
      float ringHorizontal,
      float ringVertical,
      @Nullable Quaternionf quaternionf
   ) {
      if (!(diameterX <= 0.0F) && !(diameterZ <= 0.0F)) {
         if (quaternionf == null) {
            quaternionf = EMPTY_QUATERNION;
         }

         if (diameterX < ringHorizontal) {
            diameterX = ringHorizontal;
         }

         if (diameterZ < ringHorizontal) {
            diameterZ = ringHorizontal;
         }

         float radiusX = (diameterX - 1.0F) / 2.0F;
         float radiusZ = (diameterZ - 1.0F) / 2.0F;
         float ringRadiusV = (ringVertical - 1.0F) / 2.0F;
         int ceilRadiusX = (int)Math.ceil(radiusX);
         int ceilRingRadiusV = (int)Math.ceil(ringRadiusV);
         int ceilRadiusZ = (int)Math.ceil(radiusZ);
         int maxRadiusX = 0;
         int maxRadiusY = 0;
         int maxRadiusZ = 0;
         Vector3f vector3f = new Vector3f();

         for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
               for (int z = -1; z <= 1; z += 2) {
                  vector3f.set(ceilRadiusX * x, ceilRingRadiusV * y, ceilRadiusZ * z);
                  quaternionf.transformInverse(vector3f);
                  maxRadiusX = Math.max(maxRadiusX, (int)Math.ceil(Math.abs(vector3f.x)));
                  maxRadiusY = Math.max(maxRadiusY, (int)Math.ceil(Math.abs(vector3f.y)));
                  maxRadiusZ = Math.max(maxRadiusZ, (int)Math.ceil(Math.abs(vector3f.z)));
               }
            }
         }

         float majorRadius = Math.min(radiusX, radiusZ) + 0.5F - ringHorizontal / 2.0F;
         float invRingVerticalSq = RasterizationHelper.calcInvRadiusSq(ringRadiusV);
         int centerX = center.getX();
         int centerY = center.getY();
         int centerZ = center.getZ();
         float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
         float offsetY = centerEvenDiameters ? -(ringRadiusV % 1.0F) : 0.0F;
         float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
         float subX = radiusX - radiusZ;
         float subZ = radiusZ - radiusX;

         for (int x = -maxRadiusX; x <= maxRadiusX; x++) {
            for (int y = -maxRadiusY; y <= maxRadiusY; y++) {
               for (int z = -maxRadiusZ; z <= maxRadiusZ; z++) {
                  vector3f.set(x, y, z);
                  quaternionf.transform(vector3f);
                  float rx = vector3f.x + offsetX;
                  float ry = vector3f.y + offsetY;
                  float rz = vector3f.z + offsetZ;
                  if (!(ry < cutoffY)) {
                     if (Math.abs(rx) < subX) {
                        rx = 0.0F;
                     } else if (subX > 0.0F) {
                        rx -= Math.copySign(subX, rx);
                     }

                     if (Math.abs(rz) < subZ) {
                        rz = 0.0F;
                     } else if (subZ > 0.0F) {
                        rz -= Math.copySign(subZ, rz);
                     }

                     float majorDistance = (float)Math.sqrt(rx * rx + rz * rz) - majorRadius;
                     float majorDistanceSq = majorDistance * majorDistance;
                     if (!(majorDistanceSq / (ringHorizontal * ringHorizontal / 4.0F) + ry * ry * invRingVerticalSq > 1.0F)) {
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
                              if (Math.abs(rx) < subX) {
                                 rx = 0.0F;
                              } else if (subX > 0.0F) {
                                 rx -= Math.copySign(subX, rx);
                              }

                              if (Math.abs(rz) < subZ) {
                                 rz = 0.0F;
                              } else if (subZ > 0.0F) {
                                 rz -= Math.copySign(subZ, rz);
                              }

                              majorDistance = (float)Math.sqrt(rx * rx + rz * rz) - majorRadius;
                              majorDistanceSq = majorDistance * majorDistance;
                              if (majorDistanceSq / (ringHorizontal * ringHorizontal / 4.0F) + ry * ry * invRingVerticalSq > 1.0F) {
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
   }

   public static void torusMetaball(
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      float diameterX,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float ringHorizontal,
      float ringVertical,
      @Nullable Quaternionf quaternionf
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (metaballRange > 1 && level != null) {
         int extra = (int)Math.ceil(Math.sqrt((metaballRange * metaballRange - metaballRange) / 2.0F)) + 1;
         if (quaternionf == null) {
            quaternionf = EMPTY_QUATERNION;
         }

         Quaternionf quaternionFinal = quaternionf;
         if (diameterX < ringHorizontal) {
            diameterX = ringHorizontal;
         }

         if (diameterZ < ringHorizontal) {
            diameterZ = ringHorizontal;
         }

         float radiusX = (diameterX - 1.0F) / 2.0F;
         float radiusZ = (diameterZ - 1.0F) / 2.0F;
         float ringRadiusV = (ringVertical - 1.0F) / 2.0F;
         int ceilRadiusX = (int)Math.ceil(radiusX + extra);
         int ceilRingRadiusV = (int)Math.ceil(ringRadiusV + extra);
         int ceilRadiusZ = (int)Math.ceil(radiusZ + extra);
         int maxRadiusX = 0;
         int maxRadiusY = 0;
         int maxRadiusZ = 0;
         Vector3f vector3f = new Vector3f();

         for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
               for (int z = -1; z <= 1; z += 2) {
                  vector3f.set(ceilRadiusX * x, ceilRingRadiusV * y, ceilRadiusZ * z);
                  quaternionf.transformInverse(vector3f);
                  maxRadiusX = Math.max(maxRadiusX, (int)Math.ceil(Math.abs(vector3f.x)));
                  maxRadiusY = Math.max(maxRadiusY, (int)Math.ceil(Math.abs(vector3f.y)));
                  maxRadiusZ = Math.max(maxRadiusZ, (int)Math.ceil(Math.abs(vector3f.z)));
               }
            }
         }

         float majorRadius = Math.min(radiusX, radiusZ) + 0.5F - ringHorizontal / 2.0F;
         float invRingVerticalSq = RasterizationHelper.calcInvRadiusSq(ringRadiusV);
         float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
         float offsetY = centerEvenDiameters ? -(ringRadiusV % 1.0F) : 0.0F;
         float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
         float subX = radiusX - radiusZ;
         float subZ = radiusZ - radiusX;
         RasterizationHelper.metaball(level, region, metaballRange, block, center, maxRadiusX, maxRadiusY, maxRadiusZ, (x, y, z) -> {
            vector3f.set(x, y, z);
            quaternionFinal.transform(vector3f);
            float rx = vector3f.x + offsetX;
            float ry = vector3f.y + offsetY;
            float rz = vector3f.z + offsetZ;
            if (Math.abs(rx) < subX) {
               rx = 0.0F;
            } else if (subX > 0.0F) {
               rx -= Math.copySign(subX, rx);
            }

            if (Math.abs(rz) < subZ) {
               rz = 0.0F;
            } else if (subZ > 0.0F) {
               rz -= Math.copySign(subZ, rz);
            }

            float majorDistance = (float)Math.sqrt(rx * rx + rz * rz) - majorRadius;
            float majorDistanceSq = majorDistance * majorDistance;
            if (majorDistanceSq / (ringHorizontal * ringHorizontal / 4.0F) + ry * ry * invRingVerticalSq > 1.0F) {
               return false;
            } else if (!hollow) {
               return true;
            } else {
               for (int i = 0; i < 18; i += 3) {
                  int nx = x + CARDINAL_OFFSETS[i];
                  int ny = y + CARDINAL_OFFSETS[i + 1];
                  int nz = z + CARDINAL_OFFSETS[i + 2];
                  vector3f.set(nx, ny, nz);
                  quaternionFinal.transform(vector3f);
                  rx = vector3f.x + offsetX;
                  ry = vector3f.y + offsetY;
                  rz = vector3f.z + offsetZ;
                  if (Math.abs(rx) < subX) {
                     rx = 0.0F;
                  } else if (subX > 0.0F) {
                     rx -= Math.copySign(subX, rx);
                  }

                  if (Math.abs(rz) < subZ) {
                     rz = 0.0F;
                  } else if (subZ > 0.0F) {
                     rz -= Math.copySign(subZ, rz);
                  }

                  majorDistance = (float)Math.sqrt(rx * rx + rz * rz) - majorRadius;
                  majorDistanceSq = majorDistance * majorDistance;
                  if (majorDistanceSq / (ringHorizontal * ringHorizontal / 4.0F) + ry * ry * invRingVerticalSq > 1.0F) {
                     return true;
                  }
               }

               return false;
            }
         });
      } else {
         torus(region, block, center, diameterX, diameterZ, hollow, centerEvenDiameters, Float.NEGATIVE_INFINITY, ringHorizontal, ringVertical, quaternionf);
      }
   }
}
