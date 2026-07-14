package com.moulberry.axiom.capabilities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.block_maps.DyedBlockMap;
import com.moulberry.axiom.block_maps.FlowerPotBlockMap;
import com.moulberry.axiom.block_maps.HDVoxelMap;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Tinker {
   private static final Map<Block, Block> BI_STRIPPABLE_MAP = new HashMap<>();

   public static InteractionResult performUseItemOn(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return InteractionResult.PASS;
      } else if (!AxiomClient.isAxiomActive()) {
         return InteractionResult.PASS;
      } else if (!Capability.TINKER.isEnabled()) {
         return InteractionResult.PASS;
      } else if (localPlayer.isShiftKeyDown()) {
         return InteractionResult.PASS;
      } else {
         ItemStack itemStack = localPlayer.getItemInHand(interactionHand);
         BlockPos blockPos = blockHitResult.getBlockPos();
         BlockState blockState = level.getBlockState(blockPos);
         BlockState originalBlockState = blockState;
         CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
         if (customBlockState != null && customBlockState.getProperties().isEmpty()) {
            return InteractionResult.PASS;
         } else {
            Block block = blockState.getBlock();
            Map<BlockPos, BlockState> extraBlocks = new HashMap<>();
            if (itemStack.isEmpty() && interactionHand == InteractionHand.MAIN_HAND) {
               Direction direction = blockHitResult.getDirection();
               if (block instanceof PistonBaseBlock) {
                  Direction currentFacing = (Direction)blockState.getValue(BlockStateProperties.FACING);
                  PistonType pistonType = block == Blocks.STICKY_PISTON ? PistonType.STICKY : PistonType.DEFAULT;
                  BlockPos offset = blockPos.relative(currentFacing);
                  boolean extended = (Boolean)blockState.getValue(PistonBaseBlock.EXTENDED);
                  if (currentFacing == direction) {
                     if (!extended) {
                        blockState = (BlockState)blockState.setValue(PistonBaseBlock.EXTENDED, true);
                        extraBlocks.put(
                           offset,
                           (BlockState)((BlockState)Blocks.PISTON_HEAD.defaultBlockState().setValue(BlockStateProperties.PISTON_TYPE, pistonType))
                              .setValue(BlockStateProperties.FACING, direction)
                        );
                     }
                  } else {
                     BlockState headBlock = level.getBlockState(blockPos.relative(currentFacing));
                     if (extended) {
                        if (headBlock.is(Blocks.PISTON_HEAD)
                           && headBlock.getValue(BlockStateProperties.PISTON_TYPE) == pistonType
                           && headBlock.getValue(BlockStateProperties.FACING) == currentFacing) {
                           extraBlocks.put(offset, Blocks.AIR.defaultBlockState());
                        }

                        blockState = (BlockState)blockState.setValue(BlockStateProperties.EXTENDED, false);
                     }

                     blockState = (BlockState)blockState.setValue(BlockStateProperties.FACING, direction);
                  }
               } else if (block instanceof LiquidBlock && blockState.hasProperty(BlockStateProperties.LEVEL)) {
                  int fluidLevel = (Integer)blockState.getValue(BlockStateProperties.LEVEL);
                  if (++fluidLevel >= 8) {
                     fluidLevel = 0;
                  }

                  blockState = (BlockState)blockState.setValue(BlockStateProperties.LEVEL, fluidLevel);
               } else if (block instanceof PistonHeadBlock) {
                  Direction headFacingDirection = (Direction)blockState.getValue(BlockStateProperties.FACING);
                  BlockPos baseBlockPosition = blockPos.relative(headFacingDirection, -1);
                  BlockState baseBlock = level.getBlockState(baseBlockPosition);
                  Block baseBlockType = blockState.getValue(BlockStateProperties.PISTON_TYPE) == PistonType.DEFAULT ? Blocks.PISTON : Blocks.STICKY_PISTON;
                  if (baseBlock.is(baseBlockType)
                     && (Boolean)baseBlock.getValue(PistonBaseBlock.EXTENDED)
                     && baseBlock.getValue(BlockStateProperties.FACING) == headFacingDirection) {
                     blockState = Blocks.AIR.defaultBlockState();
                     extraBlocks.put(baseBlockPosition, (BlockState)baseBlock.setValue(PistonBaseBlock.EXTENDED, false));
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.FACING, direction);
                  }
               } else if (block instanceof AbstractFurnaceBlock) {
                  if (direction.getAxis() != Axis.Y) {
                     Direction currentFacing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                     if (currentFacing == direction) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.LIT);
                     } else {
                        blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                     }
                  }
               } else if (block instanceof CarvedPumpkinBlock) {
                  if (direction.getAxis() != Axis.Y) {
                     Direction currentFacing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                     if (currentFacing == direction) {
                        if (block == Blocks.CARVED_PUMPKIN) {
                           blockState = copyProperties(blockState, Blocks.JACK_O_LANTERN.defaultBlockState());
                        } else {
                           blockState = copyProperties(blockState, Blocks.CARVED_PUMPKIN.defaultBlockState());
                        }
                     } else {
                        blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                     }
                  }
               } else if (block instanceof TrapDoorBlock trapDoorBlock && !trapDoorBlock.type.canOpenByHand()) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.OPEN);
               } else if (block instanceof DoorBlock doorBlock && !doorBlock.type.canOpenByHand()) {
                  boolean open = !(Boolean)blockState.getValue(BlockStateProperties.OPEN);
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.OPEN, open);
                  Direction otherDoorPart = blockState.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? Direction.DOWN : Direction.UP;
                  BlockPos otherDoorPos = blockPos.relative(otherDoorPart);
                  BlockState otherDoorBlockState = level.getBlockState(otherDoorPos);
                  if (otherDoorBlockState.getBlock() instanceof DoorBlock) {
                     extraBlocks.put(otherDoorPos, (BlockState)otherDoorBlockState.setValue(BlockStateProperties.OPEN, open));
                  }
               } else if (block == Blocks.SMOOTH_STONE) {
                  Vec3 location = blockHitResult.getLocation();
                  double relativeY = location.y - blockPos.getY();
                  if (relativeY > 0.5) {
                     blockState = (BlockState)Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
                  } else {
                     blockState = (BlockState)Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
                  }
               } else if (block == Blocks.SMOOTH_STONE_SLAB) {
                  SlabType slabType = (SlabType)blockState.getValue(BlockStateProperties.SLAB_TYPE);
                  if (slabType == SlabType.BOTTOM && direction == Direction.UP) {
                     blockState = Blocks.SMOOTH_STONE.defaultBlockState();
                  } else if (slabType == SlabType.TOP && direction == Direction.DOWN) {
                     blockState = Blocks.SMOOTH_STONE.defaultBlockState();
                  }
               } else if (block instanceof SlabBlock) {
                  HDVoxelMap.HDVoxelBaseBlocks hdVoxelBaseBlocks = HDVoxelMap.getAssociatedBlocks(block);
                  if (hdVoxelBaseBlocks != null) {
                     Vec3 location = blockHitResult.getLocation();
                     double relativeX = location.x - blockPos.getX();
                     double relativeY = location.y - blockPos.getY();
                     double relativeZ = location.z - blockPos.getZ();
                     double offsetRelativeY = relativeY + direction.getStepY() * 0.25F;
                     if (offsetRelativeY < 0.0 || offsetRelativeY > 1.0) {
                        return InteractionResult.PASS;
                     }

                     int index = 0;
                     if (relativeZ <= 0.5) {
                        index++;
                     }

                     if (relativeX <= 0.5) {
                        index += 2;
                     }

                     int voxel = 0;
                     switch ((SlabType)blockState.getValue(BlockStateProperties.SLAB_TYPE)) {
                        case TOP:
                           voxel = 15 | 1 << index + 4;
                           if (direction.getAxis() != Axis.Y) {
                              return InteractionResult.PASS;
                           }
                           break;
                        case BOTTOM:
                           voxel = 240 | 1 << index;
                           if (direction.getAxis() != Axis.Y) {
                              return InteractionResult.PASS;
                           }
                           break;
                        case DOUBLE:
                           if (relativeY <= 0.5) {
                              index += 4;
                           }

                           voxel = 0xFF & ~(1 << index);
                     }

                     BlockState newBlockState = HDVoxelMap.createFromVoxel222(
                        voxel,
                        hdVoxelBaseBlocks.full().defaultBlockState(),
                        hdVoxelBaseBlocks.stair().defaultBlockState(),
                        hdVoxelBaseBlocks.slab().defaultBlockState()
                     );
                     if (newBlockState != null && blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        boolean waterlogged = (Boolean)blockState.getValue(BlockStateProperties.WATERLOGGED);
                        if (newBlockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                           newBlockState = (BlockState)newBlockState.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
                        }
                     }

                     blockState = newBlockState;
                  }
               } else if (block instanceof StairBlock) {
                  HDVoxelMap.HDVoxelBaseBlocks hdVoxelBaseBlocks = HDVoxelMap.getAssociatedBlocks(block);
                  if (hdVoxelBaseBlocks != null) {
                     int voxel = HDVoxelMap.createVoxel222ForStair(blockState);
                     Vec3 locationx = blockHitResult.getLocation();
                     double relativeXx = locationx.x - blockPos.getX();
                     double relativeYx = locationx.y - blockPos.getY();
                     double relativeZx = locationx.z - blockPos.getZ();
                     int indexx = 0;
                     if (relativeZx - direction.getStepZ() * 0.25F <= 0.5) {
                        indexx++;
                     }

                     if (relativeXx - direction.getStepX() * 0.25F <= 0.5) {
                        indexx += 2;
                     }

                     if (relativeYx - direction.getStepY() * 0.25F <= 0.5) {
                        indexx += 4;
                     }

                     int removeVoxel = voxel & ~(1 << indexx);
                     BlockState newBlockState = HDVoxelMap.createFromVoxel222(
                        removeVoxel,
                        hdVoxelBaseBlocks.full().defaultBlockState(),
                        hdVoxelBaseBlocks.stair().defaultBlockState(),
                        hdVoxelBaseBlocks.slab().defaultBlockState()
                     );
                     if (newBlockState == null) {
                        indexx = 0;
                        if (relativeZx + direction.getStepZ() * 0.25F <= 0.5) {
                           indexx++;
                        }

                        if (relativeXx + direction.getStepX() * 0.25F <= 0.5) {
                           indexx += 2;
                        }

                        if (relativeYx + direction.getStepY() * 0.25F <= 0.5) {
                           indexx += 4;
                        }

                        int addVoxel = voxel | 1 << indexx;
                        newBlockState = HDVoxelMap.createFromVoxel222(
                           addVoxel,
                           hdVoxelBaseBlocks.full().defaultBlockState(),
                           hdVoxelBaseBlocks.stair().defaultBlockState(),
                           hdVoxelBaseBlocks.slab().defaultBlockState()
                        );
                     }

                     if (newBlockState != null && blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        boolean waterlogged = (Boolean)blockState.getValue(BlockStateProperties.WATERLOGGED);
                        if (newBlockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                           newBlockState = (BlockState)newBlockState.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
                        }
                     }

                     blockState = newBlockState;
                  }
               } else if (block instanceof BarrelBlock) {
                  Direction currentFacing = (Direction)blockState.getValue(BlockStateProperties.FACING);
                  if (currentFacing == direction) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.OPEN);
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.FACING, direction);
                  }
               } else if (block instanceof FenceBlock || block instanceof IronBarsBlock) {
                  Vec3 locationxx = blockHitResult.getLocation();
                  double relativeXxx = locationxx.x - blockPos.getX();
                  double relativeZxx = locationxx.z - blockPos.getZ();
                  double absX = Math.abs(relativeXxx - 0.5);
                  double absZ = Math.abs(relativeZxx - 0.5);
                  if (absX > absZ) {
                     if (direction.getAxis() == Axis.Y && absX < 0.125) {
                        return InteractionResult.PASS;
                     }

                     if (relativeXxx > 0.5) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.EAST);
                     } else {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.WEST);
                     }
                  } else {
                     if (direction.getAxis() == Axis.Y && absZ < 0.125) {
                        return InteractionResult.PASS;
                     }

                     if (relativeZxx > 0.5) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.SOUTH);
                     } else {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.NORTH);
                     }
                  }
               } else if (block instanceof WallBlock) {
                  Vec3 locationxx = blockHitResult.getLocation();
                  double relativeXxx = locationxx.x - blockPos.getX();
                  double relativeZxx = locationxx.z - blockPos.getZ();
                  double absX = Math.abs(relativeXxx - 0.5);
                  double absZ = Math.abs(relativeZxx - 0.5);
                  if (absX > absZ) {
                     if (direction.getAxis() == Axis.Y && absX < 0.25) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.UP);
                     } else if (relativeXxx > 0.5) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.EAST_WALL);
                     } else {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.WEST_WALL);
                     }
                  } else if (direction.getAxis() == Axis.Y && absZ < 0.25) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.UP);
                  } else if (relativeZxx > 0.5) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.SOUTH_WALL);
                  } else {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.NORTH_WALL);
                  }

                  if (!(Boolean)blockState.getValue(BlockStateProperties.UP)
                     && blockState.getValue(BlockStateProperties.EAST_WALL) == WallSide.NONE
                     && blockState.getValue(BlockStateProperties.WEST_WALL) == WallSide.NONE
                     && blockState.getValue(BlockStateProperties.SOUTH_WALL) == WallSide.NONE
                     && blockState.getValue(BlockStateProperties.NORTH_WALL) == WallSide.NONE) {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.UP, true);
                  }
               } else if (block == Blocks.BREWING_STAND) {
                  Vec3 locationxxx = blockHitResult.getLocation();
                  double relativeXxxx = locationxxx.x - blockPos.getX();
                  double relativeYxx = locationxxx.y - blockPos.getY();
                  double relativeZxxx = locationxxx.z - blockPos.getZ();
                  if (relativeYxx < 0.15) {
                     if (relativeXxxx >= 0.5625 && relativeZxxx >= 0.3125 && relativeXxxx <= 0.9375 && relativeZxxx <= 0.6875) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.HAS_BOTTLE_0);
                     } else if (relativeXxxx >= 0.0625 && relativeZxxx >= 0.0625 && relativeXxxx <= 0.4375 && relativeZxxx <= 0.4375) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.HAS_BOTTLE_1);
                     } else if (relativeXxxx >= 0.0625 && relativeZxxx >= 0.5625 && relativeXxxx <= 0.4375 && relativeZxxx <= 0.9375) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.HAS_BOTTLE_2);
                     }
                  }
               } else if (block instanceof FenceGateBlock) {
                  Direction facing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                  if (direction == Direction.UP) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.IN_WALL);
                  } else if (direction.getAxis() != facing.getAxis()) {
                     switch (facing) {
                        case EAST:
                           blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
                           break;
                        case SOUTH:
                           blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
                           break;
                        case WEST:
                           blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                           break;
                        case NORTH:
                           blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
                     }
                  }
               } else if (block instanceof HugeMushroomBlock) {
                  BooleanProperty property = switch (direction) {
                     case EAST -> BlockStateProperties.EAST;
                     case SOUTH -> BlockStateProperties.SOUTH;
                     case WEST -> BlockStateProperties.WEST;
                     case NORTH -> BlockStateProperties.NORTH;
                     case DOWN -> BlockStateProperties.DOWN;
                     case UP -> BlockStateProperties.UP;
                     default -> throw new IncompatibleClassChangeError();
                  };
                  blockState = (BlockState)blockState.cycle(property);
               } else if (block instanceof PipeBlock) {
                  Vec3 locationxxx = blockHitResult.getLocation();
                  double relativeXxxx = locationxxx.x - blockPos.getX();
                  double relativeYxx = locationxxx.y - blockPos.getY();
                  double relativeZxxx = locationxxx.z - blockPos.getZ();
                  double absXx = Math.abs(relativeXxxx - 0.5);
                  double absY = Math.abs(relativeYxx - 0.5);
                  double absZx = Math.abs(relativeZxxx - 0.5);
                  if (absXx > absZx && absXx > absY) {
                     if (relativeXxxx > 0.5) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.EAST);
                     } else {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.WEST);
                     }
                  } else if (absY > absZx) {
                     if (relativeYxx > 0.5) {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.UP);
                     } else {
                        blockState = (BlockState)blockState.cycle(BlockStateProperties.DOWN);
                     }
                  } else if (relativeZxxx > 0.5) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.SOUTH);
                  } else {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.NORTH);
                  }
               } else if (block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.HANGING);
               } else if (block instanceof BaseRailBlock) {
                  Property<RailShape> property;
                  if (blockState.hasProperty(BlockStateProperties.RAIL_SHAPE)) {
                     property = BlockStateProperties.RAIL_SHAPE;
                  } else {
                     if (!blockState.hasProperty(BlockStateProperties.RAIL_SHAPE_STRAIGHT)) {
                        return InteractionResult.PASS;
                     }

                     property = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
                  }

                  Vec3 locationxxx = blockHitResult.getLocation();
                  double relativeXxxx = locationxxx.x - blockPos.getX();
                  double relativeZxxx = locationxxx.z - blockPos.getZ();
                  double absXx = Math.abs(relativeXxxx - 0.5);
                  double absZx = Math.abs(relativeZxxx - 0.5);
                  boolean east = level.getBlockState(blockPos.east()).getBlock() instanceof BaseRailBlock;
                  boolean west = level.getBlockState(blockPos.west()).getBlock() instanceof BaseRailBlock;
                  boolean north = level.getBlockState(blockPos.north()).getBlock() instanceof BaseRailBlock;
                  boolean south = level.getBlockState(blockPos.south()).getBlock() instanceof BaseRailBlock;
                  RailShape shape = (RailShape)blockState.getValue(property);
                  switch (shape) {
                     case ASCENDING_EAST:
                     case ASCENDING_WEST:
                        blockState = (BlockState)blockState.setValue(property, RailShape.EAST_WEST);
                        break;
                     case ASCENDING_NORTH:
                     case ASCENDING_SOUTH:
                        blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_SOUTH);
                        break;
                     case EAST_WEST:
                        if (absXx > absZx) {
                           if (relativeXxxx < 0.5) {
                              if (east) {
                                 blockState = (BlockState)blockState.setValue(property, RailShape.ASCENDING_WEST);
                              }
                           } else if (west) {
                              blockState = (BlockState)blockState.setValue(property, RailShape.ASCENDING_EAST);
                           }
                        }

                        if (originalBlockState == blockState) {
                           if (!east && !west) {
                              blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_SOUTH);
                           } else if (property == BlockStateProperties.RAIL_SHAPE) {
                              boolean eastBias;
                              if (east && !west) {
                                 eastBias = true;
                              } else if (!east && west) {
                                 eastBias = false;
                              } else {
                                 eastBias = relativeXxxx > 0.5;
                              }

                              if (eastBias) {
                                 if (relativeZxxx < 0.5) {
                                    blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_EAST);
                                 } else {
                                    blockState = (BlockState)blockState.setValue(property, RailShape.SOUTH_EAST);
                                 }
                              } else if (relativeZxxx < 0.5) {
                                 blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_WEST);
                              } else {
                                 blockState = (BlockState)blockState.setValue(property, RailShape.SOUTH_WEST);
                              }
                           }
                        }
                        break;
                     case NORTH_EAST:
                     case NORTH_WEST:
                     case SOUTH_EAST:
                     case SOUTH_WEST:
                        boolean x = shape != RailShape.NORTH_EAST && shape != RailShape.SOUTH_EAST ? west : east;
                        boolean z = shape != RailShape.NORTH_EAST && shape != RailShape.NORTH_WEST ? south : north;
                        if (x && !z) {
                           blockState = (BlockState)blockState.setValue(property, RailShape.EAST_WEST);
                        } else if (!x && z) {
                           blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_SOUTH);
                        } else if (absXx > absZx) {
                           blockState = (BlockState)blockState.setValue(property, RailShape.EAST_WEST);
                        } else {
                           blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_SOUTH);
                        }
                        break;
                     case NORTH_SOUTH:
                        if (absZx > absXx) {
                           if (relativeZxxx < 0.5) {
                              if (south) {
                                 blockState = (BlockState)blockState.setValue(property, RailShape.ASCENDING_NORTH);
                              }
                           } else if (north) {
                              blockState = (BlockState)blockState.setValue(property, RailShape.ASCENDING_SOUTH);
                           }
                        }

                        if (originalBlockState == blockState) {
                           if (!north && !south) {
                              blockState = (BlockState)blockState.setValue(property, RailShape.EAST_WEST);
                           } else if (property == BlockStateProperties.RAIL_SHAPE) {
                              boolean northBias;
                              if (north && !south) {
                                 northBias = true;
                              } else if (!north && south) {
                                 northBias = false;
                              } else {
                                 northBias = relativeXxxx > 0.5;
                              }

                              if (northBias) {
                                 if (relativeXxxx < 0.5) {
                                    blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_WEST);
                                 } else {
                                    blockState = (BlockState)blockState.setValue(property, RailShape.NORTH_EAST);
                                 }
                              } else if (relativeXxxx < 0.5) {
                                 blockState = (BlockState)blockState.setValue(property, RailShape.SOUTH_WEST);
                              } else {
                                 blockState = (BlockState)blockState.setValue(property, RailShape.SOUTH_EAST);
                              }
                           }
                        }
                        break;
                     default:
                        if (blockState.hasProperty(BlockStateProperties.POWERED)) {
                           blockState = (BlockState)blockState.cycle(BlockStateProperties.POWERED);
                        }
                  }
               } else if (block instanceof ChiseledBookShelfBlock) {
                  if (direction.getAxis() != Axis.Y) {
                     Direction currentFacing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
                     if (currentFacing == direction) {
                        Vec3 locationxxx = blockHitResult.getLocation();
                        double relativeXxxx = locationxxx.x - blockPos.getX();
                        double relativeYxx = locationxxx.y - blockPos.getY();
                        double relativeZxxx = locationxxx.z - blockPos.getZ();
                        double across;
                        if (currentFacing.getAxis() == Axis.X) {
                           across = 1.0 - relativeZxxx;
                        } else {
                           across = relativeXxxx;
                        }

                        if (currentFacing.getAxisDirection() == AxisDirection.NEGATIVE) {
                           across = 1.0 - across;
                        }

                        if (relativeYxx > 0.5) {
                           if (across < 0.33333334F) {
                              blockState = (BlockState)blockState.cycle(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED);
                           } else if (across < 0.6666667F) {
                              blockState = (BlockState)blockState.cycle(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED);
                           } else {
                              blockState = (BlockState)blockState.cycle(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED);
                           }
                        } else if (across < 0.33333334F) {
                           blockState = (BlockState)blockState.cycle(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED);
                        } else if (across < 0.6666667F) {
                           blockState = (BlockState)blockState.cycle(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED);
                        } else {
                           blockState = (BlockState)blockState.cycle(BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED);
                        }
                     } else {
                        blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                     }
                  }
               } else if (block == Blocks.OBSERVER) {
                  Direction blockDirection = (Direction)blockState.getValue(BlockStateProperties.FACING);
                  if (blockDirection == direction.getOpposite()) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.POWERED);
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.FACING, direction.getOpposite());
                  }
               } else if (block == Blocks.END_PORTAL_FRAME && direction != Direction.DOWN) {
                  if (direction == Direction.UP) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.EYE);
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction.getOpposite());
                  }
               } else if (block == Blocks.BAMBOO_BLOCK) {
                  Axis axis = (Axis)blockState.getValue(BlockStateProperties.AXIS);
                  if (axis != direction.getAxis()) {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.AXIS, direction.getAxis());
                  } else {
                     blockState = (BlockState)Blocks.STRIPPED_BAMBOO_BLOCK.defaultBlockState().setValue(BlockStateProperties.AXIS, direction.getAxis());
                  }
               } else if (block == Blocks.STRIPPED_BAMBOO_BLOCK) {
                  Axis axis = (Axis)blockState.getValue(BlockStateProperties.AXIS);
                  if (axis != direction.getAxis()) {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.AXIS, direction.getAxis());
                  } else {
                     blockState = (BlockState)Blocks.BAMBOO_BLOCK.defaultBlockState().setValue(BlockStateProperties.AXIS, direction.getAxis());
                  }
               } else if (block instanceof ChestBlock && direction.getAxis() != Axis.Y) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
               } else if (BI_STRIPPABLE_MAP.containsKey(block)) {
                  BlockState newBlockStatex = BI_STRIPPABLE_MAP.get(block).defaultBlockState();
                  blockState = copyProperties(blockState, newBlockStatex);
               } else if (block == Blocks.BELL) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.HORIZONTAL_FACING);
               } else if (block == Blocks.TALL_GRASS) {
                  DoubleBlockHalf half = (DoubleBlockHalf)blockState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                  if (half == DoubleBlockHalf.UPPER) {
                     blockState = Blocks.AIR.defaultBlockState();
                     extraBlocks.put(blockPos.below(), Blocks.SHORT_GRASS.defaultBlockState());
                  } else if (half == DoubleBlockHalf.LOWER) {
                     blockState = Blocks.SHORT_GRASS.defaultBlockState();
                     extraBlocks.put(blockPos.above(), Blocks.AIR.defaultBlockState());
                  }
               } else if (block == Blocks.SHORT_GRASS && level.getBlockState(blockPos.above()).getBlock() == Blocks.AIR) {
                  blockState = (BlockState)Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                  extraBlocks.put(
                     blockPos.above(),
                     (BlockState)Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                  );
               } else if (block == Blocks.LARGE_FERN) {
                  DoubleBlockHalf half = (DoubleBlockHalf)blockState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                  if (half == DoubleBlockHalf.UPPER) {
                     blockState = Blocks.AIR.defaultBlockState();
                     extraBlocks.put(blockPos.below(), Blocks.FERN.defaultBlockState());
                  } else if (half == DoubleBlockHalf.LOWER) {
                     blockState = Blocks.FERN.defaultBlockState();
                     extraBlocks.put(blockPos.above(), Blocks.AIR.defaultBlockState());
                  }
               } else if (block == Blocks.BAMBOO_SAPLING) {
                  blockState = (BlockState)Blocks.BAMBOO.defaultBlockState().setValue(BlockStateProperties.BAMBOO_LEAVES, BambooLeaves.NONE);
               } else if (block == Blocks.BAMBOO) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.BAMBOO_LEAVES);
                  BambooLeaves bambooLeaves = (BambooLeaves)blockState.getValue(BlockStateProperties.BAMBOO_LEAVES);
                  if (bambooLeaves == BambooLeaves.NONE) {
                     Block belowBlock = level.getBlockState(blockPos.below()).getBlock();
                     if (belowBlock != Blocks.BAMBOO && belowBlock != Blocks.BAMBOO_SAPLING) {
                        blockState = Blocks.BAMBOO_SAPLING.defaultBlockState();
                     }
                  }
               } else if (block == Blocks.CAVE_VINES) {
                  Block belowBlock = level.getBlockState(blockPos.below()).getBlock();
                  if (belowBlock != Blocks.CAVE_VINES && belowBlock != Blocks.CAVE_VINES_PLANT) {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.BERRIES);
                  } else {
                     blockState = Blocks.CAVE_VINES_PLANT.defaultBlockState();
                  }
               } else if (block == Blocks.WEEPING_VINES) {
                  blockState = Blocks.WEEPING_VINES_PLANT.defaultBlockState();
               } else if (block == Blocks.FLOWERING_AZALEA) {
                  blockState = Blocks.AZALEA.defaultBlockState();
               } else if (block == Blocks.AZALEA) {
                  blockState = Blocks.FLOWERING_AZALEA.defaultBlockState();
               } else if (block == Blocks.GRASS_BLOCK) {
                  blockState = Blocks.DIRT_PATH.defaultBlockState();
               } else if (block == Blocks.DIRT_PATH) {
                  blockState = Blocks.GRASS_BLOCK.defaultBlockState();
               } else if (block == Blocks.SNIFFER_EGG) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.HATCH);
               } else if (block == Blocks.RESPAWN_ANCHOR) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
               } else if (block == Blocks.WEEPING_VINES_PLANT) {
                  blockState = Blocks.WEEPING_VINES.defaultBlockState();
               } else if (block == Blocks.TWISTING_VINES) {
                  blockState = Blocks.TWISTING_VINES_PLANT.defaultBlockState();
               } else if (block == Blocks.TWISTING_VINES_PLANT) {
                  blockState = Blocks.TWISTING_VINES.defaultBlockState();
               } else if (block == Blocks.FERN && level.getBlockState(blockPos.above()).getBlock() == Blocks.AIR) {
                  blockState = (BlockState)Blocks.LARGE_FERN.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                  extraBlocks.put(
                     blockPos.above(),
                     (BlockState)Blocks.LARGE_FERN.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)
                  );
               } else if (block instanceof GlazedTerracottaBlock) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.HORIZONTAL_FACING);
               } else if (block instanceof FarmBlock) {
                  if ((Integer)blockState.getValue(BlockStateProperties.MOISTURE) != 7) {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.MOISTURE, 7);
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.MOISTURE, 0);
                  }
               } else if (block instanceof ScaffoldingBlock) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.BOTTOM);
               } else if (block == Blocks.SMALL_AMETHYST_BUD) {
                  blockState = copyProperties(blockState, Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState());
               } else if (block == Blocks.MEDIUM_AMETHYST_BUD) {
                  blockState = copyProperties(blockState, Blocks.LARGE_AMETHYST_BUD.defaultBlockState());
               } else if (block == Blocks.LARGE_AMETHYST_BUD) {
                  blockState = copyProperties(blockState, Blocks.AMETHYST_CLUSTER.defaultBlockState());
               } else if (block == Blocks.AMETHYST_CLUSTER) {
                  blockState = copyProperties(blockState, Blocks.SMALL_AMETHYST_BUD.defaultBlockState());
               } else if (blockState.hasProperty(BlockStateProperties.AXIS)) {
                  blockState = (BlockState)blockState.setValue(BlockStateProperties.AXIS, direction.getAxis());
               } else if (blockState.hasProperty(BlockStateProperties.ATTACHED)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.ATTACHED);
               } else if (blockState.hasProperty(BlockStateProperties.FLOWER_AMOUNT)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.FLOWER_AMOUNT);
               } else if (blockState.hasProperty(BlockStateProperties.LIT)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.LIT);
               } else if (blockState.hasProperty(BlockStateProperties.BERRIES)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.BERRIES);
               } else if (blockState.hasProperty(BlockStateProperties.DUSTED)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.DUSTED);
               } else if (blockState.hasProperty(BlockStateProperties.LAYERS)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.LAYERS);
               } else if (blockState.hasProperty(BlockStateProperties.EGGS)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.EGGS);
               } else if (blockState.hasProperty(BlockStateProperties.DRIPSTONE_THICKNESS)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.DRIPSTONE_THICKNESS);
               } else if (block == Blocks.CAULDRON) {
                  blockState = (BlockState)Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 1);
               } else if (blockState.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) {
                  if (block == Blocks.WATER_CAULDRON && (Integer)blockState.getValue(BlockStateProperties.LEVEL_CAULDRON) == 3) {
                     blockState = Blocks.CAULDRON.defaultBlockState();
                  } else {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.LEVEL_CAULDRON);
                  }
               } else if (blockState.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.LEVEL_COMPOSTER);
               } else if (blockState.hasProperty(BlockStateProperties.HAS_BOOK)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.HAS_BOOK);
               } else if (blockState.hasProperty(BlockStateProperties.TILT)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.TILT);
               } else if (blockState.hasProperty(BlockStateProperties.AXIS) && blockState.getProperties().size() == 1) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AXIS);
               } else if (blockState.hasProperty(BlockStateProperties.LEVEL_HONEY)) {
                  int honey = (Integer)blockState.getValue(BlockStateProperties.LEVEL_HONEY);
                  if (honey == 5) {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.LEVEL_HONEY, 0);
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.LEVEL_HONEY, 5);
                  }
               } else if (block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM) {
                  int age = (Integer)blockState.getValue(BlockStateProperties.AGE_7);
                  if (age == 7 && direction.getAxis() != Axis.Y) {
                     Block attachedStem = block == Blocks.MELON_STEM ? Blocks.ATTACHED_MELON_STEM : Blocks.ATTACHED_PUMPKIN_STEM;
                     blockState = attachedStem.defaultBlockState();
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                  } else {
                     blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_7);
                  }
               } else if (block == Blocks.ATTACHED_MELON_STEM || block == Blocks.ATTACHED_PUMPKIN_STEM) {
                  if (direction.getAxis() == Axis.Y) {
                     Block stem = block == Blocks.ATTACHED_MELON_STEM ? Blocks.MELON_STEM : Blocks.PUMPKIN_STEM;
                     blockState = stem.defaultBlockState();
                  } else {
                     blockState = (BlockState)blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
                  }
               } else if (blockState.hasProperty(BlockStateProperties.ROTATION_16)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.ROTATION_16);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_1)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_1);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_2)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_2);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_3)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_3);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_4)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_4);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_5)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_5);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_7)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_7);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_15)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_15);
               } else if (blockState.hasProperty(BlockStateProperties.AGE_25)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.AGE_25);
               } else if (blockState.hasProperty(BlockStateProperties.BITES)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.BITES);
               } else if (!(block instanceof DoorBlock)
                  && !(block instanceof TrapDoorBlock)
                  && !(block instanceof DiodeBlock)
                  && !(block instanceof LeverBlock)
                  && !(block instanceof ButtonBlock)
                  && blockState.hasProperty(BlockStateProperties.POWERED)) {
                  blockState = (BlockState)blockState.cycle(BlockStateProperties.POWERED);
               } else {
                  if (blockHitResult.getLocation().distanceTo(Minecraft.getInstance().player.getEyePosition()) < 0.5) {
                     return InteractionResult.PASS;
                  }

                  HDVoxelMap.HDVoxelBaseBlocks hdVoxelBaseBlocks = HDVoxelMap.getAssociatedBlocks(block);
                  if (hdVoxelBaseBlocks != null) {
                     Vec3 locationxxxx = blockHitResult.getLocation();
                     double relativeXxxxx = locationxxxx.x - blockPos.getX();
                     double relativeYxxx = locationxxxx.y - blockPos.getY();
                     double relativeZxxxx = locationxxxx.z - blockPos.getZ();
                     int indexxx = 0;
                     if (relativeZxxxx <= 0.5) {
                        indexxx++;
                     }

                     if (relativeXxxxx <= 0.5) {
                        indexxx += 2;
                     }

                     if (relativeYxxx <= 0.5) {
                        indexxx += 4;
                     }

                     blockState = HDVoxelMap.createFromVoxel222(
                        0xFF & ~(1 << indexxx),
                        hdVoxelBaseBlocks.full().defaultBlockState(),
                        hdVoxelBaseBlocks.stair().defaultBlockState(),
                        hdVoxelBaseBlocks.slab().defaultBlockState()
                     );
                  }
               }
            } else if (itemStack.getItem() instanceof DyeItem dyeItem) {
               EnumMap<DyeColor, Block> map = DyedBlockMap.getAssociatedDyedBlocks(block);
               if (map != null) {
                  DyeColor dyeColor = dyeItem.getDyeColor();
                  Block newBlock = map.get(dyeColor);
                  if (newBlock != block) {
                     blockState = copyProperties(blockState, newBlock.defaultBlockState());
                  }
               }
            } else if (itemStack.getItem() == Items.MOSS_BLOCK) {
               if (block == Blocks.COBBLESTONE) {
                  blockState = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
               } else if (block == Blocks.COBBLESTONE_STAIRS) {
                  blockState = copyProperties(blockState, Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState());
               } else if (block == Blocks.COBBLESTONE_SLAB) {
                  blockState = copyProperties(blockState, Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState());
               } else if (block == Blocks.COBBLESTONE_WALL) {
                  blockState = copyProperties(blockState, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
               } else if (block == Blocks.STONE_BRICKS) {
                  blockState = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
               } else if (block == Blocks.STONE_BRICK_STAIRS) {
                  blockState = copyProperties(blockState, Blocks.MOSSY_STONE_BRICK_STAIRS.defaultBlockState());
               } else if (block == Blocks.STONE_BRICK_SLAB) {
                  blockState = copyProperties(blockState, Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState());
               } else if (block == Blocks.STONE_BRICK_WALL) {
                  blockState = copyProperties(blockState, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState());
               } else if (block == Blocks.INFESTED_STONE_BRICKS) {
                  blockState = Blocks.INFESTED_MOSSY_STONE_BRICKS.defaultBlockState();
               }
            } else if (itemStack.getItem() == Items.SHEARS) {
               if (block == Blocks.MOSSY_COBBLESTONE) {
                  blockState = Blocks.COBBLESTONE.defaultBlockState();
               } else if (block == Blocks.MOSSY_COBBLESTONE_STAIRS) {
                  blockState = copyProperties(blockState, Blocks.COBBLESTONE_STAIRS.defaultBlockState());
               } else if (block == Blocks.MOSSY_COBBLESTONE_SLAB) {
                  blockState = copyProperties(blockState, Blocks.COBBLESTONE_SLAB.defaultBlockState());
               } else if (block == Blocks.MOSSY_COBBLESTONE_WALL) {
                  blockState = copyProperties(blockState, Blocks.COBBLESTONE_WALL.defaultBlockState());
               } else if (block == Blocks.MOSSY_STONE_BRICKS) {
                  blockState = Blocks.STONE_BRICKS.defaultBlockState();
               } else if (block == Blocks.MOSSY_STONE_BRICK_STAIRS) {
                  blockState = copyProperties(blockState, Blocks.STONE_BRICK_STAIRS.defaultBlockState());
               } else if (block == Blocks.MOSSY_STONE_BRICK_SLAB) {
                  blockState = copyProperties(blockState, Blocks.STONE_BRICK_SLAB.defaultBlockState());
               } else if (block == Blocks.MOSSY_STONE_BRICK_WALL) {
                  blockState = copyProperties(blockState, Blocks.STONE_BRICK_WALL.defaultBlockState());
               } else if (block == Blocks.INFESTED_MOSSY_STONE_BRICKS) {
                  blockState = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
               }
            } else if (itemStack.getItem() instanceof BlockItem blockItem) {
               Block flowerPot = FlowerPotBlockMap.getAssociatedPotBlock(blockItem.getBlock());
               if (flowerPot != null && block instanceof FlowerPotBlock) {
                  blockState = flowerPot.defaultBlockState();
               }
            }

            if (blockState == null) {
               return InteractionResult.PASS;
            } else if (originalBlockState != blockState) {
               BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting();

               try {
                  int seq = blockStatePredictionHandler.currentSequence();
                  extraBlocks.put(blockPos, blockState);
                  if (BuildSymmetry.isActive()) {
                     Map<BlockPos, BlockState> symmetryBlocks = new HashMap<>();

                     for (Entry<BlockPos, BlockState> extraEntry : extraBlocks.entrySet()) {
                        Map<BlockPos, BlockState> mirrored = BuildSymmetry.applySymmetry(Map.of(extraEntry.getKey(), extraEntry.getValue()));
                        Block originalBlock = level.getBlockState(extraEntry.getKey()).getBlock();

                        for (Entry<BlockPos, BlockState> entry : mirrored.entrySet()) {
                           if (!extraBlocks.containsKey(entry.getKey())) {
                              Block mirroredOriginalBlock = level.getBlockState(entry.getKey()).getBlock();
                              if (mirroredOriginalBlock == originalBlock) {
                                 symmetryBlocks.put(entry.getKey(), entry.getValue());
                              }
                           }
                        }
                     }

                     extraBlocks.putAll(symmetryBlocks);
                  }

                  for (Entry<BlockPos, BlockState> entryx : extraBlocks.entrySet()) {
                     level.setBlock(entryx.getKey(), entryx.getValue(), 19);
                  }

                  new AxiomServerboundSetBlock(extraBlocks, false, 2, false, blockHitResult, interactionHand, seq).send();
               } catch (Throwable var34) {
                  if (blockStatePredictionHandler != null) {
                     try {
                        blockStatePredictionHandler.close();
                     } catch (Throwable var33) {
                        var34.addSuppressed(var33);
                     }
                  }

                  throw var34;
               }

               if (blockStatePredictionHandler != null) {
                  blockStatePredictionHandler.close();
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }

   private static BlockState copyProperties(BlockState from, BlockState to) {
      for (Property<?> property : from.getBlock().getStateDefinition().getProperties()) {
         if (to.hasProperty(property)) {
            to = copyProperty(from, to, property);
         }
      }

      return to;
   }

   private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
      return (BlockState)to.setValue(property, from.getValue(property));
   }

   public static boolean startDestroyBlockCreative(BlockHitResult blockHitResult) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return false;
      } else if (Capability.TINKER.isEnabled() && AxiomClient.isAxiomActive()) {
         BlockState blockState = level.getBlockState(blockHitResult.getBlockPos());
         if (blockState.getBlock() instanceof SlabBlock && blockState.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Vec3 location = blockHitResult.getLocation();
            double relativeY = location.y - blockPos.getY();
            blockState = (BlockState)blockState.setValue(BlockStateProperties.SLAB_TYPE, relativeY > 0.5 ? SlabType.BOTTOM : SlabType.TOP);
            BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting();

            try {
               int seq = blockStatePredictionHandler.currentSequence();
               level.setBlock(blockPos, blockState, 19);
               new AxiomServerboundSetBlock(Map.of(blockPos, blockState), false, 2, true, blockHitResult, InteractionHand.MAIN_HAND, seq).send();
            } catch (Throwable var11) {
               if (blockStatePredictionHandler != null) {
                  try {
                     blockStatePredictionHandler.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (blockStatePredictionHandler != null) {
               blockStatePredictionHandler.close();
            }

            BlockState opposite = (BlockState)blockState.setValue(BlockStateProperties.SLAB_TYPE, relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM);
            if (!opposite.isAir()) {
               SoundType soundType = opposite.getSoundType();
               level.playLocalSound(
                  blockPos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false
               );
            }

            level.addDestroyBlockEffect(blockPos, opposite);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean legacyRenderHitOutline(
      PoseStack poseStack, VertexConsumer vertexConsumer, Entity entity, double camX, double camY, double camZ, BlockPos blockPos, BlockState blockState
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return false;
      } else if (!AxiomClient.isAxiomActive() || EditorUI.isActive() || BuilderToolManager.isToolSlotActive()) {
         return false;
      } else if (!Capability.TINKER.isEnabled()) {
         return false;
      } else if (blockState.getBlock() instanceof SlabBlock && blockState.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
         BlockHitResult blockHitResult = (BlockHitResult)Minecraft.getInstance().hitResult;
         Vec3 location = blockHitResult.getLocation();
         double relativeY = location.y - blockPos.getY();
         blockState = (BlockState)blockState.setValue(BlockStateProperties.SLAB_TYPE, relativeY > 0.5 ? SlabType.TOP : SlabType.BOTTOM);
         VoxelShape voxelShape = blockState.getShape(level, blockPos, CollisionContext.of(entity));
         double x = blockPos.getX() - camX;
         double y = blockPos.getY() - camY;
         double z = blockPos.getZ() - camZ;
         Pose pose = poseStack.last();
         voxelShape.forAllEdges(
            (k, l, m, n, o, p) -> {
               float q = (float)(n - k);
               float r = (float)(o - l);
               float s = (float)(p - m);
               float t = Mth.sqrt(q * q + r * r + s * s);
               float var24x;
               float var25;
               float var26;
               vertexConsumer.addVertex(pose.pose(), (float)(k + x), (float)(l + y), (float)(m + z))
                  .setColor(0.0F, 0.0F, 0.0F, 0.4F)
                  .setNormal(pose, var24x = q / t, var25 = r / t, var26 = s / t);
               vertexConsumer.addVertex(pose.pose(), (float)(n + x), (float)(o + y), (float)(p + z))
                  .setColor(0.0F, 0.0F, 0.0F, 0.4F)
                  .setNormal(pose, var24x, var25, var26);
            }
         );
         return true;
      } else {
         return false;
      }
   }

   static {
      for (Entry<Block, Block> entry : AxeItem.STRIPPABLES.entrySet()) {
         BI_STRIPPABLE_MAP.put(entry.getKey(), entry.getValue());
         BI_STRIPPABLE_MAP.put(entry.getValue(), entry.getKey());
      }
   }
}
