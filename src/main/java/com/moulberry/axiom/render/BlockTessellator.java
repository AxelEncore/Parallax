package com.moulberry.axiom.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.core_rendering.AxiomRenderPipeline;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.utils.AxiomVertexFormats;
import com.moulberry.axiom.utils.EmptyBlockAndTintGetter;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class BlockTessellator {
   public static final BlockTessellator SHARED = new BlockTessellator(true, false);
   private final RandomSource randomSource = RandomSource.create(0L);
   private BlockRenderDispatcher renderManager = Minecraft.getInstance().getBlockRenderer();
   private PoseStack poseStack = new PoseStack();
   private final boolean useAmbientOcclusion;
   private final boolean cacheModel;

   public BlockTessellator(boolean useAmbientOcclusion, boolean cacheModel) {
      this.useAmbientOcclusion = useAmbientOcclusion;
      this.cacheModel = cacheModel;
      this.reset();
   }

   public void reset() {
      this.renderManager = Minecraft.getInstance().getBlockRenderer();
   }

   public void tessellateBlockAndLiquidOffsetMod16(BufferBuilder bufferBuilder, BlockPos blockPos, BlockAndTintGetter region, BlockState blockState) {
      this.tessellateBlockAndLiquidOffsetMod16WithBuffers(l -> bufferBuilder, blockPos, region, blockState);
   }

   public void tessellateBlockAndLiquidOffsetMod16WithBuffers(
      Function<ChunkRenderOverrider.AxiomChunkOverrideLayer, BufferBuilder> bufferBuilders, BlockPos blockPos, BlockAndTintGetter region, BlockState blockState
   ) {
      RenderShape renderShape = blockState.getRenderShape();
      float x = blockPos.getX() & 15;
      float y = blockPos.getY() & 15;
      float z = blockPos.getZ() & 15;
      if (renderShape == RenderShape.MODEL) {
         BakedModel bakedModel = this.renderManager.getBlockModel(blockState);
         this.poseStack.pushPose();
         this.poseStack.translate(x, y, z);
         if (!this.cacheModel) {
            Vec3 offset = blockState.getOffset(EmptyBlockGetter.INSTANCE, blockPos);
            this.poseStack.translate((float)offset.x, (float)offset.y, (float)offset.z);
         }

         RenderType chunkRenderType = ItemBlockRenderTypes.getChunkRenderType(blockState);
         BufferBuilder bufferBuilder = bufferBuilders.apply(ChunkRenderOverrider.AxiomChunkOverrideLayer.fromVanilla(chunkRenderType));
         if (this.useAmbientOcclusion && blockState.getLightEmission() == 0 && bakedModel.useAmbientOcclusion()) {
            this.renderManager
               .getModelRenderer()
               .tesselateWithAO(
                  region,
                  bakedModel,
                  blockState,
                  blockPos,
                  this.poseStack,
                  bufferBuilder,
                  true,
                  this.randomSource,
                  blockState.getSeed(blockPos),
                  OverlayTexture.NO_OVERLAY
               );
         } else {
            this.renderManager
               .getModelRenderer()
               .tesselateWithoutAO(
                  region,
                  bakedModel,
                  blockState,
                  blockPos,
                  this.poseStack,
                  bufferBuilder,
                  true,
                  this.randomSource,
                  blockState.getSeed(blockPos),
                  OverlayTexture.NO_OVERLAY
               );
         }

         this.poseStack.popPose();
      }

      FluidState fluid = blockState.getFluidState();
      if (!fluid.isEmpty()) {
         RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluid);
         BufferBuilder bufferBuilder = bufferBuilders.apply(ChunkRenderOverrider.AxiomChunkOverrideLayer.fromVanilla(renderType));
         this.renderManager.renderLiquid(blockPos, region, bufferBuilder, blockState, fluid);
      }
   }

   public void tessellateSingleBlock(BufferBuilder bufferBuilder, BlockPos blockPos, BlockState blockState, PoseStack poseStack) {
      RenderShape renderShape = blockState.getRenderShape();
      if (renderShape == RenderShape.MODEL) {
         if (!this.cacheModel) {
            Vec3 offset = blockState.getOffset(EmptyBlockGetter.INSTANCE, blockPos);
            poseStack.translate((float)offset.x, (float)offset.y, (float)offset.z);
         }

         BakedModel bakedModel = this.renderManager.getBlockModel(blockState);
         if (this.useAmbientOcclusion && blockState.getLightEmission() == 0 && bakedModel.useAmbientOcclusion()) {
            this.renderManager
               .getModelRenderer()
               .tesselateWithAO(
                  EmptyBlockAndTintGetter.INSTANCE,
                  bakedModel,
                  blockState,
                  blockPos,
                  poseStack,
                  bufferBuilder,
                  true,
                  this.randomSource,
                  blockState.getSeed(blockPos),
                  OverlayTexture.NO_OVERLAY
               );
         } else {
            this.renderManager
               .getModelRenderer()
               .tesselateWithoutAO(
                  EmptyBlockAndTintGetter.INSTANCE,
                  bakedModel,
                  blockState,
                  blockPos,
                  poseStack,
                  bufferBuilder,
                  true,
                  this.randomSource,
                  blockState.getSeed(blockPos),
                  OverlayTexture.NO_OVERLAY
               );
         }
      }

      FluidState fluid = blockState.getFluidState();
      if (!fluid.isEmpty()) {
         InjectPoseVertexConsumer injectPoseVertexConsumer = new InjectPoseVertexConsumer(poseStack, bufferBuilder);
         this.renderManager.renderLiquid(BlockPos.ZERO, EmptyBlockAndTintGetter.INSTANCE, injectPoseVertexConsumer, blockState, fluid);
      }
   }

   public void renderBlock(AxiomRenderPipeline pipeline, BlockState blockState, BlockPos blockPos, PoseStack poseStack, BufferSource multiBufferSource) {
      RenderShape renderShape = blockState.getRenderShape();
      if (renderShape == RenderShape.MODEL || !blockState.getFluidState().isEmpty()) {
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, AxiomVertexFormats.AXIOM_BLOCK);
         SHARED.tessellateSingleBlock(bufferBuilder, blockPos, blockState, poseStack);
         TextureManager textureManager = Minecraft.getInstance().getTextureManager();
         textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, true);
         AxiomRenderer.setMainTexture(TextureAtlas.LOCATION_BLOCKS);
         pipeline.render(provider.build());
      } else if (renderShape != RenderShape.INVISIBLE) {
         Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, poseStack, multiBufferSource, 15728880, OverlayTexture.NO_OVERLAY);
         multiBufferSource.endBatch();
      }
   }
}
