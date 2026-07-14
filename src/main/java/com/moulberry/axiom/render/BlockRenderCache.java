package com.moulberry.axiom.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.hooks.LevelRendererExt;
import com.moulberry.axiom.hooks.MinecraftExt;
import com.moulberry.axiom.utils.FramebufferUtils;
import com.moulberry.axiom.utils.ProjectionMatrixBackup;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public class BlockRenderCache {
   private static final int MIN_CAPACITY = 32;
   private static final int MIN_AGE = 16;
   private static final int INFLOW_PER_TICK = 8;
   private static final int OUTFLOW_PER_TICK = 8;
   private static final List<TextureTarget> textureIdPool = new ArrayList<>();
   private static final Map<BlockRenderCache.SizedBlock, BlockRenderCache.RenderSlot> blockToRenderSlot = new HashMap<>();
   private static BlockRenderCache.RenderSlot head;
   private static BlockRenderCache.RenderSlot tail;
   private static long currentTick = 0L;
   private static long nextTick = 0L;
   private static final Set<BlockRenderCache.SizedBlock> desired = new HashSet<>();
   private static final Set<BlockRenderCache.SizedBlock> desiredPriority = new HashSet<>();

   public static int request(CustomBlockState blockState, int width, int height, boolean priority) {
      AxiomGpuTexture texture = requestTexture(blockState, width, height, priority);
      return texture == null ? -1 : texture.glId();
   }

   public static AxiomGpuTexture requestTexture(CustomBlockState blockState, int width, int height, boolean priority) {
      BlockRenderCache.SizedBlock sizedBlock = new BlockRenderCache.SizedBlock(blockState.getVanillaState(), !(blockState instanceof BlockState), width, height);
      BlockRenderCache.RenderSlot slot = blockToRenderSlot.get(sizedBlock);
      if (slot == null) {
         if (priority) {
            if (desiredPriority.size() >= 8) {
               return null;
            }

            desiredPriority.add(sizedBlock);
            if (desiredPriority.size() + desired.size() > 8) {
               Iterator<BlockRenderCache.SizedBlock> iterator = desired.iterator();
               iterator.next();
               iterator.remove();
            }
         } else {
            if (desiredPriority.size() + desired.size() >= 8) {
               return null;
            }

            desired.add(sizedBlock);
         }

         return null;
      } else {
         slot.lastRequestedTick = currentTick;
         if (slot != head) {
            slot.unlink();
            slot.next = head;
            head.previous = slot;
            head = slot;
         }

         return new AxiomGpuTexture(slot.textureId.getColorTextureId());
      }
   }

   public static void tick() {
      nextTick++;
   }

   public static void renderTick(GuiGraphics guiGraphics) {
      if (currentTick != nextTick) {
         currentTick = nextTick;
         RenderSystem.assertOnRenderThread();
         int oldReadFbo = GL11.glGetInteger(36010);
         int oldDrawFbo = GL11.glGetInteger(36006);
         desired.forEach(sizedBlock -> renderSizedBlock(sizedBlock, guiGraphics));
         desiredPriority.forEach(sizedBlock -> renderSizedBlock(sizedBlock, guiGraphics));
         desired.clear();
         desiredPriority.clear();
         int outflow = 0;

         while (outflow < 8 && blockToRenderSlot.size() > 32) {
            BlockRenderCache.RenderSlot toRemove = tail;
            long age = currentTick - toRemove.lastRequestedTick;
            if (age >= -10L && age <= 16L) {
               break;
            }

            toRemove.unlink();
            BlockRenderCache.RenderSlot removedSlot = blockToRenderSlot.remove(toRemove.key);
            if (removedSlot != toRemove) {
               throw new FaultyImplementationError();
            }

            textureIdPool.add(toRemove.textureId);
            outflow++;
            int textureIdPoolSize = textureIdPool.size();
            if (textureIdPoolSize > Math.max(32, blockToRenderSlot.size())) {
               for (int i = textureIdPoolSize - 1; i >= textureIdPoolSize / 2; i--) {
                  TextureTarget id = textureIdPool.remove(i);
                  id.destroyBuffers();
               }
            }
         }

         GlStateManager._glBindFramebuffer(36008, oldReadFbo);
         GlStateManager._glBindFramebuffer(36009, oldDrawFbo);
      }
   }

   private static void renderItem(GuiGraphics guiGraphics, ItemStack itemStack, BufferSource multiBufferSource) {
      guiGraphics.renderFakeItem(itemStack, 0, 0);
   }

   private static void renderSizedBlock(BlockRenderCache.SizedBlock sizedBlock, GuiGraphics guiGraphics) {
      TextureTarget target;
      if (textureIdPool.isEmpty()) {
         int w = sizedBlock.width;
         int h = sizedBlock.height;
         target = VersionUtilsClient.helperCreateNewTextureTarget(null, w, h, true);
      } else {
         target = textureIdPool.remove(textureIdPool.size() - 1);
      }

      BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
      bufferSource.endBatch();
      RenderHelper.tryFlush(guiGraphics);
      if (target.width != sizedBlock.width || target.height != sizedBlock.height) {
         int w = sizedBlock.width;
         int h = sizedBlock.height;
         target.resize(w, h, Minecraft.ON_OSX);
      }

      FramebufferUtils.clear(target, 0);
      ((MinecraftExt)Minecraft.getInstance()).axiom$pushMainRenderTarget(target);
      ((LevelRendererExt)Minecraft.getInstance().levelRenderer).axiom$pushTranslucentRenderTarget(target);
      target.bindWrite(true);
      ItemStack itemStack = new ItemStack(sizedBlock.state.getBlock().asItem());
      ProjectionMatrixBackup projectionMatrixBackup = ProjectionMatrixBackup.create();
      RenderHelper.setOrthoProjectionMatrix(sizedBlock.width, sizedBlock.height);
      Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
      RenderHelper.pushModelViewStackWithIdentity();
      modelViewStack.translate(0.0F, 0.0F, -2000.0F);
      modelViewStack.scale(sizedBlock.width / 16.0F, sizedBlock.height / 16.0F, 1.0F);
      RenderHelper.tryApplyModelViewMatrix();
      if (!sizedBlock.forceAsBlock && shouldRenderAsItem(sizedBlock.state, itemStack)) {
         renderItem(guiGraphics, itemStack, bufferSource);
      } else {
         try {
            renderAsBlock(sizedBlock, bufferSource, modelViewStack);
         } catch (Exception var8) {
            renderItem(guiGraphics, itemStack, bufferSource);
         }
      }

      bufferSource.endBatch();
      RenderHelper.tryFlush(guiGraphics);
      RenderHelper.popModelViewStack();
      projectionMatrixBackup.restore();
      ((MinecraftExt)Minecraft.getInstance()).axiom$popMainRenderTarget();
      ((LevelRendererExt)Minecraft.getInstance().levelRenderer).axiom$popTranslucentRenderTarget();
      Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
      BlockRenderCache.RenderSlot slot = new BlockRenderCache.RenderSlot(sizedBlock, target, currentTick);
      if (head == null) {
         head = slot;
         tail = slot;
      } else {
         head.previous = slot;
         slot.next = head;
         head = slot;
      }

      blockToRenderSlot.put(sizedBlock, slot);
   }

   private static void renderAsBlock(BlockRenderCache.SizedBlock sizedBlock, BufferSource bufferSource, Matrix4fStack modelViewStack) {
      BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(sizedBlock.state);
      Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
      AxiomRenderer.setMainTexture(TextureAtlas.LOCATION_BLOCKS);
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.defaultBlendFunc();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      modelViewStack.translate(0.0F, 0.0F, 100.0F);
      modelViewStack.translate(8.0F, 8.0F, 0.0F);
      modelViewStack.scale(16.0F, 16.0F, 16.0F);
      RenderHelper.tryApplyModelViewMatrix();
      PoseStack poseStack2 = new PoseStack();
      poseStack2.scale(1.0F, -1.0F, 1.0F);
      boolean useFlatLight = !model.usesBlockLight();
      if (useFlatLight) {
         Lighting.setupForFlatItems();
      }

      ItemTransform transform = getTransformsForBlock(sizedBlock.state);
      if (transform != ItemTransform.NO_TRANSFORM) {
         transform.apply(false, poseStack2);
      } else {
         poseStack2.mulPose(new Quaternionf().rotationXYZ((float)Math.toRadians(30.0), (float)Math.toRadians(225.0), 0.0F));
         poseStack2.scale(0.625F, 0.625F, 0.625F);
      }

      if (sizedBlock.state.getBlock() instanceof StairBlock) {
         poseStack2.mulPose(new Quaternionf().rotateY((float) (-Math.PI / 2)));
      }

      poseStack2.translate(-0.5F, -0.5F, -0.5F);
      if (!shouldShade(model, sizedBlock.state)) {
         RenderHelper.setupFlatLighting();
      } else {
         RenderHelper.setup3DLighting();
      }

      BlockTessellator.SHARED.renderBlock(AxiomRenderPipelines.AXIOM_BLOCK_NO_MIPMAP, sizedBlock.state, BlockPos.ZERO, poseStack2, bufferSource);
      bufferSource.endBatch();
      RenderHelper.setup3DLighting();
      RenderSystem.enableDepthTest();
   }

   public static ItemTransform getTransformsForBlock(BlockState blockState) {
      Item item = blockState.getBlock().asItem();
      if (item != Items.AIR && item instanceof BlockItem) {
         ModelResourceLocation modelIdentifier = BlockModelShaper.stateToModelLocation(blockState);
         BakedModel bakedModel = Minecraft.getInstance().getModelManager().getModel(modelIdentifier);
         ItemTransforms transforms = bakedModel.getTransforms();
         return transforms == null ? ItemTransform.NO_TRANSFORM : transforms.getTransform(ItemDisplayContext.GUI);
      } else {
         return ItemTransform.NO_TRANSFORM;
      }
   }

   private static boolean shouldRenderAsItem(BlockState state, ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return false;
      } else if (!state.hasBlockEntity() && state.getRenderShape() == RenderShape.MODEL) {
         Item item = itemStack.getItem();
         return item == Items.CAULDRON ? false : state.getProperties().isEmpty() && state.getFluidState().isEmpty();
      } else {
         return true;
      }
   }

   public static boolean shouldShade(BakedModel bakedModel, BlockState blockState) {
      RandomSource randomSource = RandomSource.create();

      for (Direction direction : Direction.values()) {
         randomSource.setSeed(42L);

         for (BakedQuad quad : bakedModel.getQuads(blockState, direction, randomSource)) {
            if (quad.isShade()) {
               return true;
            }
         }
      }

      randomSource.setSeed(42L);

      for (BakedQuad quadx : bakedModel.getQuads(blockState, null, randomSource)) {
         if (quadx.isShade()) {
            return true;
         }
      }

      return false;
   }

   private static class RenderSlot {
      private BlockRenderCache.RenderSlot previous;
      private BlockRenderCache.RenderSlot next;
      private final BlockRenderCache.SizedBlock key;
      private final TextureTarget textureId;
      private long lastRequestedTick;

      public RenderSlot(BlockRenderCache.SizedBlock key, TextureTarget textureId, long lastRequestedTick) {
         this.key = key;
         this.textureId = textureId;
         this.lastRequestedTick = lastRequestedTick;
      }

      private void unlink() {
         if (this == BlockRenderCache.head && this == BlockRenderCache.tail) {
            BlockRenderCache.head = null;
            BlockRenderCache.tail = null;
         } else if (this == BlockRenderCache.head) {
            this.next.previous = null;
            BlockRenderCache.head = this.next;
         } else if (this == BlockRenderCache.tail) {
            this.previous.next = null;
            BlockRenderCache.tail = this.previous;
         } else {
            this.next.previous = this.previous;
            this.previous.next = this.next;
         }

         this.previous = null;
         this.next = null;
      }
   }

   private record SizedBlock(BlockState state, boolean forceAsBlock, int width, int height) {
   }
}
