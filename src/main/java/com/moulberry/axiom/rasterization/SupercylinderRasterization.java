package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SupercylinderRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void supercylinder(
      ChunkedBlockRegion region,
      BlockState block,
      BlockPos center,
      float diameterX,
      float diameterY,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float cutoffY,
      float exponent,
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

         int powLutSize = Math.max(maxRadiusX, Math.max(maxRadiusY, maxRadiusZ)) * 10 + 10;
         double[] powLookup = new double[powLutSize];

         for (int i = 0; i < powLookup.length; i++) {
            powLookup[i] = Math.pow(i / 10.0F, exponent);
         }

         float invRadiusX = RasterizationHelper.finiteOrMaxValue(1.0F / (float)Math.pow(radiusX + 0.5F, exponent));
         float invRadiusZ = RasterizationHelper.finiteOrMaxValue(1.0F / (float)Math.pow(radiusZ + 0.5F, exponent));
         float invRadiusY = radiusX > radiusZ ? invRadiusX : invRadiusZ;
         float verticalOffset = radiusY - Math.max(radiusX, radiusZ);
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
                  if (!(ry < cutoffY)) {
                     ry = Math.max(0.0F, Math.abs(ry) - verticalOffset);
                     int rxPowIndex = (int)Math.abs(rx * 10.0F);
                     int ryPowIndex = (int)Math.abs(ry * 10.0F);
                     int rzPowIndex = (int)Math.abs(rz * 10.0F);
                     if (rxPowIndex < powLutSize && ryPowIndex < powLutSize && rzPowIndex < powLutSize) {
                        double rxPow = powLookup[rxPowIndex];
                        double ryPow = powLookup[ryPowIndex];
                        double rzPow = powLookup[rzPowIndex];
                        if (rxPow * invRadiusX + ryPow * invRadiusY + rzPow * invRadiusZ <= 1.0) {
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
                                 ry = Math.max(0.0F, Math.abs(ry) - verticalOffset);
                                 rxPowIndex = (int)Math.abs(rx * 10.0F);
                                 ryPowIndex = (int)Math.abs(ry * 10.0F);
                                 rzPowIndex = (int)Math.abs(rz * 10.0F);
                                 if (rxPowIndex >= powLutSize || ryPowIndex >= powLutSize || rzPowIndex >= powLutSize) {
                                    region.addBlock(x + centerX, y + centerY, z + centerZ, block);
                                    break;
                                 }

                                 rxPow = powLookup[rxPowIndex];
                                 ryPow = powLookup[ryPowIndex];
                                 rzPow = powLookup[rzPowIndex];
                                 if (rxPow * invRadiusX + ryPow * invRadiusY + rzPow * invRadiusZ > 1.0) {
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
   }

   public static void supercylinderMetaball(
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      float diameterX,
      float diameterY,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float exponent,
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

            int powLutSize = Math.max(maxRadiusX, Math.max(maxRadiusY, maxRadiusZ)) * 10;
            float[] powLookup = new float[powLutSize];

            for (int i = 0; i < powLookup.length; i++) {
               powLookup[i] = (float)Math.pow(i / 10.0F, exponent);
            }

            float invRadiusX = RasterizationHelper.finiteOrMaxValue(1.0F / (float)Math.pow(radiusX + 0.5F, exponent));
            float invRadiusZ = RasterizationHelper.finiteOrMaxValue(1.0F / (float)Math.pow(radiusZ + 0.5F, exponent));
            float invRadiusY = radiusX > radiusZ ? invRadiusX : invRadiusZ;
            float verticalOffset = radiusY - Math.max(radiusX, radiusZ);
            float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
            float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
            float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
            RasterizationHelper.metaball(level, region, metaballRange, block, center, maxRadiusX, maxRadiusY, maxRadiusZ, (x, y, z) -> {
               vector3f.set(x, y, z);
               quaternionFinal.transform(vector3f);
               float rx = vector3f.x + offsetX;
               float ry = vector3f.y + offsetY;
               float rz = vector3f.z + offsetZ;
               ry = Math.max(0.0F, Math.abs(ry) - verticalOffset);
               int rxPowIndex = (int)Math.abs(rx * 10.0F);
               int ryPowIndex = (int)Math.abs(ry * 10.0F);
               int rzPowIndex = (int)Math.abs(rz * 10.0F);
               if (rxPowIndex >= powLutSize) {
                  return false;
               } else if (ryPowIndex >= powLutSize) {
                  return false;
               } else if (rzPowIndex >= powLutSize) {
                  return false;
               } else {
                  float rxPow = powLookup[rxPowIndex];
                  float ryPow = powLookup[ryPowIndex];
                  float rzPow = powLookup[rzPowIndex];
                  if (rxPow * invRadiusX + ryPow * invRadiusY + rzPow * invRadiusZ <= 1.0F) {
                     if (!hollow) {
                        return true;
                     }

                     for (int i = 0; i < 18; i += 3) {
                        int nx = x + CARDINAL_OFFSETS[i];
                        int ny = y + CARDINAL_OFFSETS[i + 1];
                        int nz = z + CARDINAL_OFFSETS[i + 2];
                        vector3f.set(nx, ny, nz);
                        quaternionFinal.transform(vector3f);
                        rx = vector3f.x + offsetX;
                        ry = vector3f.y + offsetY;
                        rz = vector3f.z + offsetZ;
                        ry = Math.max(0.0F, Math.abs(ry) - verticalOffset);
                        rxPowIndex = (int)Math.abs(rx * 10.0F);
                        ryPowIndex = (int)Math.abs(ry * 10.0F);
                        rzPowIndex = (int)Math.abs(rz * 10.0F);
                        if (rxPowIndex < powLutSize && ryPowIndex < powLutSize && rzPowIndex < powLutSize) {
                           rxPow = powLookup[rxPowIndex];
                           ryPow = powLookup[ryPowIndex];
                           rzPow = powLookup[rzPowIndex];
                           if (rxPow * invRadiusX + ryPow * invRadiusY + rzPow * invRadiusZ > 1.0F) {
                              return true;
                           }
                        }
                     }
                  }

                  return false;
               }
            });
         }
      } else {
         supercylinder(region, block, center, diameterX, diameterY, diameterZ, hollow, centerEvenDiameters, Float.NEGATIVE_INFINITY, exponent, quaternionf);
      }
   }
}
