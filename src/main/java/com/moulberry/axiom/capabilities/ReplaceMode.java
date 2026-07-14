package com.moulberry.axiom.capabilities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.MinecraftExt;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import com.moulberry.axiom.render.BlockTessellator;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.utils.BlockVoxelShapeUtils;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ReplaceMode {
   public static Vec3 lastView = null;
   private static BlockPos hiddenBlock = null;
   private static boolean renderedHitOutlineThisFrame = false;
   private static boolean acquiredChunkRenderOverrider = false;

   public static InteractionResult performUseItemOn(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return InteractionResult.PASS;
      } else {
         if (AxiomClient.isAxiomActive() && Capability.REPLACE_MODE.isEnabled()) {
            ItemStack itemStack = localPlayer.getItemInHand(interactionHand);
            boolean isBucket = itemStack.getItem() == Items.WATER_BUCKET || itemStack.getItem() == Items.LAVA_BUCKET;
            String customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
            CustomBlock customBlock = customBlockPlacer == null ? null : ServerCustomBlocks.getCustomBlock(ResourceLocation.parse(customBlockPlacer));
            if (itemStack.getItem() instanceof BlockItem || isBucket || customBlock != null) {
               float range = (float)localPlayer.blockInteractionRange();
               int infiniteReachLimit = ClientRestrictions.getInfiniteReachLimit();
               if (Capability.INFINITE_REACH.isEnabled() && infiniteReachLimit > range) {
                  range = infiniteReachLimit;
               }

               Map<BlockPos, BlockState> changedBlocks = new HashMap<>();
               InteractionResult highestResult = blockHitResult.getType() == Type.BLOCK ? InteractionResult.CONSUME : InteractionResult.PASS;

               for (int i = 0; i < 100; i++) {
                  Vec3 eye = localPlayer.getEyePosition(i / 100.0F);
                  Vec3 view = lastView.lerp(localPlayer.getViewVector(1.0F), i / 100.0F);
                  BlockHitResult hitResult;
                  if (Capability.INFINITE_REACH.isEnabled() && infiniteReachLimit < 0) {
                     RayCaster.RaycastResult result = RayCaster.raycast(level, eye, view, false, isBucket);
                     if (result == null) {
                        continue;
                     }

                     hitResult = new BlockHitResult(result.getLocation(), result.direction(), result.blockPos(), false);
                  } else {
                     Vec3 end = eye.add(view.x * range, view.y * range, view.z * range);
                     hitResult = level.clip(new ClipContext(eye, end, Block.OUTLINE, isBucket ? Fluid.ANY : Fluid.NONE, localPlayer));
                  }

                  if (hitResult.getType() == Type.BLOCK && !changedBlocks.containsKey(hitResult.getBlockPos())) {
                     ((MinecraftExt)Minecraft.getInstance()).axiom$setRightClickDelay(0);
                     InteractionResult result = performAction(localPlayer, interactionHand, hitResult, level, itemStack, customBlock, changedBlocks);
                     if (highestResult == InteractionResult.PASS) {
                        highestResult = result;
                     } else if (highestResult == InteractionResult.CONSUME && result == InteractionResult.SUCCESS) {
                        highestResult = InteractionResult.SUCCESS;
                     }
                  }
               }

               if (changedBlocks.isEmpty()) {
                  return highestResult;
               }

               changedBlocks = BuildSymmetry.applySymmetry(changedBlocks);
               BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting();

               try {
                  int seq = blockStatePredictionHandler.currentSequence();

                  for (Entry<BlockPos, BlockState> entry : changedBlocks.entrySet()) {
                     level.setBlock(entry.getKey(), entry.getValue(), 19);
                  }

                  new AxiomServerboundSetBlock(changedBlocks, false, 1, false, blockHitResult, interactionHand, seq).send();
               } catch (Throwable var18) {
                  if (blockStatePredictionHandler != null) {
                     try {
                        blockStatePredictionHandler.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (blockStatePredictionHandler != null) {
                  blockStatePredictionHandler.close();
               }

               return highestResult;
            }
         }

         return InteractionResult.PASS;
      }
   }

   private static InteractionResult performAction(
      LocalPlayer localPlayer,
      InteractionHand interactionHand,
      BlockHitResult blockHitResult,
      ClientLevel level,
      ItemStack itemStack,
      @Nullable CustomBlock customBlock,
      Map<BlockPos, BlockState> changedBlocks
   ) {
      BlockPos blockPos = blockHitResult.getBlockPos();
      ReplaceMode.ReplacingBlockPlaceContext context = new ReplaceMode.ReplacingBlockPlaceContext(localPlayer, interactionHand, itemStack, blockHitResult);
      BlockState existingBlock = level.getBlockState(blockHitResult.getBlockPos());
      CustomBlockState customExistingBlock = getCustomState(existingBlock);
      BlockState blockState;
      if (itemStack.getItem() == Items.WATER_BUCKET) {
         blockState = Blocks.WATER.defaultBlockState();
      } else if (itemStack.getItem() == Items.LAVA_BUCKET) {
         blockState = Blocks.LAVA.defaultBlockState();
      } else {
         blockState = getStateForPlacement(context, customBlock, customExistingBlock);
         if (blockState == null) {
            blockState = ((BlockItem)itemStack.getItem()).getBlock().defaultBlockState();

            for (Property<?> property : customExistingBlock.getProperties()) {
               if (blockState.hasProperty(property)) {
                  blockState = copyProperty(customExistingBlock, blockState, property);
               }
            }
         }
      }

      if (blockState == null) {
         return InteractionResult.PASS;
      } else if (blockState == existingBlock) {
         return InteractionResult.CONSUME;
      } else {
         SoundType soundType = blockState.getSoundType();
         level.playSound(
            localPlayer, blockPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F
         );
         level.addDestroyBlockEffect(blockPos, existingBlock);
         changedBlocks.put(blockPos, blockState);
         return InteractionResult.SUCCESS;
      }
   }

   private static CustomBlockState getCustomState(BlockState blockState) {
      CustomBlockState custom = ServerCustomBlocks.getCustomStateFor(blockState);
      return custom != null ? custom : (CustomBlockState)blockState;
   }

   private static BlockState getStateForPlacement(
      BlockPlaceContext blockPlaceContext, @Nullable CustomBlock customPlacingBlock, CustomBlockState existingBlockState
   ) {
      if (customPlacingBlock != null) {
         CustomBlockState placedState = customPlacingBlock.getCustomStateForPlacement(blockPlaceContext, Capability.FORCE_PLACE.isEnabled());
         if (placedState == null) {
            return null;
         } else {
            for (Property<?> property : existingBlockState.getProperties()) {
               if (placedState.axiomHasProperty(property)) {
                  placedState = placedState.setPropertyUnsafe(property, existingBlockState.getProperty((Property)property));
               }
            }

            return placedState.getVanillaState();
         }
      } else {
         ItemStack itemStack = blockPlaceContext.getItemInHand();
         if (itemStack.isEmpty()) {
            return getVanillaStateForPlacement(blockPlaceContext, existingBlockState);
         } else {
            boolean typeReplace = Axiom.configuration.capabilities.typeReplace;
            net.minecraft.world.level.block.Block existingBlock = existingBlockState.getVanillaState().getBlock();
            FamilyMap.AxiomBlockVariant existingVariant = FamilyMap.getVariantFor(existingBlock);
            if (existingVariant != null) {
               net.minecraft.world.level.block.Block block = ((BlockItem)itemStack.getItem()).getBlock();
               FamilyMap.AxiomBlockFamily family = FamilyMap.getFamilyFor(block);
               FamilyMap.AxiomBlockVariant heldVariant = FamilyMap.getVariantFor(block);
               boolean doTypeReplace = heldVariant == null && typeReplace;
               if (family != null) {
                  net.minecraft.world.level.block.Block newBlock = doTypeReplace ? family.getVariant(existingVariant) : null;
                  if (newBlock != null) {
                     BlockState newState = newBlock.defaultBlockState();

                     for (Property<?> propertyx : existingBlockState.getProperties()) {
                        if (newState.hasProperty(propertyx)) {
                           newState = copyProperty(existingBlockState, newState, propertyx);
                        }
                     }

                     return newState;
                  }

                  Map<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> converters = FamilyMap.getVariantConverters(existingVariant);
                  if (converters != null) {
                     for (Entry<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> entry : converters.entrySet()) {
                        newBlock = !doTypeReplace && heldVariant != entry.getKey() ? null : family.getVariant(entry.getKey());
                        if (newBlock != null) {
                           BlockState newState = newBlock.defaultBlockState();

                           for (Property<?> propertyxx : existingBlockState.getProperties()) {
                              if (newState.hasProperty(propertyxx)) {
                                 newState = copyProperty(existingBlockState, newState, propertyxx);
                              }
                           }

                           return entry.getValue()
                              .convert(existingBlockState.getVanillaState(), newState, blockPlaceContext.getClickedPos(), blockPlaceContext.getLevel());
                        }
                     }
                  }
               }
            }

            return getVanillaStateForPlacement(blockPlaceContext, existingBlockState);
         }
      }
   }

   private static BlockState getVanillaStateForPlacement(BlockPlaceContext blockPlaceContext, CustomBlockState existingBlock) {
      ItemStack itemStack = blockPlaceContext.getItemInHand();
      BlockItem blockItem = (BlockItem)itemStack.getItem();
      BlockState blockState = blockItem.getBlock().getStateForPlacement(blockPlaceContext);
      if (blockState == null) {
         return null;
      } else {
         for (Property<?> property : existingBlock.getProperties()) {
            if (blockState.hasProperty(property)) {
               blockState = copyProperty(existingBlock, blockState, property);
            }
         }

         return ItemStackDataHelper.updateBlockStateFromTag(blockState, itemStack);
      }
   }

   public static ReplaceMode.ReplaceModeRenderState extractRenderState(Level level, Entity entity, BlockState existingState, BlockPos blockPos) {
      if (level == null) {
         return null;
      } else if (!AxiomClient.isAxiomActive() || EditorUI.isActive() || BuilderToolManager.isToolSlotActive()) {
         return null;
      } else if (AxiomClient.isAxiomActive() && Capability.REPLACE_MODE.isEnabled()) {
         String customBlockPlacer = null;
         CustomBlock customBlock = null;
         if (entity instanceof LocalPlayer localPlayer) {
            ItemStack itemStack = localPlayer.getItemInHand(InteractionHand.MAIN_HAND);
            customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
            customBlock = customBlockPlacer == null ? null : ServerCustomBlocks.getCustomBlock(ResourceLocation.parse(customBlockPlacer));
            InteractionHand interactionHand;
            if (!(itemStack.getItem() instanceof BlockItem) && customBlock == null) {
               itemStack = localPlayer.getItemInHand(InteractionHand.OFF_HAND);
               customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
               customBlock = customBlockPlacer == null ? null : ServerCustomBlocks.getCustomBlock(ResourceLocation.parse(customBlockPlacer));
               if (!(itemStack.getItem() instanceof BlockItem) && customBlock == null) {
                  return null;
               }

               interactionHand = InteractionHand.OFF_HAND;
            } else {
               interactionHand = InteractionHand.MAIN_HAND;
            }

            ReplaceMode.ReplacingBlockPlaceContext context = new ReplaceMode.ReplacingBlockPlaceContext(
               localPlayer, interactionHand, itemStack, (BlockHitResult)Minecraft.getInstance().hitResult
            );
            CustomBlockState customExistingState = getCustomState(existingState);
            BlockState placeState = getStateForPlacement(context, customBlock, customExistingState);
            if (placeState == null) {
               placeState = ((BlockItem)itemStack.getItem()).getBlock().defaultBlockState();

               for (Property<?> property : customExistingState.getProperties()) {
                  if (placeState.hasProperty(property)) {
                     placeState = copyProperty(customExistingState, placeState, property);
                  }
               }
            }

            if (!acquiredChunkRenderOverrider) {
               acquiredChunkRenderOverrider = true;
               ChunkRenderOverrider.acquire("replace-mode");
            }

            VoxelShape shape1 = placeState.getVisualShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
            VoxelShape shape2 = existingState.getVisualShape(level, blockPos, CollisionContext.empty());
            float opacity;
            if (BlockVoxelShapeUtils.firstCompletelyOverlapsSecond(shape1, shape2)) {
               if (hiddenBlock != null) {
                  hiddenBlock = null;
                  ChunkRenderOverrider.clear();
               }

               opacity = 0.5F;
            } else {
               if (!blockPos.equals(hiddenBlock)) {
                  hiddenBlock = blockPos.immutable();
                  ChunkRenderOverrider.clear();
                  ChunkRenderOverrider.setBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), Blocks.AIR.defaultBlockState());
               }

               opacity = 0.75F;
            }

            renderedHitOutlineThisFrame = true;
            return new ReplaceMode.ReplaceModeRenderState(placeState, blockPos, opacity);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void renderHitOutline(
      PoseStack poseStack, BufferSource multiBufferSource, double camX, double camY, double camZ, ReplaceMode.ReplaceModeRenderState renderState
   ) {
      BlockState placeState = renderState.placeState;
      BlockPos blockPos = renderState.blockPos;
      float opacity = renderState.opacity;
      poseStack.pushPose();
      poseStack.translate(blockPos.getX() - camX, blockPos.getY() - camY, blockPos.getZ() - camZ);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, opacity);
      BlockTessellator.SHARED.renderBlock(AxiomRenderPipelines.AXIOM_BLOCK_WITH_OFFSET, placeState, blockPos, poseStack, multiBufferSource);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      poseStack.popPose();
   }

   public static <T extends Comparable<T>> BlockState copyProperty(CustomBlockState blockState, BlockState blockState2, Property<T> property) {
      return (BlockState)blockState2.setValue(property, blockState.getProperty(property));
   }

   public static void renderTick() {
      if (!renderedHitOutlineThisFrame) {
         releaseChunkRenderOverrider();
      }

      renderedHitOutlineThisFrame = false;
   }

   public static void releaseChunkRenderOverrider() {
      if (acquiredChunkRenderOverrider) {
         acquiredChunkRenderOverrider = false;
         hiddenBlock = null;
         ChunkRenderOverrider.release("replace-mode");
      }
   }

   public record ReplaceModeRenderState(BlockState placeState, BlockPos blockPos, float opacity) {
   }

   public static class ReplacingBlockPlaceContext extends BlockPlaceContext {
      public ReplacingBlockPlaceContext(Player player, InteractionHand interactionHand, ItemStack itemStack, BlockHitResult blockHitResult) {
         super(player, interactionHand, itemStack, blockHitResult);
         this.replaceClicked = true;
      }
   }
}
