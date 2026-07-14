package com.moulberry.axiom.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.core_rendering.AxiomRenderPipeline;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.utils.RenderHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class EffectRenderer {
   public static void render(EffectRenderer.EffectCallback effectCallback, long time, int effects) {
      if (Effects.any(effects)) {
         if ((effects & 8) != 0) {
            float factor = (float)Math.sin((float)time / 5.0E8F) * 0.5F + 0.5F;
            factor = factor < 0.5 ? 2.0F * (float)Math.pow(factor, 2.0) : 1.0F - (float)Math.pow(-2.0F * factor + 2.0F, 2.0) / 2.0F;
            if (factor > 0.0F) {
               AxiomRenderer.setShaderColour(1.0F, 0.5F, 0.5F, 0.2F + factor * 0.2F);
               effectCallback.render(AxiomRenderPipelines.EFFECT_RENDERER_NORMAL_PIPELINE, null);
            }
         } else if (Effects.blueOrRed(effects)) {
            float factor;
            if ((effects & 3) == 1) {
               factor = 1.0F;
            } else if ((effects & 3) == 2) {
               factor = 0.0F;
            } else {
               factor = (float)Math.sin((float)time / 5.0E8F) * 0.5F + 0.5F;
               factor = factor < 0.5 ? 2.0F * (float)Math.pow(factor, 2.0) : 1.0F - (float)Math.pow(-2.0F * factor + 2.0F, 2.0) / 2.0F;
            }

            if (factor < 1.0F) {
               AxiomRenderer.setShaderColour(1.0F, 0.5F, 0.5F, 0.3F - factor * 0.3F);
               effectCallback.render(AxiomRenderPipelines.EFFECT_RENDERER_RED_INVERSE_DEPTH, null);
            }

            if (factor > 0.0F) {
               AxiomRenderer.setShaderColour(1.0F - factor * 0.3F, 1.0F - factor * 0.3F, 1.0F, factor * 0.5F);
               effectCallback.render(AxiomRenderPipelines.EFFECT_RENDERER_BLUE_BACK, null);
               effectCallback.render(AxiomRenderPipelines.EFFECT_RENDERER_NORMAL_PIPELINE, null);
            }
         }

         if ((effects & 4) != 0) {
            AxiomRenderer.setShaderColour(1.0F, 0.9F, 0.12F, 1.0F);
            effectCallback.render(AxiomRenderPipelines.EFFECT_RENDERER_OUTLINE, ShaderManager.getSelectionOutlineTarget(false));
         }

         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static void renderBoundingBox(AxiomWorldRenderContext rc, Vec3 one, Vec3 two, int effects) {
      double minX = Math.min(one.x(), two.x()) - 1.0E-4;
      double minY = Math.min(one.y(), two.y()) - 1.0E-4;
      double minZ = Math.min(one.z(), two.z()) - 1.0E-4;
      float sizeX = (float)Math.abs(one.x() - two.x()) + 2.0E-4F;
      float sizeY = (float)Math.abs(one.y() - two.y()) + 2.0E-4F;
      float sizeZ = (float)Math.abs(one.z() - two.z()) + 2.0E-4F;
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(minX - rc.x(), minY - rc.y(), minZ - rc.z());
      RenderHelper.tryApplyModelViewMatrix();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Shapes.lineBox(matrices, bufferBuilder, 0.0F, 0.0F, 0.0F, sizeX, sizeY, sizeZ, 1.0F, 1.0F, 1.0F, 0.5F, 1.0F, 1.0F, 1.0F, RenderHelper.baseLineWidth);
      AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(provider.build());
      bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Shapes.lineBox(matrices, bufferBuilder, 0.0F, 0.0F, 0.0F, sizeX, sizeY, sizeZ, 1.0F, 1.0F, 1.0F, 0.15F, 1.0F, 1.0F, 1.0F, RenderHelper.baseLineWidth);
      AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
      render((pipeline, target) -> {
         Shapes.shadedBox(provider, matrices.last().pose(), sizeX, sizeY, sizeZ, -1);
         pipeline.render(target, provider.build());
      }, rc.nanos(), effects & -5);
      matrices.popPose();
   }

   @FunctionalInterface
   public interface EffectCallback {
      void render(AxiomRenderPipeline var1, @Nullable RenderTarget var2);
   }
}
