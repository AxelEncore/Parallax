package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public class SpecialPlace {
   private static boolean usedForcePlace = false;
   private static boolean usedCustomShapeUpdate = false;
   private static boolean usedCustomPlacement = false;
   private static boolean usedSkipInteraction = false;
   private static boolean isCollectingBlockChanges = false;
   private static boolean isDoingUseWithoutItemChanges = false;
   private static boolean hadPredictedBlockChanges = false;
   private static final Map<BlockPos, BlockState> collectedBlockChanges = new LinkedHashMap<>();
   private static final Map<BlockPos, BlockState> collectedBlockChangesUseWithoutItem = new LinkedHashMap<>();
   private static final Set<BlockPos> preventUpdatesAt = new HashSet<>();

   private static void reset() {
      usedForcePlace = false;
      usedCustomShapeUpdate = false;
      usedCustomPlacement = false;
      usedSkipInteraction = false;
      isCollectingBlockChanges = false;
      isDoingUseWithoutItemChanges = false;
      hadPredictedBlockChanges = false;
      collectedBlockChanges.clear();
      collectedBlockChangesUseWithoutItem.clear();
      preventUpdatesAt.clear();
   }

   public static InteractionResult performUseItemOn(
      LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, SpecialPlace.NormalUseItemOn normalUseItemOn
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return InteractionResult.PASS;
      } else if (!AxiomClient.isAxiomActive()) {
         return InteractionResult.PASS;
      } else if (!AxiomClient.hasPermission(AxiomPermission.BUILD_PLACE)) {
         return InteractionResult.PASS;
      } else {
         boolean forceBecauseOfInfiniteReach = false;
         if (Capability.INFINITE_REACH.isEnabled()) {
            double hitDistance = blockHitResult.getLocation().distanceTo(localPlayer.getEyePosition());
            forceBecauseOfInfiniteReach = hitDistance > localPlayer.blockInteractionRange();
         }

         boolean noUpdates = Capability.NO_UPDATES.isEnabled();
         boolean forcePlace = Capability.FORCE_PLACE.isEnabled();
         boolean forceBecauseOfSymmetry = BuildSymmetry.isActive();
         BlockState placingOnBlockState = level.getBlockState(blockHitResult.getBlockPos());
         ItemStack itemStackInHand = localPlayer.getItemInHand(interactionHand);
         Item itemInHand = itemStackInHand.getItem();
         boolean forceBecauseOfLiquidPlace = false;
         boolean replaceClickedBlockState = placingOnBlockState.canBeReplaced()
            && (itemStackInHand.isEmpty() || !itemStackInHand.is(placingOnBlockState.getBlock().asItem()));
         if (Axiom.configuration.blockAttributes.makeFluidHitboxesSolid && placingOnBlockState.getBlock() instanceof LiquidBlock) {
            forceBecauseOfLiquidPlace = true;
            replaceClickedBlockState = false;
         }

         BlockPos clickedPos = null;
         if (replaceClickedBlockState) {
            clickedPos = blockHitResult.getBlockPos();
         } else if (level.getBlockState(blockHitResult.getBlockPos().relative(blockHitResult.getDirection())).canBeReplaced()) {
            clickedPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
         }

         if (!noUpdates && !forceBecauseOfInfiniteReach && !forceBecauseOfLiquidPlace || itemInHand != Items.WATER_BUCKET && itemInHand != Items.LAVA_BUCKET) {
            if (!noUpdates
               && !forceBecauseOfSymmetry
               && !forcePlace
               && !forceBecauseOfLiquidPlace
               && !forceBecauseOfInfiniteReach
               && !Axiom.configuration.blockAttributes.preventInteractions
               && ServerCustomBlocks.customBlockMap.isEmpty()) {
               return InteractionResult.PASS;
            } else {
               if (forceBecauseOfLiquidPlace) {
                  if (clickedPos == null) {
                     return InteractionResult.FAIL;
                  }

                  Direction dir = blockHitResult.getDirection();
                  blockHitResult = new BlockHitResult(Vec3.atCenterOf(clickedPos), dir, clickedPos, true);
               }

               if (isCollectingBlockChanges) {
                  throw new FaultyImplementationError("Already collecting block changes - was something called async?");
               } else {
                  reset();
                  BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting();

                  InteractionResult normalResult;
                  int sequence;
                  try {
                     isCollectingBlockChanges = true;
                     normalResult = performUseItemOn(
                        localPlayer, interactionHand, blockHitResult, normalUseItemOn, forcePlace, itemStackInHand, clickedPos, level
                     );
                     if (isDoingUseWithoutItemChanges) {
                        Axiom.LOGGER.warn("isDoingUseWithoutItemChanges was true after processing. Bug with MixinBlockStateBase#useWithoutItemXYZ?");
                        finishDoingUseWithoutItem(level);
                     }

                     isCollectingBlockChanges = false;
                     sequence = blockStatePredictionHandler.currentSequence();
                  } catch (Throwable var21) {
                     if (blockStatePredictionHandler != null) {
                        try {
                           blockStatePredictionHandler.close();
                        } catch (Throwable var20) {
                           var21.addSuppressed(var20);
                        }
                     }

                     throw var21;
                  }

                  if (blockStatePredictionHandler != null) {
                     blockStatePredictionHandler.close();
                  }

                  if (!normalResult.consumesAction()) {
                     if (normalResult == InteractionResult.PASS
                        && (
                           usedSkipInteraction
                              || usedCustomShapeUpdate
                              || usedCustomPlacement
                              || ItemStackDataHelper.getCustomBlockPlacer(itemStackInHand) != null
                        )) {
                        normalResult = InteractionResult.CONSUME;
                     }

                     if (hadPredictedBlockChanges) {
                        level.getBlockStatePredictionHandler().endPredictionsUpTo(sequence, level);
                     }

                     reset();
                     return normalResult;
                  } else if (!usedForcePlace
                     && !usedCustomShapeUpdate
                     && !usedCustomPlacement
                     && !usedSkipInteraction
                     && (!noUpdates || collectedBlockChanges.isEmpty())
                     && !forceBecauseOfInfiniteReach
                     && !forceBecauseOfSymmetry
                     && !forceBecauseOfLiquidPlace) {
                     reset();
                     Minecraft.getInstance().getConnection().send(new ServerboundUseItemOnPacket(interactionHand, blockHitResult, sequence));
                     return normalResult;
                  } else if (!collectedBlockChanges.isEmpty()) {
                     int reasons = calculateReason(noUpdates, forceBecauseOfInfiniteReach, forceBecauseOfSymmetry, forceBecauseOfLiquidPlace);
                     sendCollectedBlockChanges(reasons, false, blockHitResult, interactionHand, sequence);
                     reset();
                     return normalResult;
                  } else {
                     if (hadPredictedBlockChanges) {
                        level.getBlockStatePredictionHandler().endPredictionsUpTo(sequence, level);
                     }

                     return InteractionResult.CONSUME;
                  }
               }
            }
         } else if (clickedPos == null) {
            return InteractionResult.FAIL;
         } else {
            int reasons = 0;
            if (noUpdates) {
               reasons |= 8;
            }

            if (forceBecauseOfInfiniteReach) {
               reasons |= 64;
            }

            if (forceBecauseOfLiquidPlace) {
               reasons |= 512;
            }

            Block placedBlock = itemInHand == Items.WATER_BUCKET ? Blocks.WATER : Blocks.LAVA;
            Map<BlockPos, BlockState> changes = Map.of(clickedPos, placedBlock.defaultBlockState());
            new AxiomServerboundSetBlock(changes, !noUpdates, reasons, false, blockHitResult, interactionHand, -1).send();
            localPlayer.swing(interactionHand);
            return InteractionResult.CONSUME;
         }
      }
   }

   private static int calculateReason(boolean noUpdates, boolean forceBecauseOfInfiniteReach, boolean forceBecauseOfSymmetry, boolean forceBecauseOfLiquidPlace) {
      int reasons = 0;
      if (usedForcePlace) {
         reasons |= 4;
      }

      if (usedCustomShapeUpdate) {
         reasons |= 16;
      }

      if (usedCustomPlacement) {
         reasons |= 32;
      }

      if (noUpdates) {
         reasons |= 8;
      }

      if (forceBecauseOfInfiniteReach) {
         reasons |= 64;
      }

      if (forceBecauseOfSymmetry) {
         reasons |= 256;
      }

      if (forceBecauseOfLiquidPlace) {
         reasons |= 512;
      }

      if (usedSkipInteraction) {
         reasons |= 1024;
      }

      return reasons;
   }

   private static InteractionResult performUseItemOn(
      LocalPlayer localPlayer,
      InteractionHand interactionHand,
      BlockHitResult blockHitResult,
      SpecialPlace.NormalUseItemOn normalUseItemOn,
      boolean forcePlace,
      ItemStack itemStack,
      BlockPos clickedPos,
      ClientLevel level
   ) {
      if (forcePlace && itemStack.getItem() instanceof BlockItem blockItem && ItemStackDataHelper.hasBlockStateTag(itemStack)) {
         if (clickedPos == null) {
            return InteractionResult.FAIL;
         } else {
            usedForcePlace = true;
            BlockState blockState = blockItem.getBlock().defaultBlockState();
            blockState = ItemStackDataHelper.updateBlockStateFromTag(blockState, itemStack);
            SoundType soundType = blockState.getSoundType();
            level.playLocalSound(
               clickedPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false
            );
            level.setBlock(clickedPos, blockState, 2);
            localPlayer.swing(interactionHand);
            return InteractionResult.CONSUME;
         }
      } else {
         if (!(itemStack.getItem() instanceof BlockItem)) {
            String customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
            if (customBlockPlacer != null) {
               if (clickedPos == null) {
                  return InteractionResult.FAIL;
               }

               usedCustomPlacement = true;
               BlockPlaceContext blockPlaceContext = new BlockPlaceContext(new UseOnContext(localPlayer, interactionHand, blockHitResult));
               BlockState placed = doCustomPlacement(customBlockPlacer, blockPlaceContext);
               if (placed == null) {
                  return InteractionResult.FAIL;
               }

               blockPlaceContext.getLevel().setBlock(clickedPos, placed, 11);
               SoundType soundType = placed.getSoundType();
               level.playLocalSound(
                  clickedPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false
               );
               localPlayer.swing(interactionHand);
               return InteractionResult.CONSUME;
            }
         }

         return normalUseItemOn.performUseItemOn(localPlayer, interactionHand, blockHitResult);
      }
   }

   @Nullable
   private static BlockState doCustomPlacement(String customBlockPlacer, BlockPlaceContext blockPlaceContext) {
      CustomBlock customBlock = ServerCustomBlocks.getCustomBlock(ResourceLocation.parse(customBlockPlacer));
      if (customBlock == null) {
         ChatUtils.error("Unknown custom block: " + customBlockPlacer);
         return null;
      } else {
         CustomBlockState placedState = customBlock.getCustomStateForPlacement(blockPlaceContext, Capability.FORCE_PLACE.isEnabled());
         BlockState vanillaState = placedState == null ? null : placedState.getVanillaState();
         if (vanillaState == null) {
            return null;
         } else {
            if (!Capability.FORCE_PLACE.isEnabled()) {
               CollisionContext collisionContext = blockPlaceContext.getPlayer() == null
                  ? CollisionContext.empty()
                  : CollisionContext.of(blockPlaceContext.getPlayer());
               if (!blockPlaceContext.getLevel().isUnobstructed(vanillaState, blockPlaceContext.getClickedPos(), collisionContext)) {
                  return null;
               }
            }

            return vanillaState;
         }
      }
   }

   public static boolean destroyBlock(ClientLevel level, BlockPos blockPos, Direction direction, SpecialPlace.NormalDestroyBlock normalDestroyBlock) {
      if (!AxiomClient.isAxiomActive()) {
         return false;
      } else {
         BlockState destroyedBlockState = level.getBlockState(blockPos);
         if (destroyedBlockState.getBlock() instanceof LiquidBlock) {
            reset();
            collectedBlockChanges.put(blockPos, Blocks.AIR.defaultBlockState());
            BlockHitResult blockHitResult = (BlockHitResult)Minecraft.getInstance().hitResult;
            sendCollectedBlockChanges(512, true, blockHitResult, InteractionHand.MAIN_HAND, -1);
            level.addDestroyBlockEffect(blockPos, destroyedBlockState);
            reset();
            return true;
         } else {
            boolean forceBecauseOfInfiniteReach = false;
            if (Capability.INFINITE_REACH.isEnabled()) {
               HitResult hitResult = Minecraft.getInstance().hitResult;
               LocalPlayer localPlayer = Minecraft.getInstance().player;
               double hitDistance = hitResult.getLocation().distanceTo(localPlayer.getEyePosition());
               forceBecauseOfInfiniteReach = hitDistance > localPlayer.blockInteractionRange();
            }

            boolean noUpdates = Capability.NO_UPDATES.isEnabled();
            boolean forceBecauseOfSymmetry = BuildSymmetry.isActive();
            if (!noUpdates && !forceBecauseOfSymmetry && !forceBecauseOfInfiniteReach && ServerCustomBlocks.customBlockMap.isEmpty()) {
               return false;
            } else if (isCollectingBlockChanges) {
               throw new FaultyImplementationError("Already collecting block changes - was something called async?");
            } else {
               reset();
               BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting();

               int sequence;
               boolean normalResult;
               try {
                  isCollectingBlockChanges = true;
                  normalResult = normalDestroyBlock.destroyBlock(blockPos);
                  isCollectingBlockChanges = false;
                  sequence = blockStatePredictionHandler.currentSequence();
               } catch (Throwable var14) {
                  if (blockStatePredictionHandler != null) {
                     try {
                        blockStatePredictionHandler.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (blockStatePredictionHandler != null) {
                  blockStatePredictionHandler.close();
               }

               if (!normalResult) {
                  if (usedSkipInteraction || usedCustomShapeUpdate || usedCustomPlacement) {
                     normalResult = true;
                  }

                  if (hadPredictedBlockChanges) {
                     level.getBlockStatePredictionHandler().endPredictionsUpTo(sequence, level);
                  }

                  reset();
                  return normalResult;
               } else {
                  collectedBlockChanges.entrySet()
                     .removeIf(entry -> entry.getKey().equals(blockPos) ? false : !entry.getValue().isAir() && !entry.getValue().liquid());
                  if (!usedCustomShapeUpdate && (!noUpdates || collectedBlockChanges.isEmpty()) && !forceBecauseOfInfiniteReach && !forceBecauseOfSymmetry) {
                     reset();
                     Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, direction, sequence));
                     return true;
                  } else if (!collectedBlockChanges.isEmpty()) {
                     BlockHitResult blockHitResult = (BlockHitResult)Minecraft.getInstance().hitResult;
                     int reasons = calculateReason(noUpdates, forceBecauseOfInfiniteReach, forceBecauseOfSymmetry, false);
                     sendCollectedBlockChanges(reasons, true, blockHitResult, InteractionHand.MAIN_HAND, sequence);
                     reset();
                     return true;
                  } else {
                     if (hadPredictedBlockChanges) {
                        level.getBlockStatePredictionHandler().endPredictionsUpTo(sequence, level);
                     }

                     reset();
                     return true;
                  }
               }
            }
         }
      }
   }

   private static void sendCollectedBlockChanges(int reasons, boolean breaking, BlockHitResult blockHitResult, InteractionHand hand, int sequence) {
      Map<BlockPos, BlockState> changes = BuildSymmetry.applySymmetry(collectedBlockChanges);
      boolean updateNeighbors = !Capability.NO_UPDATES.isEnabled();
      new AxiomServerboundSetBlock(changes, updateNeighbors, preventUpdatesAt, reasons, breaking, blockHitResult, hand, sequence).send();
   }

   public static boolean isSpecialPlacing(BlockGetter blockGetter) {
      return AxiomClient.isAxiomActive() && isCollectingBlockChanges && blockGetter instanceof ClientLevel;
   }

   public static boolean isForcePlacing(BlockGetter blockGetter) {
      if (isSpecialPlacing(blockGetter) && Capability.FORCE_PLACE.isEnabled()) {
         usedForcePlace = true;
         return true;
      } else {
         return false;
      }
   }

   public static boolean isCancellingUpdates(BlockGetter blockGetter) {
      return isSpecialPlacing(blockGetter) && Capability.NO_UPDATES.isEnabled();
   }

   public static void markCustomShapeUpdated(BlockGetter blockGetter, BlockPos blockPos) {
      if (isSpecialPlacing(blockGetter)) {
         usedCustomShapeUpdate = true;
         preventUpdatesAt.add(blockPos);
      }
   }

   public static void markCustomPlacement(BlockGetter blockGetter) {
      if (isSpecialPlacing(blockGetter)) {
         usedCustomPlacement = true;
      }
   }

   public static void markPreventedDefaultInteraction(BlockGetter blockGetter) {
      if (isSpecialPlacing(blockGetter)) {
         usedSkipInteraction = true;
      }

      collectedBlockChangesUseWithoutItem.clear();
   }

   public static boolean isUseWithoutItemCancellable(BlockGetter blockGetter) {
      return isSpecialPlacing(blockGetter) && (Axiom.configuration.blockAttributes.preventInteractions || !ServerCustomBlocks.customBlockMap.isEmpty());
   }

   public static boolean isDoingUseWithoutItemChanges(BlockGetter blockGetter) {
      return isDoingUseWithoutItemChanges && isSpecialPlacing(blockGetter);
   }

   public static void startDoingUseWithoutItem() {
      if (isDoingUseWithoutItemChanges) {
         Axiom.LOGGER
            .warn("startDoingUseWithoutItem() called when isDoingUseWithoutItemChanges was already true. Bug with MixinBlockStateBase#useWithoutItemXYZ?");
      }

      isDoingUseWithoutItemChanges = true;
   }

   public static void finishDoingUseWithoutItem(Level level) {
      if (isDoingUseWithoutItemChanges) {
         isDoingUseWithoutItemChanges = false;

         for (Entry<BlockPos, BlockState> entry : collectedBlockChangesUseWithoutItem.entrySet()) {
            tryCollectBlockChange(level, entry.getKey(), entry.getValue());
         }

         collectedBlockChangesUseWithoutItem.clear();
      }
   }

   public static void tryCollectBlockChange(Level level, BlockPos blockPos, BlockState blockState) {
      if (isCollectingBlockChanges) {
         if (!level.isOutsideBuildHeight(blockPos)) {
            if (level.getBlockState(blockPos) != blockState) {
               hadPredictedBlockChanges = true;
               if (isDoingUseWithoutItemChanges) {
                  collectedBlockChangesUseWithoutItem.put(blockPos, blockState);
               } else {
                  collectedBlockChanges.put(blockPos, blockState);
               }
            }
         }
      }
   }

   public interface NormalDestroyBlock {
      boolean destroyBlock(BlockPos var1);
   }

   public interface NormalUseItemOn {
      InteractionResult performUseItemOn(LocalPlayer var1, InteractionHand var2, BlockHitResult var3);
   }
}
