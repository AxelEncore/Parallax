package com.moulberry.axiom.operations.smooth;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.Position2dToIntMap;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.RegionHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.text.NumberFormat;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SmoothSnow {
   public static void smoothSnow() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         smoothSnow(aabb);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         smoothSnow(set);
      }
   }

   private static void smoothSnow(SelectionBuffer.AABB aabb) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         int minX = aabb.min().getX();
         int minY = aabb.min().getY();
         int minZ = aabb.min().getZ();
         int maxX = aabb.max().getX();
         int maxY = aabb.max().getY();
         int maxZ = aabb.max().getZ();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         Position2dToIntMap heightmap = new Position2dToIntMap(Integer.MIN_VALUE);
         ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                  if (blockState.getBlock() == Blocks.SNOW) {
                     blockRegion.addBlock(x, y, z, Blocks.AIR.defaultBlockState());
                     BlockState below = level.getBlockState(mutableBlockPos.set(x, y - 1, z));
                     if (below.hasProperty(BlockStateProperties.SNOWY)) {
                        blockRegion.addBlock(x, y - 1, z, (BlockState)below.setValue(BlockStateProperties.SNOWY, false));
                     }
                  } else if (blockState.blocksMotion()) {
                     if (y == maxY) {
                        heightmap.put(x, z, Integer.MAX_VALUE);
                     } else {
                        heightmap.max(x, z, y);
                     }
                  }
               }
            }
         }

         doSmoothSnow(heightmap, minX, maxX, minZ, maxZ, blockRegion);
      }
   }

   private static void smoothSnow(SelectionBuffer.Set set) {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         Position2dToIntMap heightmap = new Position2dToIntMap(Integer.MIN_VALUE);
         ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
         set.forEach((xx, y, zx) -> {
            BlockState blockState = level.getBlockState(mutableBlockPos.set(xx, y, zx));
            if (blockState.getBlock() == Blocks.SNOW) {
               blockRegion.addBlock(xx, y, zx, Blocks.AIR.defaultBlockState());
               BlockState below = level.getBlockState(mutableBlockPos.set(xx, y - 1, zx));
               if (below.hasProperty(BlockStateProperties.SNOWY)) {
                  blockRegion.addBlock(xx, y - 1, zx, (BlockState)below.setValue(BlockStateProperties.SNOWY, false));
               }

               if (below.blocksMotion() && below.getBlock() != Blocks.SNOW) {
                  heightmap.max(xx, zx, y - 1);
               }
            } else if (blockState.blocksMotion()) {
               heightmap.max(xx, zx, y);
            } else {
               BlockState belowx = level.getBlockState(mutableBlockPos.set(xx, y - 1, zx));
               if (belowx.blocksMotion() && belowx.getBlock() != Blocks.SNOW) {
                  heightmap.max(xx, zx, y - 1);
               }
            }
         });
         int minX = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;
         ObjectIterator var9 = heightmap.unsafeGetMap().long2ObjectEntrySet().iterator();

         while (var9.hasNext()) {
            Entry<int[]> entry = (Entry<int[]>)var9.next();
            int cx = ChunkPos.getX(entry.getLongKey()) * 16;
            int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
            int index = 0;

            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  int v = ((int[])entry.getValue())[index];
                  if (v != Integer.MIN_VALUE) {
                     if (!set.contains(cx + x, v + 1, cz + z)) {
                        ((int[])entry.getValue())[index] = Integer.MAX_VALUE;
                     } else {
                        if (cx + x < minX) {
                           minX = cx + x;
                        }

                        if (cz + z < minZ) {
                           minZ = cz + z;
                        }

                        if (cx + x > maxX) {
                           maxX = cx + x;
                        }

                        if (cz + z > maxZ) {
                           maxZ = cz + z;
                        }
                     }
                  }

                  index++;
               }
            }
         }

         if (maxX >= minX && maxZ >= minZ) {
            doSmoothSnow(heightmap, minX, maxX, minZ, maxZ, blockRegion);
         }
      }
   }

   private static void doSmoothSnow(Position2dToIntMap heightmap, int minX, int maxX, int minZ, int maxZ, ChunkedBlockRegion blockRegion) {
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      Level level = Minecraft.getInstance().level;
      int sizeX = maxX - minX + 1;
      int sizeZ = maxZ - minZ + 1;
      int[] distancesUpper = new int[(sizeX + 2) * (sizeZ + 2)];
      Arrays.fill(distancesUpper, Integer.MAX_VALUE);
      int[] distancesLower = new int[(sizeX + 2) * (sizeZ + 2)];
      Arrays.fill(distancesLower, Integer.MAX_VALUE);
      int xIndexOffset = sizeZ + 2;
      int zIndexOffset = 1;

      for (int x = 0; x < sizeX; x++) {
         for (int z = 0; z < sizeZ; z++) {
            int index = (x + 1) * xIndexOffset + (z + 1) * zIndexOffset;
            int height = heightmap.get(minX + x, minZ + z);
            int heightNeighborPX = heightmap.get(minX + x + 1, minZ + z);
            int heightNeighborPZ = heightmap.get(minX + x, minZ + z + 1);
            int heightNeighborNX = heightmap.get(minX + x - 1, minZ + z);
            int heightNeighborNZ = heightmap.get(minX + x, minZ + z - 1);
            int minNeighborHeight = Math.min(heightNeighborPX, Math.min(heightNeighborPZ, Math.min(heightNeighborNX, heightNeighborNZ)));
            boolean hasUpperNeighbor = false;
            if (heightNeighborPX > height && level.getBlockState(mutableBlockPos.set(minX + x + 1, height + 1, minZ + z)).blocksMotion()) {
               hasUpperNeighbor = true;
            } else if (heightNeighborPZ > height && level.getBlockState(mutableBlockPos.set(minX + x, height + 1, minZ + z + 1)).blocksMotion()) {
               hasUpperNeighbor = true;
            } else if (heightNeighborNX > height && level.getBlockState(mutableBlockPos.set(minX + x - 1, height + 1, minZ + z)).blocksMotion()) {
               hasUpperNeighbor = true;
            } else if (heightNeighborNZ > height && level.getBlockState(mutableBlockPos.set(minX + x, height + 1, minZ + z - 1)).blocksMotion()) {
               hasUpperNeighbor = true;
            }

            if (hasUpperNeighbor) {
               distancesUpper[index] = 1;
            } else {
               int least = Integer.MAX_VALUE;
               if (heightNeighborNX == height) {
                  int neighbor = distancesUpper[index - xIndexOffset];
                  if (neighbor != Integer.MAX_VALUE && neighbor + 1 < least) {
                     least = neighbor + 1;
                  }
               }

               if (heightNeighborNZ == height) {
                  int neighbor = distancesUpper[index - zIndexOffset];
                  if (neighbor != Integer.MAX_VALUE && neighbor + 1 < least) {
                     least = neighbor + 1;
                  }
               }

               distancesUpper[index] = least;
            }

            if (minNeighborHeight < height) {
               distancesLower[index] = 1;
            } else {
               int leastx = Integer.MAX_VALUE;
               if (heightNeighborNX == height) {
                  int neighbor = distancesLower[index - xIndexOffset];
                  if (neighbor != Integer.MAX_VALUE && neighbor + 1 < leastx) {
                     leastx = neighbor + 1;
                  }
               }

               if (heightNeighborNZ == height) {
                  int neighbor = distancesLower[index - zIndexOffset];
                  if (neighbor != Integer.MAX_VALUE && neighbor + 1 < leastx) {
                     leastx = neighbor + 1;
                  }
               }

               distancesLower[index] = leastx;
            }
         }
      }

      for (int x = sizeX - 1; x >= 0; x--) {
         for (int z = sizeZ - 1; z >= 0; z--) {
            int indexx = (x + 1) * xIndexOffset + (z + 1) * zIndexOffset;
            int heightx = heightmap.get(minX + x, minZ + z);
            int heightNeighborPXx = heightmap.get(minX + x + 1, minZ + z);
            int heightNeighborPZx = heightmap.get(minX + x, minZ + z + 1);
            int leastxx = distancesUpper[indexx];
            if (heightNeighborPXx == heightx) {
               int neighborX = distancesUpper[indexx + xIndexOffset];
               if (neighborX != Integer.MAX_VALUE && neighborX + 1 < leastxx) {
                  leastxx = neighborX + 1;
               }
            }

            if (heightNeighborPZx == heightx) {
               int neighborZ = distancesUpper[indexx + zIndexOffset];
               if (neighborZ != Integer.MAX_VALUE && neighborZ + 1 < leastxx) {
                  leastxx = neighborZ + 1;
               }
            }

            distancesUpper[indexx] = leastxx;
            leastxx = distancesLower[indexx];
            if (heightNeighborPXx == heightx) {
               int neighborX = distancesLower[indexx + xIndexOffset];
               if (neighborX != Integer.MAX_VALUE && neighborX + 1 < leastxx) {
                  leastxx = neighborX + 1;
               }
            }

            if (heightNeighborPZx == heightx) {
               int neighborZ = distancesLower[indexx + zIndexOffset];
               if (neighborZ != Integer.MAX_VALUE && neighborZ + 1 < leastxx) {
                  leastxx = neighborZ + 1;
               }
            }

            distancesLower[indexx] = leastxx;
         }
      }

      for (int x = 0; x < sizeX; x++) {
         for (int z = 0; z < sizeZ; z++) {
            int indexxx = (x + 1) * xIndexOffset + (z + 1) * zIndexOffset;
            int heightxx = heightmap.get(minX + x, minZ + z);
            int heightNeighborNXx = heightmap.get(minX + x - 1, minZ + z);
            int heightNeighborNZx = heightmap.get(minX + x, minZ + z - 1);
            int leastxxx = distancesUpper[indexxx];
            if (heightNeighborNXx == heightxx) {
               int neighborX = distancesUpper[indexxx - xIndexOffset];
               if (neighborX != Integer.MAX_VALUE && neighborX + 1 < leastxxx) {
                  leastxxx = neighborX + 1;
               }
            }

            if (heightNeighborNZx == heightxx) {
               int neighborZ = distancesUpper[indexxx - zIndexOffset];
               if (neighborZ != Integer.MAX_VALUE && neighborZ + 1 < leastxxx) {
                  leastxxx = neighborZ + 1;
               }
            }

            distancesUpper[indexxx] = leastxxx;
            leastxxx = distancesLower[indexxx];
            if (heightNeighborNXx == heightxx) {
               int neighborX = distancesLower[indexxx - xIndexOffset];
               if (neighborX != Integer.MAX_VALUE && neighborX + 1 < leastxxx) {
                  leastxxx = neighborX + 1;
               }
            }

            if (heightNeighborNZx == heightxx) {
               int neighborZ = distancesLower[indexxx - zIndexOffset];
               if (neighborZ != Integer.MAX_VALUE && neighborZ + 1 < leastxxx) {
                  leastxxx = neighborZ + 1;
               }
            }

            distancesLower[indexxx] = leastxxx;
         }
      }

      for (int x = sizeX - 1; x >= 0; x--) {
         for (int z = sizeZ - 1; z >= 0; z--) {
            int indexxxx = (x + 1) * xIndexOffset + (z + 1) * zIndexOffset;
            int heightxxx = heightmap.get(minX + x, minZ + z);
            int heightNeighborPXxx = heightmap.get(minX + x + 1, minZ + z);
            int heightNeighborPZxx = heightmap.get(minX + x, minZ + z + 1);
            int leastxxxx = distancesUpper[indexxxx];
            if (heightNeighborPXxx == heightxxx) {
               int neighborX = distancesUpper[indexxxx + xIndexOffset];
               if (neighborX != Integer.MAX_VALUE && neighborX + 1 < leastxxxx) {
                  leastxxxx = neighborX + 1;
               }
            }

            if (heightNeighborPZxx == heightxxx) {
               int neighborZ = distancesUpper[indexxxx + zIndexOffset];
               if (neighborZ != Integer.MAX_VALUE && neighborZ + 1 < leastxxxx) {
                  leastxxxx = neighborZ + 1;
               }
            }

            distancesUpper[indexxxx] = leastxxxx;
            leastxxxx = distancesLower[indexxxx];
            if (heightNeighborPXxx == heightxxx) {
               int neighborX = distancesLower[indexxxx + xIndexOffset];
               if (neighborX != Integer.MAX_VALUE && neighborX + 1 < leastxxxx) {
                  leastxxxx = neighborX + 1;
               }
            }

            if (heightNeighborPZxx == heightxxx) {
               int neighborZ = distancesLower[indexxxx + zIndexOffset];
               if (neighborZ != Integer.MAX_VALUE && neighborZ + 1 < leastxxxx) {
                  leastxxxx = neighborZ + 1;
               }
            }

            distancesLower[indexxxx] = leastxxxx;
         }
      }

      int[][] distanceLookup = new int[][]{
         {4}, {2, 6}, {1, 3, 6}, {1, 3, 5, 7}, {1, 3, 4, 5, 7}, {1, 2, 4, 5, 6, 7}, {1, 2, 3, 4, 5, 6, 7}, {1, 2, 3, 4, 4, 5, 6, 7}
      };
      Position2dToIntMap snowHeight = new Position2dToIntMap(Integer.MIN_VALUE);

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            int y = heightmap.get(x, z);
            if (y != Integer.MIN_VALUE && y != Integer.MAX_VALUE) {
               y++;
               int indexxxxx = (x - minX + 1) * xIndexOffset + (z - minZ + 1) * zIndexOffset;
               int upperDistance = distancesUpper[indexxxxx];
               int lowerDistance = distancesLower[indexxxxx];
               if (upperDistance == Integer.MAX_VALUE) {
                  if (lowerDistance != Integer.MAX_VALUE) {
                     snowHeight.put(x, z, Math.min(8, lowerDistance));
                  }
               } else if (lowerDistance == Integer.MAX_VALUE) {
                  snowHeight.put(x, z, 9 - Math.min(8, upperDistance));
               } else {
                  float layers1;
                  if (lowerDistance + upperDistance >= 8) {
                     layers1 = (float)(lowerDistance - 1) / (lowerDistance + upperDistance - 2) * 7.0F + 1.0F;
                  } else {
                     layers1 = distanceLookup[lowerDistance + upperDistance - 2][lowerDistance - 1];
                  }

                  int layers = Math.round(layers1);
                  if (layers < 1) {
                     layers = 1;
                  }

                  if (layers > 8) {
                     layers = 8;
                  }

                  snowHeight.put(x, z, layers);
               }
            }
         }
      }

      for (int x = minX; x <= maxX; x++) {
         int minHalfZ = Integer.MIN_VALUE;
         int minHalfZPosY = Integer.MIN_VALUE;
         int leftSnowHeight = 0;
         int leftY = Integer.MIN_VALUE;
         int currLeftSnowHeight = 0;
         int currLeftY = Integer.MIN_VALUE;

         for (int zx = minZ; zx <= maxZ; zx++) {
            int y = heightmap.get(x, zx);
            if (y != Integer.MIN_VALUE && y != Integer.MAX_VALUE) {
               if (minHalfZ != Integer.MIN_VALUE && minHalfZPosY != ++y) {
                  int adjustment = 0;
                  if (currLeftY < minHalfZPosY && y < minHalfZPosY) {
                     adjustment = 1;
                  } else if (currLeftY > minHalfZPosY && y > minHalfZPosY) {
                     adjustment = -1;
                  }

                  for (int z2 = minHalfZ; z2 < zx; z2++) {
                     int leftOffset = currLeftY == minHalfZPosY ? 1 : 0;
                     float amount = (float)(z2 - minHalfZ + leftOffset) / (zx - 1 - minHalfZ + leftOffset);
                     int layersx;
                     if (amount < 0.5) {
                        amount *= 2.0F;
                        if (currLeftY < minHalfZPosY) {
                           layersx = Math.round(4.0F * amount + 2.0F * (1.0F - amount));
                        } else if (currLeftY > minHalfZPosY) {
                           layersx = Math.round(4.0F * amount + 6.0F * (1.0F - amount));
                        } else {
                           layersx = Math.round(4.0F * amount + currLeftSnowHeight * (1.0F - amount));
                        }
                     } else if (amount > 0.5) {
                        amount = 2.0F - amount * 2.0F;
                        if (y < minHalfZPosY) {
                           layersx = Math.round(4.0F * amount + 2.0F * (1.0F - amount));
                        } else {
                           layersx = Math.round(4.0F * amount + 6.0F * (1.0F - amount));
                        }
                     } else {
                        layersx = 4;
                     }

                     snowHeight.put(x, z2, layersx + adjustment);
                  }

                  minHalfZ = Integer.MIN_VALUE;
               }

               int indexxxxx = (x - minX + 1) * xIndexOffset + (zx - minZ + 1) * zIndexOffset;
               int upperDistance = distancesUpper[indexxxxx];
               int lowerDistance = distancesLower[indexxxxx];
               int minusX = heightmap.get(x - 1, zx) + 1;
               int plusX = heightmap.get(x + 1, zx) + 1;
               if (upperDistance + lowerDistance <= 2 && minusX != y && plusX != y && minusX < y != plusX < y) {
                  if (minHalfZ == Integer.MIN_VALUE) {
                     minHalfZ = zx;
                     minHalfZPosY = y;
                     currLeftSnowHeight = leftSnowHeight;
                     currLeftY = leftY;
                  }
               } else if (minHalfZ != Integer.MIN_VALUE) {
                  int currRightSnowHeight = snowHeight.get(x, zx);

                  for (int z2 = minHalfZ; z2 < zx; z2++) {
                     int leftOffset = currLeftY == minHalfZPosY ? 1 : 0;
                     float amount = (float)(z2 - minHalfZ + leftOffset) / (zx - 1 - minHalfZ + leftOffset + 1);
                     int layersx;
                     if (amount < 0.5) {
                        amount *= 2.0F;
                        if (currLeftY < minHalfZPosY) {
                           layersx = Math.round(4.0F * amount + 2.0F * (1.0F - amount));
                        } else if (currLeftY > minHalfZPosY) {
                           layersx = Math.round(4.0F * amount + 6.0F * (1.0F - amount));
                        } else {
                           layersx = Math.round(4.0F * amount + currLeftSnowHeight * (1.0F - amount));
                        }
                     } else if (amount > 0.5) {
                        amount = 2.0F - amount * 2.0F;
                        layersx = Math.round(4.0F * amount + currRightSnowHeight * (1.0F - amount));
                     } else {
                        layersx = 4;
                     }

                     snowHeight.put(x, z2, layersx);
                  }

                  minHalfZ = Integer.MIN_VALUE;
               }

               leftSnowHeight = snowHeight.get(x, zx);
               leftY = y;
            }
         }
      }

      for (int zxx = minZ; zxx <= maxZ; zxx++) {
         int minHalfX = Integer.MIN_VALUE;
         int minHalfXPosY = Integer.MIN_VALUE;
         int leftSnowHeight = 0;
         int leftY = Integer.MIN_VALUE;
         int currLeftSnowHeight = 0;
         int currLeftY = Integer.MIN_VALUE;

         for (int x = minX; x <= maxX; x++) {
            int y = heightmap.get(x, zxx);
            if (y != Integer.MIN_VALUE && y != Integer.MAX_VALUE) {
               if (minHalfX != Integer.MIN_VALUE && minHalfXPosY != ++y) {
                  int adjustment = 0;
                  if (currLeftY < minHalfXPosY && y < minHalfXPosY) {
                     adjustment = 1;
                  } else if (currLeftY > minHalfXPosY && y > minHalfXPosY) {
                     adjustment = -1;
                  }

                  if (x - minHalfX >= 2) {
                     for (int x2 = minHalfX; x2 < x; x2++) {
                        int leftOffset = currLeftY == minHalfXPosY ? 1 : 0;
                        float amount = (float)(x2 - minHalfX + leftOffset) / (x - 1 - minHalfX + leftOffset);
                        int layersx;
                        if (amount < 0.5) {
                           amount *= 2.0F;
                           if (currLeftY < minHalfXPosY) {
                              layersx = Math.round(4.0F * amount + 2.0F * (1.0F - amount));
                           } else if (currLeftY > minHalfXPosY) {
                              layersx = Math.round(4.0F * amount + 6.0F * (1.0F - amount));
                           } else {
                              layersx = Math.round(4.0F * amount + currLeftSnowHeight * (1.0F - amount));
                           }
                        } else if (amount > 0.5) {
                           amount = 2.0F - amount * 2.0F;
                           if (y < minHalfXPosY) {
                              layersx = Math.round(4.0F * amount + 2.0F * (1.0F - amount));
                           } else {
                              layersx = Math.round(4.0F * amount + 6.0F * (1.0F - amount));
                           }
                        } else {
                           layersx = 4;
                        }

                        snowHeight.put(x2, zxx, layersx + adjustment);
                     }
                  }

                  minHalfX = Integer.MIN_VALUE;
               }

               int indexxxxx = (x - minX + 1) * xIndexOffset + (zxx - minZ + 1) * zIndexOffset;
               int upperDistance = distancesUpper[indexxxxx];
               int lowerDistance = distancesLower[indexxxxx];
               int minusZ = heightmap.get(x, zxx - 1) + 1;
               int plusZ = heightmap.get(x, zxx + 1) + 1;
               if (upperDistance + lowerDistance <= 2 && minusZ != y && plusZ != y && minusZ < y != plusZ < y) {
                  if (minHalfX == Integer.MIN_VALUE) {
                     minHalfX = x;
                     minHalfXPosY = y;
                     currLeftSnowHeight = leftSnowHeight;
                     currLeftY = leftY;
                  }
               } else if (minHalfX != Integer.MIN_VALUE) {
                  if (x - minHalfX >= 2) {
                     int currRightSnowHeight = snowHeight.get(x, zxx);

                     for (int x2 = minHalfX; x2 < x; x2++) {
                        int leftOffset = currLeftY == minHalfXPosY ? 1 : 0;
                        float amount = (float)(x2 - minHalfX + leftOffset) / (x - 1 - minHalfX + leftOffset + 1);
                        int layersx;
                        if (amount < 0.5) {
                           amount *= 2.0F;
                           if (currLeftY < minHalfXPosY) {
                              layersx = Math.round(4.0F * amount + 2.0F * (1.0F - amount));
                           } else if (currLeftY > minHalfXPosY) {
                              layersx = Math.round(4.0F * amount + 6.0F * (1.0F - amount));
                           } else {
                              layersx = Math.round(4.0F * amount + currLeftSnowHeight * (1.0F - amount));
                           }
                        } else if (amount > 0.5) {
                           amount = 2.0F - amount * 2.0F;
                           layersx = Math.round(4.0F * amount + currRightSnowHeight * (1.0F - amount));
                        } else {
                           layersx = 4;
                        }

                        snowHeight.put(x2, zxx, layersx);
                     }
                  }

                  minHalfX = Integer.MIN_VALUE;
               }

               leftSnowHeight = snowHeight.get(x, zxx);
               leftY = y;
            }
         }
      }

      for (int xx = minX; xx <= maxX; xx++) {
         for (int zxx = minZ; zxx <= maxZ; zxx++) {
            int y = heightmap.get(xx, zxx);
            if (y != Integer.MIN_VALUE && y != Integer.MAX_VALUE) {
               BlockState replacingBlock = level.getBlockState(mutableBlockPos.set(xx, ++y, zxx));
               if (replacingBlock.isAir() || replacingBlock.getBlock() == Blocks.SNOW) {
                  int snowHeightValue = snowHeight.get(xx, zxx);
                  if (snowHeightValue >= 1 && snowHeightValue <= 8) {
                     blockRegion.addBlock(xx, y, zxx, (BlockState)Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, snowHeightValue));
                     BlockState below = level.getBlockState(mutableBlockPos.set(xx, y - 1, zxx));
                     if (below.hasProperty(BlockStateProperties.SNOWY)) {
                        blockRegion.addBlock(xx, y - 1, zxx, (BlockState)below.setValue(BlockStateProperties.SNOWY, true));
                     }
                  }
               }
            }
         }
      }

      String blockCountString = NumberFormat.getNumberInstance().format((long)blockRegion.count());
      RegionHelper.pushBlockRegionChange(blockRegion, "Smooth Snow (" + blockCountString + " blocks)");
   }
}
