package com.moulberry.axiom.block_maps;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.Tool;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily.Variant;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;

public class HDVoxelMap {
   private static final Map<Block, HDVoxelMap.HDVoxelBaseBlocks> VOXEL_BASE_BLOCKS_MAP = new HashMap<>();
   private static final Map<Block, HDVoxelMap.HDVoxelBaseBlocks> VOXEL_BASE_BLOCKS_FULL_MAP = new HashMap<>();
   public static int[] validVoxel222 = new int[]{
      0, 255, 240, 15, 248, 244, 242, 241, 252, 250, 245, 243, 247, 251, 253, 254, 143, 79, 47, 31, 207, 175, 95, 63, 127, 191, 223, 239
   };

   private static void process(Block full, Block stair, Block slab) {
      HDVoxelMap.HDVoxelBaseBlocks hdVoxelBaseBlocks = new HDVoxelMap.HDVoxelBaseBlocks(full, stair, slab);
      VOXEL_BASE_BLOCKS_FULL_MAP.put(full, hdVoxelBaseBlocks);
      VOXEL_BASE_BLOCKS_MAP.put(full, hdVoxelBaseBlocks);
      VOXEL_BASE_BLOCKS_MAP.put(stair, hdVoxelBaseBlocks);
      VOXEL_BASE_BLOCKS_MAP.put(slab, hdVoxelBaseBlocks);
   }

   public static HDVoxelMap.HDVoxelBaseBlocks getAssociatedBlocks(Block block) {
      return VOXEL_BASE_BLOCKS_MAP.get(block);
   }

   public static HDVoxelMap.HDVoxelBaseBlocks getAssociatedBlocksForFull(Block block) {
      return VOXEL_BASE_BLOCKS_FULL_MAP.get(block);
   }

   public static void downsizePathTool(ChunkedBlockRegion doubleSizeRegion, ChunkedBlockRegion into) {
      PositionSet tempSet = new PositionSet();
      doubleSizeRegion.forEachChunk((cx, cy, cz, blocksx) -> {
         BlockState[] minusX = doubleSizeRegion.getChunk(cx - 1, cy, cz);
         BlockState[] plusX = doubleSizeRegion.getChunk(cx + 1, cy, cz);
         BlockState[] minusY = doubleSizeRegion.getChunk(cx, cy - 1, cz);
         BlockState[] plusY = doubleSizeRegion.getChunk(cx, cy + 1, cz);
         BlockState[] minusZ = doubleSizeRegion.getChunk(cx, cy, cz - 1);
         BlockState[] plusZ = doubleSizeRegion.getChunk(cx, cy, cz + 1);

         for (int x = 0; x < 16; x += 2) {
            for (int y = 0; y < 16; y += 2) {
               for (int z = 0; z < 16; z += 2) {
                  int bits = 0;
                  if (blocksx[x + y * 16 + z * 16 * 16] != null) {
                     bits |= 128;
                  }

                  if (blocksx[x + y * 16 + (z + 1) * 16 * 16] != null) {
                     bits |= 64;
                  }

                  if (blocksx[x + 1 + y * 16 + z * 16 * 16] != null) {
                     bits |= 32;
                  }

                  if (blocksx[x + 1 + y * 16 + (z + 1) * 16 * 16] != null) {
                     bits |= 16;
                  }

                  if (blocksx[x + (y + 1) * 16 + z * 16 * 16] != null) {
                     bits |= 8;
                  }

                  if (blocksx[x + (y + 1) * 16 + (z + 1) * 16 * 16] != null) {
                     bits |= 4;
                  }

                  if (blocksx[x + 1 + (y + 1) * 16 + z * 16 * 16] != null) {
                     bits |= 2;
                  }

                  if (blocksx[x + 1 + (y + 1) * 16 + (z + 1) * 16 * 16] != null) {
                     bits |= 1;
                  }

                  if (bits != 0) {
                     int minError = Integer.MAX_VALUE;
                     int closestValid = 255;

                     for (int voxel : validVoxel222) {
                        int different = (bits ^ voxel) & 0xFF;
                        int error = Integer.bitCount(different) * 3;
                        if ((different & 128) != 0 && (voxel & 128) != 0) {
                           error += penalizeJagged(x, y, z, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 64) != 0 && (voxel & 64) != 0) {
                           error += penalizeJagged(x, y, z + 1, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 32) != 0 && (voxel & 32) != 0) {
                           error += penalizeJagged(x + 1, y, z, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 16) != 0 && (voxel & 16) != 0) {
                           error += penalizeJagged(x + 1, y, z + 1, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 8) != 0 && (voxel & 8) != 0) {
                           error += penalizeJagged(x, y + 1, z, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 4) != 0 && (voxel & 4) != 0) {
                           error += penalizeJagged(x, y + 1, z + 1, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 2) != 0 && (voxel & 2) != 0) {
                           error += penalizeJagged(x + 1, y + 1, z, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        if ((different & 1) != 0 && (voxel & 1) != 0) {
                           error += penalizeJagged(x + 1, y + 1, z + 1, blocksx, minusX, plusX, minusY, plusY, minusZ, plusZ) * 2;
                        }

                        error *= 16;
                        error += 8 - Integer.bitCount(voxel & 0xFF);
                        if (error < minError) {
                           minError = error;
                           closestValid = voxel;
                        }
                     }

                     if ((closestValid & 128) != 0) {
                        tempSet.add(cx * 16 + x, cy * 16 + y, cz * 16 + z);
                     }

                     if ((closestValid & 64) != 0) {
                        tempSet.add(cx * 16 + x, cy * 16 + y, cz * 16 + z + 1);
                     }

                     if ((closestValid & 32) != 0) {
                        tempSet.add(cx * 16 + x + 1, cy * 16 + y, cz * 16 + z);
                     }

                     if ((closestValid & 16) != 0) {
                        tempSet.add(cx * 16 + x + 1, cy * 16 + y, cz * 16 + z + 1);
                     }

                     if ((closestValid & 8) != 0) {
                        tempSet.add(cx * 16 + x, cy * 16 + y + 1, cz * 16 + z);
                     }

                     if ((closestValid & 4) != 0) {
                        tempSet.add(cx * 16 + x, cy * 16 + y + 1, cz * 16 + z + 1);
                     }

                     if ((closestValid & 2) != 0) {
                        tempSet.add(cx * 16 + x + 1, cy * 16 + y + 1, cz * 16 + z);
                     }

                     if ((closestValid & 1) != 0) {
                        tempSet.add(cx * 16 + x + 1, cy * 16 + y + 1, cz * 16 + z + 1);
                     }
                  }
               }
            }
         }
      });
      PositionSet tempSet2 = new PositionSet();
      tempSet.forEach((x, y, z) -> {
         tempSet2.add(x, y, z);
         if (tempSet.contains(x + 2, y, z)) {
            tempSet2.add(x + 1, y, z);
         }

         if (tempSet.contains(x, y + 2, z)) {
            tempSet2.add(x, y + 1, z);
         }

         if (tempSet.contains(x, y, z + 2)) {
            tempSet2.add(x, y, z + 1);
         }
      });
      BlockState activeBlock = Tool.getActiveBlock();
      HDVoxelMap.HDVoxelBaseBlocks blocks = getAssociatedBlocks(activeBlock.getBlock());
      BlockState full;
      BlockState stair;
      BlockState slab;
      if (blocks == null) {
         full = Blocks.STONE.defaultBlockState();
         stair = Blocks.STONE_STAIRS.defaultBlockState();
         slab = Blocks.STONE_SLAB.defaultBlockState();
      } else {
         full = blocks.full().defaultBlockState();
         stair = blocks.stair().defaultBlockState();
         slab = blocks.slab().defaultBlockState();
      }

      tempSet2.forEachChunk((cx, cy, cz, chunk) -> {
         for (int x = 0; x < 16; x += 2) {
            for (int y = 0; y < 16; y += 2) {
               for (int z = 0; z < 16; z += 2) {
                  int bits = 0;
                  if ((chunk[y + z * 16] & 1 << x) != 0) {
                     bits |= 128;
                  }

                  if ((chunk[y + (z + 1) * 16] & 1 << x) != 0) {
                     bits |= 64;
                  }

                  if ((chunk[y + z * 16] & 1 << x + 1) != 0) {
                     bits |= 32;
                  }

                  if ((chunk[y + (z + 1) * 16] & 1 << x + 1) != 0) {
                     bits |= 16;
                  }

                  if ((chunk[y + 1 + z * 16] & 1 << x) != 0) {
                     bits |= 8;
                  }

                  if ((chunk[y + 1 + (z + 1) * 16] & 1 << x) != 0) {
                     bits |= 4;
                  }

                  if ((chunk[y + 1 + z * 16] & 1 << x + 1) != 0) {
                     bits |= 2;
                  }

                  if ((chunk[y + 1 + (z + 1) * 16] & 1 << x + 1) != 0) {
                     bits |= 1;
                  }

                  if (bits != 0) {
                     BlockState validBlock = createFromVoxel222(bits, full, stair, slab);
                     into.addBlockWithoutDirty(cx * 8 + x / 2, cy * 8 + y / 2, cz * 8 + z / 2, validBlock);
                  }
               }
            }
         }
      });
   }

   private static int penalizeJagged(
      int x,
      int y,
      int z,
      BlockState[] blocks,
      BlockState[] minusX,
      BlockState[] plusX,
      BlockState[] minusY,
      BlockState[] plusY,
      BlockState[] minusZ,
      BlockState[] plusZ
   ) {
      int jagged = 0;
      boolean plusEmpty = x == 15 ? plusX == null || plusX[0 + y * 16 + z * 16 * 16] == null : blocks[x + 1 + y * 16 + z * 16 * 16] == null;
      boolean minusEmpty = x == 0 ? minusX == null || minusX[15 + y * 16 + z * 16 * 16] == null : blocks[x - 1 + y * 16 + z * 16 * 16] == null;
      if (plusEmpty && minusEmpty) {
         jagged++;
      }

      plusEmpty = y == 15 ? plusY == null || plusY[x + 0 + z * 16 * 16] == null : blocks[x + (y + 1) * 16 + z * 16 * 16] == null;
      minusEmpty = y == 0 ? minusY == null || minusY[x + 240 + z * 16 * 16] == null : blocks[x + (y - 1) * 16 + z * 16 * 16] == null;
      if (plusEmpty && minusEmpty) {
         jagged++;
      }

      plusEmpty = z == 15 ? plusZ == null || plusZ[x + y * 16 + 0] == null : blocks[x + y * 16 + (z + 1) * 16 * 16] == null;
      minusEmpty = z == 0 ? minusZ == null || minusZ[x + y * 16 + 3840] == null : blocks[x + y * 16 + (z - 1) * 16 * 16] == null;
      if (plusEmpty && minusEmpty) {
         jagged++;
      }

      return jagged;
   }

   public static int createVoxel222ForStair(BlockState blockState) {
      int opposite = switch ((StairsShape)blockState.getValue(BlockStateProperties.STAIRS_SHAPE)) {
         case STRAIGHT -> 10;
         case OUTER_LEFT -> 8;
         case OUTER_RIGHT -> 2;
         case INNER_LEFT -> 14;
         case INNER_RIGHT -> 11;
         default -> throw new IncompatibleClassChangeError();
      };

      int rotate = switch ((Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
         case NORTH -> 0;
         case SOUTH -> 2;
         case WEST -> 1;
         case EAST -> 3;
         default -> throw new IllegalStateException();
      };
      opposite = opposite & 3 | (opposite & 4) << 1 | (opposite & 8) >> 1;
      opposite <<= rotate;
      opposite = (opposite | (opposite & 240) >> 4) & 15;
      opposite = opposite & 3 | (opposite & 4) << 1 | (opposite & 8) >> 1;
      int voxel;
      if (blockState.getValue(BlockStateProperties.HALF) == Half.BOTTOM) {
         voxel = 240 | opposite;
      } else {
         voxel = 15 | opposite << 4;
      }

      return voxel;
   }

   public static BlockState createFromVoxel222(int voxel, BlockState full, BlockState stair, BlockState slab) {
      if (voxel == 0) {
         return Blocks.AIR.defaultBlockState();
      } else if (voxel == 255) {
         return full;
      } else {
         if ((voxel & 240) == 240) {
            int upperCount = Integer.bitCount(voxel & 15);
            if (upperCount == 0) {
               return (BlockState)slab.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            }

            if (upperCount == 1) {
               stair = (BlockState)stair.setValue(BlockStateProperties.HALF, Half.BOTTOM);
               stair = (BlockState)stair.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_LEFT);
               Direction stairFacing;
               if ((voxel & 1) != 0) {
                  stairFacing = Direction.SOUTH;
               } else if ((voxel & 2) != 0) {
                  stairFacing = Direction.EAST;
               } else if ((voxel & 4) != 0) {
                  stairFacing = Direction.WEST;
               } else {
                  stairFacing = Direction.NORTH;
               }

               return (BlockState)stair.setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacing);
            }

            if (upperCount == 2) {
               stair = (BlockState)stair.setValue(BlockStateProperties.HALF, Half.BOTTOM);
               stair = (BlockState)stair.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
               Direction stairFacing = null;
               if ((voxel & 1) != 0) {
                  if ((voxel & 2) != 0) {
                     stairFacing = Direction.EAST;
                  } else if ((voxel & 4) != 0) {
                     stairFacing = Direction.SOUTH;
                  }
               } else if ((voxel & 8) != 0) {
                  if ((voxel & 2) != 0) {
                     stairFacing = Direction.NORTH;
                  } else if ((voxel & 4) != 0) {
                     stairFacing = Direction.WEST;
                  }
               }

               if (stairFacing == null) {
                  return null;
               }

               return (BlockState)stair.setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacing);
            }

            if (upperCount == 3) {
               stair = (BlockState)stair.setValue(BlockStateProperties.HALF, Half.BOTTOM);
               stair = (BlockState)stair.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.INNER_LEFT);
               Direction stairFacingx;
               if ((voxel & 1) == 0) {
                  stairFacingx = Direction.NORTH;
               } else if ((voxel & 2) == 0) {
                  stairFacingx = Direction.WEST;
               } else if ((voxel & 4) == 0) {
                  stairFacingx = Direction.EAST;
               } else {
                  stairFacingx = Direction.SOUTH;
               }

               return (BlockState)stair.setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacingx);
            }
         } else if ((voxel & 15) == 15) {
            int lowerCount = Integer.bitCount(voxel & 240);
            if (lowerCount == 0) {
               return (BlockState)slab.setValue(SlabBlock.TYPE, SlabType.TOP);
            }

            if (lowerCount == 1) {
               stair = (BlockState)stair.setValue(BlockStateProperties.HALF, Half.TOP);
               stair = (BlockState)stair.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_LEFT);
               Direction stairFacingx;
               if ((voxel & 16) != 0) {
                  stairFacingx = Direction.SOUTH;
               } else if ((voxel & 32) != 0) {
                  stairFacingx = Direction.EAST;
               } else if ((voxel & 64) != 0) {
                  stairFacingx = Direction.WEST;
               } else {
                  stairFacingx = Direction.NORTH;
               }

               return (BlockState)stair.setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacingx);
            }

            if (lowerCount == 2) {
               stair = (BlockState)stair.setValue(BlockStateProperties.HALF, Half.TOP);
               stair = (BlockState)stair.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
               Direction stairFacingx = null;
               if ((voxel & 16) != 0) {
                  if ((voxel & 32) != 0) {
                     stairFacingx = Direction.EAST;
                  } else if ((voxel & 64) != 0) {
                     stairFacingx = Direction.SOUTH;
                  }
               } else if ((voxel & 128) != 0) {
                  if ((voxel & 32) != 0) {
                     stairFacingx = Direction.NORTH;
                  } else if ((voxel & 64) != 0) {
                     stairFacingx = Direction.WEST;
                  }
               }

               if (stairFacingx == null) {
                  return null;
               }

               return (BlockState)stair.setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacingx);
            }

            if (lowerCount == 3) {
               stair = (BlockState)stair.setValue(BlockStateProperties.HALF, Half.TOP);
               stair = (BlockState)stair.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.INNER_LEFT);
               Direction stairFacingxx;
               if ((voxel & 16) == 0) {
                  stairFacingxx = Direction.NORTH;
               } else if ((voxel & 32) == 0) {
                  stairFacingxx = Direction.WEST;
               } else if ((voxel & 64) == 0) {
                  stairFacingxx = Direction.EAST;
               } else {
                  stairFacingxx = Direction.SOUTH;
               }

               return (BlockState)stair.setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacingxx);
            }
         }

         return null;
      }
   }

   static {
      BlockFamilies.getAllFamilies().forEach(blockFamily -> {
         Block base = blockFamily.getBaseBlock();
         Block stairs = blockFamily.get(Variant.STAIRS);
         Block slab = blockFamily.get(Variant.SLAB);
         if (stairs != null && slab != null) {
            process(base, stairs, slab);
         }
      });
   }

   public record HDVoxelBaseBlocks(Block full, Block stair, Block slab) {
   }
}
