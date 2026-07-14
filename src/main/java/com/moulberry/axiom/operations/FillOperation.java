package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FillOperation {
   public static int FILL_INSIDE = 1;
   public static int FILL_WALLS = 2;
   public static int FILL_CEILING = 4;
   public static int FILL_FLOOR = 8;
   public static int FILL_EXTERIOR = FILL_WALLS | FILL_CEILING | FILL_FLOOR;
   public static int FILL_ALL = FILL_INSIDE | FILL_EXTERIOR;

   public static void fill(BlockState blockState) {
      fill(blockState, FILL_ALL);
   }

   public static void fill(BlockState blockState, int fillFlags) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         fillAABB(aabb, blockState, fillFlags);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         fillSet(set, blockState, fillFlags);
      }
   }

   private static void fillAABB(SelectionBuffer.AABB aabb, BlockState blockState, int fillFlags) {
      if ((fillFlags & FILL_ALL) != 0) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockBuffer setOperation = new BlockBuffer();
            BlockBuffer previousBlocksForUndo = new BlockBuffer();
            int changeCount = 0;
            int minX = aabb.min().getX();
            int minY = aabb.min().getY();
            int minZ = aabb.min().getZ();
            int maxX = aabb.max().getX();
            int maxY = aabb.max().getY();
            int maxZ = aabb.max().getZ();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            if ((fillFlags & FILL_INSIDE) != 0) {
               if ((fillFlags & FILL_WALLS) == 0) {
                  minX++;
                  minZ++;
                  maxX--;
                  maxZ--;
               }

               if ((fillFlags & FILL_CEILING) == 0) {
                  maxY--;
               }

               if ((fillFlags & FILL_FLOOR) == 0) {
                  minY++;
               }

               for (int x = minX; x <= maxX; x++) {
                  for (int y = minY; y <= maxY; y++) {
                     for (int z = minZ; z <= maxZ; z++) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                        if (block.getBlock() != Blocks.VOID_AIR && block != blockState) {
                           setOperation.set(x, y, z, blockState);
                           previousBlocksForUndo.set(x, y, z, block);
                           changeCount++;
                        }
                     }
                  }
               }
            } else {
               if ((fillFlags & FILL_WALLS) != 0) {
                  for (int y = minY; y <= maxY; y++) {
                     for (int x = minX; x <= maxX; x++) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(x, y, minZ));
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (block != blockState) {
                              setOperation.set(x, y, minZ, blockState);
                              previousBlocksForUndo.set(x, y, minZ, block);
                              changeCount++;
                           }

                           block = world.getBlockState(mutableBlockPos.set(x, y, maxZ));
                           if (block.getBlock() != Blocks.VOID_AIR && block != blockState) {
                              setOperation.set(x, y, maxZ, blockState);
                              previousBlocksForUndo.set(x, y, maxZ, block);
                              changeCount++;
                           }
                        }
                     }

                     for (int zx = minZ + 1; zx <= maxZ - 1; zx++) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(minX, y, zx));
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (block != blockState) {
                              setOperation.set(minX, y, zx, blockState);
                              previousBlocksForUndo.set(minX, y, zx, block);
                              changeCount++;
                           }

                           block = world.getBlockState(mutableBlockPos.set(maxX, y, zx));
                           if (block.getBlock() != Blocks.VOID_AIR && block != blockState) {
                              setOperation.set(maxX, y, zx, blockState);
                              previousBlocksForUndo.set(maxX, y, zx, block);
                              changeCount++;
                           }
                        }
                     }
                  }

                  minX++;
                  minZ++;
                  maxX--;
                  maxZ--;
               }

               boolean ceiling = (fillFlags & FILL_CEILING) != 0;
               boolean floor = (fillFlags & FILL_FLOOR) != 0;
               if (floor && ceiling) {
                  for (int xx = minX; xx <= maxX; xx++) {
                     for (int zxx = minZ; zxx <= maxZ; zxx++) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(xx, minY, zxx));
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (block != blockState) {
                              setOperation.set(xx, minY, zxx, blockState);
                              previousBlocksForUndo.set(xx, minY, zxx, block);
                              changeCount++;
                           }

                           block = world.getBlockState(mutableBlockPos.set(xx, maxY, zxx));
                           if (block.getBlock() != Blocks.VOID_AIR && block != blockState) {
                              setOperation.set(xx, maxY, zxx, blockState);
                              previousBlocksForUndo.set(xx, maxY, zxx, block);
                              changeCount++;
                           }
                        }
                     }
                  }
               } else if (floor) {
                  for (int xx = minX; xx <= maxX; xx++) {
                     for (int zxxx = minZ; zxxx <= maxZ; zxxx++) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(xx, minY, zxxx));
                        if (block.getBlock() != Blocks.VOID_AIR && block != blockState) {
                           setOperation.set(xx, minY, zxxx, blockState);
                           previousBlocksForUndo.set(xx, minY, zxxx, block);
                           changeCount++;
                        }
                     }
                  }
               } else if (ceiling) {
                  for (int xx = minX; xx <= maxX; xx++) {
                     for (int zxxxx = minZ; zxxxx <= maxZ; zxxxx++) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(xx, maxY, zxxxx));
                        if (block.getBlock() != Blocks.VOID_AIR && block != blockState) {
                           setOperation.set(xx, maxY, zxxxx, blockState);
                           previousBlocksForUndo.set(xx, maxY, zxxxx, block);
                           changeCount++;
                        }
                     }
                  }
               }
            }

            if (changeCount != 0) {
               String countString = NumberFormat.getInstance().format((long)changeCount);
               String blockName = AxiomI18n.get(blockState.getBlock().getDescriptionId());
               String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
               RegionHelper.pushBlockBufferChange(setOperation, aabb.center(), historyDescription, null);
            }
         }
      }
   }

   private static void fillSet(SelectionBuffer.Set set, BlockState blockState, int fillFlags) {
      if ((fillFlags & FILL_ALL) != 0) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockBuffer setOperation = new BlockBuffer();
            BlockBuffer previousBlocksForUndo = new BlockBuffer();
            IntWrapper changeCount = new IntWrapper();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            if ((fillFlags & FILL_ALL) == FILL_ALL) {
               set.selectionRegion.forEach((x, y, z) -> {
                  BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (block.getBlock() != Blocks.VOID_AIR) {
                     if (block != blockState) {
                        setOperation.set(x, y, z, blockState);
                        previousBlocksForUndo.set(x, y, z, block);
                        changeCount.value++;
                     }
                  }
               });
            } else if ((fillFlags & FILL_INSIDE) != 0) {
               boolean ceiling = (fillFlags & FILL_CEILING) != 0;
               boolean floor = (fillFlags & FILL_FLOOR) != 0;
               boolean walls = (fillFlags & FILL_WALLS) != 0;
               if (!ceiling && !floor && !walls) {
                  set.selectionRegion.forEach((x, y, z) -> {
                     if (set.selectionRegion.contains(x, y + 1, z)) {
                        if (set.selectionRegion.contains(x, y - 1, z)) {
                           if (set.selectionRegion.contains(x + 1, y, z)) {
                              if (set.selectionRegion.contains(x - 1, y, z)) {
                                 if (set.selectionRegion.contains(x, y, z + 1)) {
                                    if (set.selectionRegion.contains(x, y, z - 1)) {
                                       BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                                       if (block.getBlock() != Blocks.VOID_AIR) {
                                          if (block != blockState) {
                                             setOperation.set(x, y, z, blockState);
                                             previousBlocksForUndo.set(x, y, z, block);
                                             changeCount.value++;
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  });
               } else {
                  set.selectionRegion.forEach((x, y, z) -> {
                     if (ceiling || set.selectionRegion.contains(x, y + 1, z)) {
                        if (floor || set.selectionRegion.contains(x, y - 1, z)) {
                           if (!walls) {
                              if (!set.selectionRegion.contains(x + 1, y, z)) {
                                 return;
                              }

                              if (!set.selectionRegion.contains(x - 1, y, z)) {
                                 return;
                              }

                              if (!set.selectionRegion.contains(x, y, z + 1)) {
                                 return;
                              }

                              if (!set.selectionRegion.contains(x, y, z - 1)) {
                                 return;
                              }
                           }

                           BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                           if (block.getBlock() != Blocks.VOID_AIR) {
                              if (block != blockState) {
                                 setOperation.set(x, y, z, blockState);
                                 previousBlocksForUndo.set(x, y, z, block);
                                 changeCount.value++;
                              }
                           }
                        }
                     }
                  });
               }
            } else {
               boolean ceiling = (fillFlags & FILL_CEILING) != 0;
               boolean floor = (fillFlags & FILL_FLOOR) != 0;
               boolean walls = (fillFlags & FILL_WALLS) != 0;
               if (ceiling && !floor && !walls) {
                  set.selectionRegion.forEach((x, y, z) -> {
                     if (!set.selectionRegion.contains(x, y + 1, z)) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (block != blockState) {
                              setOperation.set(x, y, z, blockState);
                              previousBlocksForUndo.set(x, y, z, block);
                              changeCount.value++;
                           }
                        }
                     }
                  });
               } else if (!ceiling && floor && !walls) {
                  set.selectionRegion.forEach((x, y, z) -> {
                     if (!set.selectionRegion.contains(x, y - 1, z)) {
                        BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (block != blockState) {
                              setOperation.set(x, y, z, blockState);
                              previousBlocksForUndo.set(x, y, z, block);
                              changeCount.value++;
                           }
                        }
                     }
                  });
               } else if (!ceiling && !floor && walls) {
                  set.selectionRegion
                     .forEach(
                        (x, y, z) -> {
                           if (!set.selectionRegion.contains(x + 1, y, z)
                              || !set.selectionRegion.contains(x - 1, y, z)
                              || !set.selectionRegion.contains(x, y, z + 1)
                              || !set.selectionRegion.contains(x, y, z - 1)) {
                              BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                              if (block.getBlock() != Blocks.VOID_AIR) {
                                 if (block != blockState) {
                                    setOperation.set(x, y, z, blockState);
                                    previousBlocksForUndo.set(x, y, z, block);
                                    changeCount.value++;
                                 }
                              }
                           }
                        }
                     );
               } else {
                  set.selectionRegion
                     .forEach(
                        (x, y, z) -> {
                           if (ceiling && !set.selectionRegion.contains(x, y + 1, z)
                              || floor && !set.selectionRegion.contains(x, y - 1, z)
                              || walls
                                 && (
                                    !set.selectionRegion.contains(x + 1, y, z)
                                       || !set.selectionRegion.contains(x - 1, y, z)
                                       || !set.selectionRegion.contains(x, y, z + 1)
                                       || !set.selectionRegion.contains(x, y, z - 1)
                                 )) {
                              BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                              if (block.getBlock() == Blocks.VOID_AIR) {
                                 return;
                              }

                              if (block != blockState) {
                                 setOperation.set(x, y, z, blockState);
                                 previousBlocksForUndo.set(x, y, z, block);
                                 changeCount.value++;
                              }
                           }
                        }
                     );
               }
            }

            if (changeCount.value != 0) {
               String countString = NumberFormat.getInstance().format((long)changeCount.value);
               String blockName = AxiomI18n.get(blockState.getBlock().getDescriptionId());
               String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
               RegionHelper.pushBlockBufferChange(setOperation, set.center(), historyDescription, null);
            }
         }
      }
   }
}
