package com.moulberry.axiom.capabilities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.AxiomClient;
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
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import com.moulberry.axiom.utils.PositionUtils;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AngelPlacement {
   public static Vec3 lastView = null;
   private static BlockPos lastPos = null;
   private static BlockPos nextPos = null;
   private static int zeroDelayTicks = 0;

   public static void tick() {
      lastPos = nextPos;
      nextPos = null;
      if (Capability.FAST_PLACE.isEnabled()) {
         HitResult hitResult = Minecraft.getInstance().hitResult;
         boolean reset = false;
         if (zeroDelayTicks > 0) {
            zeroDelayTicks--;
            reset = true;
         } else if (hitResult != null && hitResult.getType() == Type.MISS) {
            reset = true;
         }

         if (reset) {
            Minecraft mc = Minecraft.getInstance();
            MinecraftExt ext = (MinecraftExt)mc;
            ext.axiom$setRightClickDelay(0);
         }
      } else {
         zeroDelayTicks = 0;
      }
   }

   public static boolean handlePlace(ClientLevel level, LocalPlayer player, InteractionHand hand, ItemStack itemStack) {
      if (!AxiomClient.isAxiomActive() || EditorUI.isActive() || BuilderToolManager.isToolSlotActive()) {
         return false;
      } else if (!Capability.ANGEL_PLACEMENT.isEnabled()) {
         return false;
      } else {
         double range = 5.0;
         Vec3 viewVec = player.getViewVector(1.0F);
         Vec3 targetVec = player.getEyePosition().add(viewVec.scale(range));
         BlockPos target = BlockPos.containing(targetVec);
         HitResult hitResult = Minecraft.getInstance().hitResult;
         if (hitResult.getType() == Type.ENTITY) {
            return false;
         } else {
            Map<BlockPos, BlockState> changedBlocks = new HashMap<>();
            if (Capability.FAST_PLACE.isEnabled() && lastView != null) {
               for (int i = 0; i < 100; i++) {
                  Vec3 eye = player.getEyePosition(i / 100.0F);
                  Vec3 view = lastView.lerp(player.getViewVector(1.0F), i / 100.0F);
                  Vec3 end = eye.add(view.x * range, view.y * range, view.z * range);
                  BlockHitResult newHitResult = level.clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, player));
                  if (newHitResult.getType() == Type.MISS) {
                     BlockPos targetEnd = BlockPos.containing(end);
                     if (!changedBlocks.containsKey(targetEnd)) {
                        BlockHitResult fakeBlockHitResult = new BlockHitResult(
                           end, PositionUtils.getNearestDirection(view.x, view.y, view.z).getOpposite(), targetEnd, false
                        );
                        ReplaceMode.ReplacingBlockPlaceContext context = new ReplaceMode.ReplacingBlockPlaceContext(player, hand, itemStack, fakeBlockHitResult);
                        BlockState blockState = getStateForPlacement(context);
                        if (blockState != null) {
                           changedBlocks.put(targetEnd, blockState);
                           FastPlace.afterUseItemOn(fakeBlockHitResult, true);
                        }
                     }
                  }
               }
            }

            BlockHitResult blockHitResult;
            if (hitResult.getType() == Type.BLOCK) {
               blockHitResult = (BlockHitResult)hitResult;
            } else {
               blockHitResult = new BlockHitResult(targetVec, PositionUtils.getNearestDirection(viewVec.x, viewVec.y, viewVec.z).getOpposite(), target, false);
            }

            if (hitResult.getType() == Type.MISS && !changedBlocks.containsKey(target)) {
               ReplaceMode.ReplacingBlockPlaceContext context = new ReplaceMode.ReplacingBlockPlaceContext(player, hand, itemStack, blockHitResult);
               BlockState blockState = getStateForPlacement(context);
               if (blockState != null) {
                  changedBlocks.put(target, blockState);
                  FastPlace.afterUseItemOn(blockHitResult, true);
               }
            }

            if (changedBlocks.isEmpty()) {
               return false;
            } else {
               changedBlocks = BuildSymmetry.applySymmetry(changedBlocks);
               boolean updateNeighbors = !Capability.NO_UPDATES.isEnabled();
               BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting();

               try {
                  int seq = blockStatePredictionHandler.currentSequence();

                  for (Entry<BlockPos, BlockState> entry : changedBlocks.entrySet()) {
                     level.setBlock(entry.getKey(), entry.getValue(), 19);
                  }

                  new AxiomServerboundSetBlock(changedBlocks, updateNeighbors, 128, false, blockHitResult, hand, seq).send();
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

               player.swing(hand);
               Minecraft.getInstance().gameRenderer.itemInHandRenderer.itemUsed(hand);
               zeroDelayTicks = 5;
               return true;
            }
         }
      }
   }

   private static BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
      ItemStack itemStack = blockPlaceContext.getItemInHand();
      if (itemStack.isEmpty()) {
         return getVanillaStateForPlacement(blockPlaceContext);
      } else {
         String customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
         if (customBlockPlacer == null) {
            return getVanillaStateForPlacement(blockPlaceContext);
         } else {
            CustomBlock customBlock = ServerCustomBlocks.getCustomBlock(ResourceLocation.parse(customBlockPlacer));
            if (customBlock == null) {
               ChatUtils.error("Unknown custom block: " + customBlockPlacer);
               return getVanillaStateForPlacement(blockPlaceContext);
            } else {
               CustomBlockState placedState = customBlock.getCustomStateForPlacement(blockPlaceContext, Capability.FORCE_PLACE.isEnabled());
               return placedState == null ? null : placedState.getVanillaState();
            }
         }
      }
   }

   private static BlockState getVanillaStateForPlacement(BlockPlaceContext blockPlaceContext) {
      ItemStack itemStack = blockPlaceContext.getItemInHand();
      BlockItem blockItem = (BlockItem)itemStack.getItem();
      BlockState blockState = blockItem.getBlock().getStateForPlacement(blockPlaceContext);
      return blockState == null ? null : ItemStackDataHelper.updateBlockStateFromTag(blockState, itemStack);
   }

   private static <T extends Comparable<T>> BlockState updateStateString(BlockState blockState, Property<T> property, String string) {
      return property.getValue(string).map(comparable -> (BlockState)blockState.setValue(property, comparable)).orElse(blockState);
   }

   public static void render(
      PoseStack poseStack, BufferSource multiBufferSource, double camX, double camY, double camZ, float partial, InteractionHand hand, ItemStack itemStack
   ) {
      if (AxiomClient.isAxiomActive() && !EditorUI.isActive() && !BuilderToolManager.isToolSlotActive()) {
         if (Capability.ANGEL_PLACEMENT.isEnabled()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
               ClientLevel level = Minecraft.getInstance().level;
               if (level != null) {
                  HitResult hitResult = Minecraft.getInstance().hitResult;
                  if (hitResult == null || hitResult.getType() == Type.MISS) {
                     double pickRange = 5.0;
                     Vec3 viewVec = player.getViewVector(partial);
                     Vec3 targetVec = player.getEyePosition().add(viewVec.scale(pickRange));
                     BlockPos target = BlockPos.containing(targetVec);
                     nextPos = target;
                     BlockHitResult blockHitResult = new BlockHitResult(
                        targetVec, PositionUtils.getNearestDirection(viewVec.x, viewVec.y, viewVec.z).getOpposite(), target, false
                     );
                     ReplaceMode.ReplacingBlockPlaceContext context = new ReplaceMode.ReplacingBlockPlaceContext(player, hand, itemStack, blockHitResult);
                     BlockState placeState = getStateForPlacement(context);
                     if (placeState != null) {
                        float x = target.getX();
                        float y = target.getY();
                        float z = target.getZ();
                        if (lastPos != null) {
                           x = Mth.lerp(partial, lastPos.getX(), x);
                           y = Mth.lerp(partial, lastPos.getY(), y);
                           z = Mth.lerp(partial, lastPos.getZ(), z);
                        }

                        poseStack.pushPose();
                        poseStack.translate(x - camX, y - camY, z - camZ);
                        AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.75F);
                        BlockTessellator.SHARED
                           .renderBlock(AxiomRenderPipelines.AXIOM_BLOCK_WITH_OFFSET, placeState, target.immutable(), poseStack, multiBufferSource);
                        AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
                        poseStack.popPose();
                     }
                  }
               }
            }
         }
      }
   }
}
