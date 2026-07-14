package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CapsuleRasterization {
   private static final Quaternionf EMPTY_QUATERNION = new Quaternionf();
   private static final int[] CARDINAL_OFFSETS = new int[]{0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0};

   public static void capsule(
      ChunkedBlockRegion region,
      BlockState block,
      BlockPos center,
      float diameterX,
      float height,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float capHeightFactor,
      @Nullable Quaternionf quaternionf
   ) {
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

         float invRadiusSqX = RasterizationHelper.finiteOrMaxValue(1.0F / ((radiusX + 0.5F) * (radiusX + 0.5F)));
         float invRadiusSqZ = RasterizationHelper.finiteOrMaxValue(1.0F / ((radiusZ + 0.5F) * (radiusZ + 0.5F)));
         float capWidth = Math.max(radiusX, radiusZ);
         float capHeight = capWidth * capHeightFactor;
         float capRadius = (capWidth * capWidth + capHeight * capHeight) / (2.0F * capHeight);
         float invCapRadiusSqX = RasterizationHelper.calcInvRadiusSq(capRadius * radiusX / capWidth);
         float invCapRadiusSqZ = RasterizationHelper.calcInvRadiusSq(capRadius * radiusZ / capWidth);
         float invCapRadiusSqY = radiusX > radiusZ ? invCapRadiusSqX : invCapRadiusSqZ;
         float verticalOffset = radiusY - capHeight;
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
                  boolean inside;
                  if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ > 1.0F) {
                     inside = false;
                  } else if (Math.abs(ry) <= verticalOffset) {
                     inside = true;
                  } else {
                     float capY = Math.abs(ry) - verticalOffset + capRadius - capHeight;
                     inside = rx * rx * invCapRadiusSqX + capY * capY * invCapRadiusSqY + rz * rz * invCapRadiusSqZ <= 1.0F;
                  }

                  if (inside) {
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
                           boolean insideOther;
                           if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ > 1.0F) {
                              insideOther = false;
                           } else if (Math.abs(ry) <= verticalOffset) {
                              insideOther = true;
                           } else {
                              float capY = Math.abs(ry) - verticalOffset + capRadius - capHeight;
                              insideOther = rx * rx * invCapRadiusSqX + capY * capY * invCapRadiusSqY + rz * rz * invCapRadiusSqZ <= 1.0F;
                           }

                           if (!insideOther) {
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

   public static void capsuleMetaball(
      ChunkedBlockRegion region,
      int metaballRange,
      BlockState block,
      BlockPos center,
      float diameterX,
      float height,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float capHeightFactor,
      @Nullable Quaternionf quaternionf
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (metaballRange > 1 && level != null) {
         int extra = (int)Math.ceil(Math.sqrt((metaballRange * metaballRange - metaballRange) / 2.0F)) + 1;
         if (!(diameterX <= 0.0F) && !(height <= 0.0F) && !(diameterZ <= 0.0F)) {
            if (quaternionf == null) {
               quaternionf = EMPTY_QUATERNION;
            }

            Quaternionf quaternionFinal = quaternionf;
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

            float invRadiusSqX = RasterizationHelper.finiteOrMaxValue(1.0F / ((radiusX + 0.5F) * (radiusX + 0.5F)));
            float invRadiusSqZ = RasterizationHelper.finiteOrMaxValue(1.0F / ((radiusZ + 0.5F) * (radiusZ + 0.5F)));
            float capWidth = Math.max(radiusX, radiusZ);
            float capHeight = capWidth * capHeightFactor;
            float capRadius = (capWidth * capWidth + capHeight * capHeight) / (2.0F * capHeight);
            float invCapRadiusSqX = RasterizationHelper.calcInvRadiusSq(capRadius * radiusX / capWidth);
            float invCapRadiusSqZ = RasterizationHelper.calcInvRadiusSq(capRadius * radiusZ / capWidth);
            float invCapRadiusSqY = radiusX > radiusZ ? invCapRadiusSqX : invCapRadiusSqZ;
            float verticalOffset = radiusY - capHeight;
            float offsetX = centerEvenDiameters ? -(radiusX % 1.0F) : 0.0F;
            float offsetY = centerEvenDiameters ? -(radiusY % 1.0F) : 0.0F;
            float offsetZ = centerEvenDiameters ? -(radiusZ % 1.0F) : 0.0F;
            RasterizationHelper.metaball(level, region, metaballRange, block, center, maxRadiusX, maxRadiusY, maxRadiusZ, (x, y, z) -> {
               vector3f.set(x, y, z);
               quaternionFinal.transform(vector3f);
               float rx = vector3f.x + offsetX;
               float ry = vector3f.y + offsetY;
               float rz = vector3f.z + offsetZ;
               boolean inside;
               if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ > 1.0F) {
                  inside = false;
               } else if (Math.abs(ry) <= verticalOffset) {
                  inside = true;
               } else {
                  float capY = Math.abs(ry) - verticalOffset + capRadius - capHeight;
                  inside = rx * rx * invCapRadiusSqX + capY * capY * invCapRadiusSqY + rz * rz * invCapRadiusSqZ <= 1.0F;
               }

               if (inside) {
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
                     boolean insideOther;
                     if (rx * rx * invRadiusSqX + rz * rz * invRadiusSqZ > 1.0F) {
                        insideOther = false;
                     } else if (Math.abs(ry) <= verticalOffset) {
                        insideOther = true;
                     } else {
                        float capY = Math.abs(ry) - verticalOffset + capRadius - capHeight;
                        insideOther = rx * rx * invCapRadiusSqX + capY * capY * invCapRadiusSqY + rz * rz * invCapRadiusSqZ <= 1.0F;
                     }

                     if (!insideOther) {
                        return true;
                     }
                  }
               }

               return false;
            });
         }
      } else {
         capsule(region, block, center, diameterX, height, diameterZ, hollow, centerEvenDiameters, capHeightFactor, quaternionf);
      }
   }
}
