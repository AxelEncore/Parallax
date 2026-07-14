package com.moulberry.axiom.scaling;

import com.google.common.math.DoubleMath;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.BlockHelper;
import java.math.RoundingMode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class RotSprite {
   public static ChunkedBlockRegion rotate(ChunkedBlockRegion in, float rotationX, float rotationY, float rotationZ) {
      ChunkedBlockRegion scaled = Scale3x.scale3x(in, false);
      Matrix4f matrix4f = new Matrix4f();
      matrix4f.rotateYXZ(rotationY, rotationX, rotationZ);
      return rotateCached(scaled, matrix4f);
   }

   public static ChunkedBlockRegion rotateCached(ChunkedBlockRegion scaled, Matrix4f matrix4f) {
      return rotateCachedWithOutput(scaled, matrix4f, new ChunkedBlockRegion(), 0, 0, 0);
   }

   public static ChunkedBlockRegion rotateCachedWithOutput(
      ChunkedBlockRegion scaled, Matrix4f matrix4f, ChunkedBlockRegion out, int outputX, int outputY, int outputZ
   ) {
      if (scaled.isEmpty()) {
         return out;
      } else {
         int minX = scaled.min().getX() / 3;
         int minY = scaled.min().getY() / 3;
         int minZ = scaled.min().getZ() / 3;
         int maxX = scaled.max().getX() / 3;
         int maxY = scaled.max().getY() / 3;
         int maxZ = scaled.max().getZ() / 3;
         int newMinX = Integer.MAX_VALUE;
         int newMinY = Integer.MAX_VALUE;
         int newMinZ = Integer.MAX_VALUE;
         int newMaxX = Integer.MIN_VALUE;
         int newMaxY = Integer.MIN_VALUE;
         int newMaxZ = Integer.MIN_VALUE;
         Vector4f vector4f = new Vector4f();

         for (int x = minX; x <= maxX; x = maxX) {
            for (int y = minY; y <= maxY; y = maxY) {
               for (int z = minZ; z <= maxZ; z = maxZ) {
                  vector4f.set(x, y, z, 1.0F);
                  matrix4f.transform(vector4f);
                  newMinX = Math.min(newMinX, (int)Math.floor(vector4f.x));
                  newMinY = Math.min(newMinY, (int)Math.floor(vector4f.y));
                  newMinZ = Math.min(newMinZ, (int)Math.floor(vector4f.z));
                  newMaxX = Math.max(newMaxX, (int)Math.ceil(vector4f.x));
                  newMaxY = Math.max(newMaxY, (int)Math.ceil(vector4f.y));
                  newMaxZ = Math.max(newMaxZ, (int)Math.ceil(vector4f.z));
                  if (z == maxZ) {
                     break;
                  }
               }

               if (y == maxY) {
                  break;
               }
            }

            if (x == maxX) {
               break;
            }
         }

         Matrix4f inverted = matrix4f.invert(new Matrix4f());
         Quaternionf dest = new Quaternionf();
         matrix4f.getNormalizedRotation(dest);
         Vector3f euler = dest.getEulerAnglesYXZ(new Vector3f());
         Rotation rotationX = BlockHelper.rotationFromRadians(-euler.x);
         Rotation rotationY = BlockHelper.rotationFromRadians(-euler.y);
         Rotation rotationZ = BlockHelper.rotationFromRadians(-euler.z);
         PositionSet filled = new PositionSet();

         for (int x = newMinX; x <= newMaxX; x++) {
            for (int y = newMinY; y <= newMaxY; y++) {
               for (int zx = newMinZ; zx <= newMaxZ; zx++) {
                  vector4f.set(x, y, zx, 1.0F);
                  inverted.transform(vector4f);
                  int sampleX = DoubleMath.roundToInt(vector4f.x * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                  int sampleY = DoubleMath.roundToInt(vector4f.y * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                  int sampleZ = DoubleMath.roundToInt(vector4f.z * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                  BlockState blockState = scaled.getBlockStateOrAir(sampleX, sampleY, sampleZ);
                  if (!blockState.isAir()) {
                     blockState = BlockHelper.rotateX(blockState, rotationX);
                     blockState = BlockHelper.rotateY(blockState, rotationY);
                     blockState = BlockHelper.rotateZ(blockState, rotationZ);
                     out.addBlockWithoutDirty(x + outputX, y + outputY, zx + outputZ, blockState);
                     filled.add(x, y, zx);
                  }
               }
            }
         }

         BlockState air = Blocks.AIR.defaultBlockState();
         Position2ObjectMap<BlockState> additionalBlocks = new Position2ObjectMap<>(k -> new BlockState[4096]);

         for (int x = newMinX; x <= newMaxX; x++) {
            for (int y = newMinY; y <= newMaxY; y++) {
               label185:
               for (int zxx = newMinZ; zxx <= newMaxZ; zxx++) {
                  if (out.getBlockStateOrAir(x + outputX, y + outputY, zxx + outputZ).isAir()) {
                     vector4f.set(x, y, zxx, 1.0F);
                     inverted.transform(vector4f);
                     int sampleX = DoubleMath.roundToInt(vector4f.x * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                     int sampleY = DoubleMath.roundToInt(vector4f.y * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                     int sampleZ = DoubleMath.roundToInt(vector4f.z * 3.0F + 1.0F, RoundingMode.HALF_DOWN);

                     for (int xo = -1; xo <= 1; xo++) {
                        for (int yo = -1; yo <= 1; yo++) {
                           for (int zo = -1; zo <= 1; zo++) {
                              if ((xo != 0 || yo != 0 || zo != 0) && !filled.contains(x + xo, y + yo, zxx + zo)) {
                                 int sum = Math.abs(xo) + Math.abs(yo) + Math.abs(zo);
                                 if ((
                                       sum != 2
                                          || (
                                             xo == 0
                                                ? !filled.contains(x, y + yo, zxx) && !filled.contains(x, y, zxx + zo)
                                                : (
                                                   yo == 0
                                                      ? !filled.contains(x + xo, y, zxx) && !filled.contains(x, y, zxx + zo)
                                                      : !filled.contains(x + xo, y, zxx) && !filled.contains(x, y + yo, zxx)
                                                )
                                          )
                                    )
                                    && sum != 3) {
                                    vector4f.set(x + xo, y + yo, zxx + zo, 1.0F);
                                    inverted.transform(vector4f);
                                    int sampleX2 = DoubleMath.roundToInt(vector4f.x * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                                    int sampleY2 = DoubleMath.roundToInt(vector4f.y * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                                    int sampleZ2 = DoubleMath.roundToInt(vector4f.z * 3.0F + 1.0F, RoundingMode.HALF_DOWN);
                                    int offsetX = sampleX2 > sampleX ? 1 : -1;
                                    int offsetY = sampleY2 > sampleY ? 1 : -1;
                                    int offsetZ = sampleZ2 > sampleZ ? 1 : -1;
                                    BlockState block = sampleX == sampleX2 ? air : scaled.getBlockStateOrAir(sampleX + offsetX, sampleY, sampleZ);
                                    if (block.isAir()) {
                                       block = sampleY == sampleY2 ? air : scaled.getBlockStateOrAir(sampleX, sampleY + offsetY, sampleZ);
                                    }

                                    if (block.isAir()) {
                                       block = sampleZ == sampleZ2 ? air : scaled.getBlockStateOrAir(sampleX, sampleY, sampleZ + offsetZ);
                                    }

                                    if (!block.isAir()) {
                                       if (sampleX != sampleX2 && !scaled.getBlockStateOrAir(sampleX2 - offsetX, sampleY2, sampleZ2).isAir()) {
                                          additionalBlocks.put(x, y, zxx, block);
                                          continue label185;
                                       }

                                       if (sampleY != sampleY2 && !scaled.getBlockStateOrAir(sampleX2, sampleY2 - offsetY, sampleZ2).isAir()) {
                                          additionalBlocks.put(x, y, zxx, block);
                                          continue label185;
                                       }

                                       if (sampleZ != sampleZ2 && !scaled.getBlockStateOrAir(sampleX2, sampleY2, sampleZ2 - offsetZ).isAir()) {
                                          additionalBlocks.put(x, y, zxx, block);
                                          continue label185;
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

         additionalBlocks.forEachEntry((xx, yx, zxxx, blockx) -> {
            if (shouldUseAdditional(xx, yx, zxxx, additionalBlocks, filled)) {
               blockx = BlockHelper.rotateX(blockx, rotationX);
               blockx = BlockHelper.rotateY(blockx, rotationY);
               blockx = BlockHelper.rotateZ(blockx, rotationZ);
               out.addBlockWithoutDirty(xx + outputX, yx + outputY, zxxx + outputZ, blockx);
            }
         });
         out.dirtyAll();
         return out;
      }
   }

   private static boolean shouldUseAdditional(int x, int y, int z, Position2ObjectMap<BlockState> additionalBlocks, PositionSet filled) {
      int neighbors = 0;
      if (additionalBlocks.get(x + 1, y, z) != null) {
         neighbors++;
      }

      if (additionalBlocks.get(x - 1, y, z) != null) {
         neighbors++;
      }

      if (additionalBlocks.get(x, y + 1, z) != null) {
         neighbors++;
      }

      if (additionalBlocks.get(x, y - 1, z) != null) {
         neighbors++;
      }

      if (additionalBlocks.get(x, y, z + 1) != null) {
         neighbors++;
      }

      if (additionalBlocks.get(x, y, z - 1) != null) {
         neighbors++;
      }

      if (neighbors >= 2) {
         return true;
      } else {
         boolean plusX = false;
         boolean plusY = false;
         boolean plusZ = false;
         boolean zeroX = false;
         boolean zeroY = false;
         boolean zeroZ = false;
         boolean minusX = false;
         boolean minusY = false;
         boolean minusZ = false;

         for (int xo = -1; xo <= 1; xo++) {
            for (int yo = -1; yo <= 1; yo++) {
               for (int zo = -1; zo <= 1; zo++) {
                  if (xo != 0 || yo != 0 || zo != 0) {
                     if (filled.contains(x + xo, y + yo, z + zo)) {
                        if (xo == -1) {
                           minusX = true;
                        }

                        if (yo == -1) {
                           minusY = true;
                        }

                        if (zo == -1) {
                           minusZ = true;
                        }

                        if (xo == 0) {
                           zeroX = true;
                        }

                        if (yo == 0) {
                           zeroY = true;
                        }

                        if (zo == 0) {
                           zeroZ = true;
                        }

                        if (xo == 1) {
                           plusX = true;
                        }

                        if (yo == 1) {
                           plusY = true;
                        }

                        if (zo == 1) {
                           plusZ = true;
                        }
                     } else if (additionalBlocks.get(x + xo, y + yo, z + zo) != null) {
                        if (xo == -1) {
                           minusX = true;
                        }

                        if (yo == -1) {
                           minusY = true;
                        }

                        if (zo == -1) {
                           minusZ = true;
                        }

                        if (xo == 1) {
                           plusX = true;
                        }

                        if (yo == 1) {
                           plusY = true;
                        }

                        if (zo == 1) {
                           plusZ = true;
                        }
                     }
                  }
               }
            }
         }

         if (plusX && !zeroX && minusX) {
            return true;
         } else {
            return plusY && !zeroY && minusY ? true : plusZ && !zeroZ && minusZ;
         }
      }
   }
}
