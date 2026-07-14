package com.moulberry.axiom.utils;

import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.block_maps.HDVoxelMap;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.NotNull;

public class BlockHelper {
   public static Set<Block> ignoreRotation = Set.of();
   public static Map<BlockState, BlockState> customRotateY = new HashMap<>();
   public static Map<BlockState, BlockState> customFlipX = new HashMap<>();
   public static Map<BlockState, BlockState> customFlipY = new HashMap<>();
   public static Map<BlockState, BlockState> customFlipZ = new HashMap<>();

   public static Rotation rotationFromRadians(double radians) {
      return rotationFromDegrees(Math.toDegrees(radians));
   }

   public static Rotation rotationFromDegrees(double degrees) {
      long rotationCount = Math.round(degrees / 90.0);
      rotationCount %= 4L;
      if (rotationCount < 0L) {
         rotationCount += 4L;
      }
      return switch ((int)rotationCount) {
         case 1 -> Rotation.CLOCKWISE_90;
         case 2 -> Rotation.CLOCKWISE_180;
         case 3 -> Rotation.COUNTERCLOCKWISE_90;
         default -> Rotation.NONE;
      };
   }

   public static BlockState rotateX(BlockState blockState, Rotation rotation) {
      if (rotation == Rotation.NONE) {
         return blockState;
      } else if (ignoreRotation.contains(blockState.getBlock())) {
         return blockState;
      } else {
         if (blockState.getBlock() instanceof StairBlock) {
            int voxel = HDVoxelMap.createVoxel222ForStair(blockState);
            int a = voxel & 5;
            int b = voxel >> 1 & 5;
            int c = voxel >> 4 & 5;
            int d = voxel >> 5 & 5;
            int newVoxel = a << 1 | b << 5 | c | d << 4;

            try {
               BlockState rotated = HDVoxelMap.createFromVoxel222(newVoxel, null, blockState, null);
               if (rotated != null) {
                  blockState = rotated;
               }
            } catch (Exception var9) {
            }
         }

         return customRotateXZ(blockState, rotation, Axis.X, Axis.Z);
      }
   }

   public static BlockState rotateZ(BlockState blockState, Rotation rotation) {
      if (rotation == Rotation.NONE) {
         return blockState;
      } else if (ignoreRotation.contains(blockState.getBlock())) {
         return blockState;
      } else {
         if (blockState.getBlock() instanceof StairBlock) {
            int voxel = HDVoxelMap.createVoxel222ForStair(blockState);
            int a = voxel & 3;
            int b = voxel >> 2 & 3;
            int c = voxel >> 4 & 3;
            int d = voxel >> 6 & 3;
            int newVoxel = a << 4 | b | c << 6 | d << 2;

            try {
               BlockState rotated = HDVoxelMap.createFromVoxel222(newVoxel, null, blockState, null);
               if (rotated != null) {
                  blockState = rotated;
               }
            } catch (Exception var9) {
            }
         }

         return customRotateXZ(blockState, rotation, Axis.Z, Axis.X);
      }
   }

   private static BlockState customRotateXZ(BlockState blockState, Rotation rotation, Axis main, Axis cross) {
      int rotations = switch (rotation) {
         case NONE -> 0;
         case CLOCKWISE_90 -> 1;
         case CLOCKWISE_180 -> 2;
         case COUNTERCLOCKWISE_90 -> 3;
         default -> throw new IncompatibleClassChangeError();
      };
      if (blockState.getBlock() instanceof TrapDoorBlock) {
         boolean open = (Boolean)blockState.getValue(BlockStateProperties.OPEN);
         if (rotations == 1 || rotations == 3) {
            Direction first = switch (cross) {
               case X -> Direction.WEST;
               case Y -> throw new UnsupportedOperationException();
               case Z -> Direction.SOUTH;
               default -> throw new IncompatibleClassChangeError();
            };

            Direction second = switch (cross) {
               case X -> Direction.EAST;
               case Y -> throw new UnsupportedOperationException();
               case Z -> Direction.NORTH;
               default -> throw new IncompatibleClassChangeError();
            };
            if (rotations == 3) {
               Direction temp = first;
               first = second;
               second = temp;
            }

            if (open) {
               Direction facing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
               if (facing == first) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.HALF, Half.BOTTOM);
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.OPEN, false);
               } else if (facing == second) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.HALF, Half.TOP);
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.OPEN, false);
               }
            } else {
               Half half = (Half)blockState.getValue(BlockStateProperties.HALF);
               if (half == Half.TOP) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, first);
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.OPEN, true);
               } else if (half == Half.BOTTOM) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, second);
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.OPEN, true);
               }
            }
         } else if (rotations == 2) {
            blockState = (BlockState)blockState.cycle(BlockStateProperties.HALF);
            Direction facing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (facing.getAxis() == cross) {
               blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing.getOpposite());
            }
         }

         return blockState;
      } else {
         if (blockState.hasProperty(BlockStateProperties.AXIS)) {
            Axis axis = (Axis)blockState.getValue(BlockStateProperties.AXIS);
            if (rotations == 1 || rotations == 3) {
               if (axis == cross) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.AXIS, Axis.Y);
               } else if (axis == Axis.Y) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.AXIS, cross);
               }
            }
         }

         if (blockState.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = (Direction)blockState.getValue(BlockStateProperties.FACING);
            if (facing.getAxis() != main) {
               Direction newFacing = switch (rotation) {
                  case NONE -> facing;
                  case CLOCKWISE_90 -> facing.getClockWise(main);
                  case CLOCKWISE_180 -> facing.getOpposite();
                  case COUNTERCLOCKWISE_90 -> facing.getCounterClockWise(main);
                  default -> throw new IncompatibleClassChangeError();
               };
               blockState = (BlockState)blockState.setValue(BlockStateProperties.FACING, newFacing);
            }
         }

         if (blockState.hasProperty(BlockStateProperties.SLAB_TYPE) && rotations == 2) {
            SlabType slabType = (SlabType)blockState.getValue(BlockStateProperties.SLAB_TYPE);

            blockState = (BlockState)blockState.setValue(BlockStateProperties.SLAB_TYPE, switch (slabType) {
               case TOP -> SlabType.BOTTOM;
               case BOTTOM -> SlabType.TOP;
               case DOUBLE -> SlabType.DOUBLE;
               default -> throw new IncompatibleClassChangeError();
            });
         }

         return blockState;
      }
   }

   public static BlockState rotateY(BlockState blockState, Rotation rotation) {
      if (rotation == Rotation.NONE) {
         return blockState;
      } else if (ignoreRotation.contains(blockState.getBlock())) {
         return blockState;
      } else if (!customRotateY.containsKey(blockState)) {
         return blockState.rotate(rotation);
      } else {
         int rotations;
         for (rotations = switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> throw new IncompatibleClassChangeError();
         }; rotations > 0; rotations--) {
            BlockState rotated = customRotateY.get(blockState);
            if (rotated == blockState) {
               return blockState;
            }

            if (rotated == null) {
               break;
            }

            blockState = rotated;
         }

         if (rotations > 0) {
            Rotation remainingRotation = switch (rotations) {
               case 1 -> Rotation.CLOCKWISE_90;
               case 2 -> Rotation.CLOCKWISE_180;
               case 3 -> Rotation.COUNTERCLOCKWISE_90;
               default -> throw new FaultyImplementationError();
            };
            return blockState.rotate(remainingRotation);
         } else {
            return blockState;
         }
      }
   }

   public static BlockState flipX(BlockState blockState) {
      if (ignoreRotation.contains(blockState.getBlock())) {
         return blockState;
      } else {
         BlockState custom = customFlipX.get(blockState);
         if (custom != null) {
            return custom;
         } else {
            if (blockState.getBlock() instanceof ChestBlock) {
               try {
                  return doFixedChestFlip(blockState, Axis.X, Axis.Z);
               } catch (Exception var3) {
               }
            }

            return blockState.mirror(Mirror.FRONT_BACK);
         }
      }
   }

   @NotNull
   private static BlockState doFixedChestFlip(BlockState blockState, Axis parallel, Axis perpendicular) {
      Direction direction = (Direction)blockState.getValue(ChestBlock.FACING);
      if (direction.getAxis() == parallel) {
         ChestType chestType = (ChestType)blockState.getValue(ChestBlock.TYPE);

         blockState = switch (chestType) {
            case SINGLE -> blockState;
            case LEFT -> (BlockState)blockState.setValue(ChestBlock.TYPE, ChestType.RIGHT);
            case RIGHT -> (BlockState)blockState.setValue(ChestBlock.TYPE, ChestType.LEFT);
            default -> throw new IncompatibleClassChangeError();
         };
         return (BlockState)blockState.setValue(ChestBlock.FACING, direction.getOpposite());
      } else if (direction.getAxis() == perpendicular) {
         ChestType chestType = (ChestType)blockState.getValue(ChestBlock.TYPE);

         return switch (chestType) {
            case SINGLE -> blockState;
            case LEFT -> (BlockState)blockState.setValue(ChestBlock.TYPE, ChestType.RIGHT);
            case RIGHT -> (BlockState)blockState.setValue(ChestBlock.TYPE, ChestType.LEFT);
            default -> throw new IncompatibleClassChangeError();
         };
      } else {
         return blockState;
      }
   }

   public static BlockState flipZ(BlockState blockState) {
      if (ignoreRotation.contains(blockState.getBlock())) {
         return blockState;
      } else {
         BlockState custom = customFlipZ.get(blockState);
         if (custom != null) {
            return custom;
         } else {
            if (blockState.getBlock() instanceof ChestBlock) {
               try {
                  return doFixedChestFlip(blockState, Axis.Z, Axis.X);
               } catch (Exception var3) {
               }
            }

            return blockState.mirror(Mirror.LEFT_RIGHT);
         }
      }
   }

   public static BlockState flipY(BlockState blockState) {
      if (ignoreRotation.contains(blockState.getBlock())) {
         return blockState;
      } else {
         BlockState custom = customFlipY.get(blockState);
         if (custom != null) {
            return custom;
         } else if (blockState.hasProperty(BlockStateProperties.HALF)) {
            Half half = (Half)blockState.getValue(BlockStateProperties.HALF);
            half = half == Half.TOP ? Half.BOTTOM : Half.TOP;
            return (BlockState)blockState.setValue(BlockStateProperties.HALF, half);
         } else if (blockState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = (DoubleBlockHalf)blockState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            half = half == DoubleBlockHalf.UPPER ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
            return (BlockState)blockState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, half);
         } else {
            if (blockState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
               SlabType slabType = (SlabType)blockState.getValue(BlockStateProperties.SLAB_TYPE);
               if (slabType == SlabType.TOP) {
                  return (BlockState)blockState.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
               }

               if (slabType == SlabType.BOTTOM) {
                  return (BlockState)blockState.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
               }
            }

            if (blockState.hasProperty(BlockStateProperties.FACING)) {
               Direction facing = (Direction)blockState.getValue(BlockStateProperties.FACING);
               if (facing.getAxis() == Axis.Y) {
                  return (BlockState)blockState.setValue(BlockStateProperties.FACING, facing.getOpposite());
               }
            }

            if (blockState.hasProperty(BlockStateProperties.UP) && blockState.hasProperty(BlockStateProperties.DOWN)) {
               boolean up = (Boolean)blockState.getValue(BlockStateProperties.UP);
               boolean down = (Boolean)blockState.getValue(BlockStateProperties.DOWN);
               return (BlockState)((BlockState)blockState.setValue(BlockStateProperties.UP, down)).setValue(BlockStateProperties.DOWN, up);
            } else {
               return blockState;
            }
         }
      }
   }

   @Deprecated
   public static BlockState readVanillaStateByString(String input) {
      try {
         BlockResult result = BlockStateParser.parseForBlock(VersionUtils.createLookup(BuiltInRegistries.BLOCK), input, false);
         return result.blockState();
      } catch (Exception var2) {
         return Blocks.AIR.defaultBlockState();
      }
   }
}
