package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DodecahedronRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void dodhecahedron(
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

         int centerX = center.getX();
         int centerY = center.getY();
         int centerZ = center.getZ();
         float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
         float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
         float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
         float goldenRatio = (1.0F + (float)Math.sqrt(5.0)) / 2.0F;
         float[] planes = new float[18];

         for (int i = 0; i < 18; i += 3) {
            int coordMult = (i / 3 & 1) == 0 ? 1 : -1;
            if (i < 6) {
               planes[i] = 1.0F / (radiusX + 0.5F);
               planes[i + 1] = coordMult * goldenRatio / (radiusY + 0.5F);
               planes[i + 2] = 0.0F;
            } else if (i < 12) {
               planes[i] = 0.0F;
               planes[i + 1] = 1.0F / (radiusY + 0.5F);
               planes[i + 2] = coordMult * goldenRatio / (radiusZ + 0.5F);
            } else {
               planes[i] = coordMult * goldenRatio / (radiusX + 0.5F);
               planes[i + 1] = 0.0F;
               planes[i + 2] = 1.0F / (radiusZ + 0.5F);
            }
         }

         for (int x = -maxRadiusX; x <= maxRadiusX; x++) {
            for (int y = -maxRadiusY; y <= maxRadiusY; y++) {
               label114:
               for (int z = -maxRadiusZ; z <= maxRadiusZ; z++) {
                  vector3f.set(x, y, z);
                  quaternionf.transform(vector3f);
                  float rx = vector3f.x + offsetX;
                  float ry = vector3f.y + offsetY;
                  float rz = vector3f.z + offsetZ;
                  if (!(ry < cutoffY)) {
                     for (int ix = 0; ix < 18; ix += 3) {
                        if (Math.abs(planes[ix] * rx + planes[ix + 1] * ry + planes[ix + 2] * rz) > goldenRatio) {
                           continue label114;
                        }
                     }

                     if (!hollow) {
                        region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                     } else {
                        for (int ixx = 0; ixx < 18; ixx += 3) {
                           int nx = x + CARDINAL_OFFSETS[ixx];
                           int ny = y + CARDINAL_OFFSETS[ixx + 1];
                           int nz = z + CARDINAL_OFFSETS[ixx + 2];
                           vector3f.set(nx, ny, nz);
                           quaternionf.transform(vector3f);
                           rx = vector3f.x + offsetX;
                           ry = vector3f.y + offsetY;
                           rz = vector3f.z + offsetZ;

                           for (int j = 0; j < 18; j += 3) {
                              if (Math.abs(planes[j] * rx + planes[j + 1] * ry + planes[j + 2] * rz) > goldenRatio) {
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

   public static void dodhecahedronMetaball(
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

            Quaternionf quaternionFinal = quaternionf;
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

            float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
            float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
            float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
            float goldenRatio = (1.0F + (float)Math.sqrt(5.0)) / 2.0F;
            float[] planes = new float[18];

            for (int i = 0; i < 18; i += 3) {
               int coordMult = (i / 3 & 1) == 0 ? 1 : -1;
               if (i < 6) {
                  planes[i] = 1.0F / (radiusX + 0.5F);
                  planes[i + 1] = coordMult * goldenRatio / (radiusY + 0.5F);
                  planes[i + 2] = 0.0F;
               } else if (i < 12) {
                  planes[i] = 0.0F;
                  planes[i + 1] = 1.0F / (radiusY + 0.5F);
                  planes[i + 2] = coordMult * goldenRatio / (radiusZ + 0.5F);
               } else {
                  planes[i] = coordMult * goldenRatio / (radiusX + 0.5F);
                  planes[i + 1] = 0.0F;
                  planes[i + 2] = 1.0F / (radiusZ + 0.5F);
               }
            }

            RasterizationHelper.metaball(level, region, metaballRange, block, center, maxRadiusX, maxRadiusY, maxRadiusZ, (x, y, z) -> {
               vector3f.set(x, y, z);
               quaternionFinal.transform(vector3f);
               float rx = vector3f.x + offsetX;
               float ry = vector3f.y + offsetY;
               float rz = vector3f.z + offsetZ;

               for (int ixx = 0; ixx < 18; ixx += 3) {
                  if (Math.abs(planes[ixx] * rx + planes[ixx + 1] * ry + planes[ixx + 2] * rz) > goldenRatio) {
                     return false;
                  }
               }

               if (!hollow) {
                  return true;
               } else {
                  for (int ix = 0; ix < 18; ix += 3) {
                     int nx = x + CARDINAL_OFFSETS[ix];
                     int ny = y + CARDINAL_OFFSETS[ix + 1];
                     int nz = z + CARDINAL_OFFSETS[ix + 2];
                     vector3f.set(nx, ny, nz);
                     quaternionFinal.transform(vector3f);
                     rx = vector3f.x + offsetX;
                     ry = vector3f.y + offsetY;
                     rz = vector3f.z + offsetZ;

                     for (int j = 0; j < 18; j += 3) {
                        if (Math.abs(planes[j] * rx + planes[j + 1] * ry + planes[j + 2] * rz) > goldenRatio) {
                           return true;
                        }
                     }
                  }

                  return false;
               }
            });
         }
      } else {
         dodhecahedron(region, block, center, diameterX, diameterY, diameterZ, hollow, centerEvenDiameters, Float.NEGATIVE_INFINITY, quaternionf);
      }
   }
}
