package com.moulberry.axiom.utils;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomBlending;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.core_rendering.AxiomRenderPipeline;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.render.VertexConsumerProvider;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class FramebufferUtils {
   private static int dynamicReadFbo = -1;
   private static RenderTarget tempRenderTarget = null;

   public static void bindDepth(RenderTarget renderTarget) {
      renderTarget.bindWrite(false);
   }

   public static int bindColour(RenderTarget renderTarget) {
      renderTarget.bindWrite(false);
      return renderTarget.frameBufferId;
   }

   public static void unbind() {
      GlStateManager._glBindFramebuffer(36008, 0);
   }

   public static int getRawColorTextureId(RenderTarget renderTarget) {
      return renderTarget.getColorTextureId();
   }

   public static CompletableFuture<NativeImage> downloadAsync(RenderTarget renderTarget) {
      NativeImage nativeImage = new NativeImage(renderTarget.width, renderTarget.height, false);
      RenderSystem.bindTexture(renderTarget.getColorTextureId());
      nativeImage.downloadTexture(0, false);
      return CompletableFuture.completedFuture(nativeImage);
   }

   public static void copyDepth(RenderTarget from, RenderTarget to) {
      int oldReadFbo = GL11.glGetInteger(36010);
      int oldDrawFbo = GL11.glGetInteger(36006);
      to.copyDepthFrom(from);
      GlStateManager._glBindFramebuffer(36008, oldReadFbo);
      GlStateManager._glBindFramebuffer(36009, oldDrawFbo);
   }

   public static void clear(RenderTarget renderTarget, int colour) {
      int oldReadFbo = GL11.glGetInteger(36010);
      int oldDrawFbo = GL11.glGetInteger(36006);
      renderTarget.setClearColor((colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F, (colour >> 24 & 0xFF) / 255.0F);
      renderTarget.clear(Minecraft.ON_OSX);
      GlStateManager._glBindFramebuffer(36008, oldReadFbo);
      GlStateManager._glBindFramebuffer(36009, oldDrawFbo);
   }

   public static RenderTarget resizeOrCreateFramebuffer(RenderTarget renderTarget, int width, int height) {
      if (renderTarget == null) {
         renderTarget = VersionUtilsClient.helperCreateNewTextureTarget(null, width, height, true);
         renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      } else if (renderTarget.width != width || renderTarget.height != height) {
         renderTarget.resize(width, height, Minecraft.ON_OSX);
      }

      return renderTarget;
   }

   public static void blitToMainBlend(RenderTarget renderTarget, int width, int height) {
      blitToMainBlend(new AxiomGpuTexture(renderTarget.getColorTextureId()), width, height);
   }

   public static void blitToMainBlend(RenderTarget renderTarget, int width, int height, AxiomBlending blending) {
      blitToMainBlend(new AxiomGpuTexture(renderTarget.getColorTextureId()), width, height, blending);
   }

   public static void blitToMainBlend(AxiomGpuTexture textureId, int width, int height) {
      blitToMainBlend(textureId, width, height, AxiomBlending.DEFAULT);
   }

   public static void blitToMainBlend(AxiomGpuTexture textureId, int width, int height, AxiomBlending blending) {
      blitTo(textureId, null, width, height, 0.0F, 0.0F, 1.0F, 1.0F, blending);
   }

   public static void blitTo(AxiomGpuTexture from, RenderTarget to, int width, int height, float x1, float y1, float x2, float y2, AxiomBlending blending) {
      RenderHelper.pushModelViewMatrix(new Matrix4f().translation(0.0F, 0.0F, -2000.0F));
      ProjectionMatrixBackup backup = ProjectionMatrixBackup.create();
      RenderHelper.setOrthoProjectionMatrix(width, height);
      AxiomRenderer.setMainTexture(from);
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder builder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      builder.addVertex(width * x1, height * y2, 0.0F).setUv(0.0F, 0.0F);
      builder.addVertex(width * x2, height * y2, 0.0F).setUv(1.0F, 0.0F);
      builder.addVertex(width * x2, height * y1, 0.0F).setUv(1.0F, 1.0F);
      builder.addVertex(width * x1, height * y1, 0.0F).setUv(0.0F, 1.0F);
      AxiomRenderPipeline pipeline;
      if (blending == null) {
         pipeline = AxiomRenderPipelines.BLIT_NO_BLEND;
      } else {
         pipeline = switch (blending) {
            case ONE_ONE_MINUS_SRC_ALPHA -> AxiomRenderPipelines.BLIT_ONE_ONE_MINUS_SRC_ALPHA;
            default -> AxiomRenderPipelines.BLIT;
         };
      }

      pipeline.render(to, provider.build());
      backup.restore();
      RenderHelper.popModelViewStack();
   }

   public static void blitToScreenPartial(RenderTarget renderTarget, int width, int height, float x1, float y1, float x2, float y2) {
      GlStateManager._viewport(0, 0, width, height);
      int oldReadFbo = GL11.glGetInteger(36010);
      int oldDrawFbo = GL11.glGetInteger(36006);
      tempRenderTarget = resizeOrCreateFramebuffer(tempRenderTarget, width, height);
      clear(tempRenderTarget, 0);
      blitTo(new AxiomGpuTexture(renderTarget.getColorTextureId()), tempRenderTarget, width, height, x1, y1, x2, y2, null);
      GlStateManager._glBindFramebuffer(36009, 0);
      tempRenderTarget.blitToScreen(width, height);
      GlStateManager._glBindFramebuffer(36008, oldReadFbo);
      GlStateManager._glBindFramebuffer(36009, oldDrawFbo);
   }
}
