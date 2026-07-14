package com.moulberry.axiom.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.utils.AxiomVertexFormats;
import com.moulberry.axiom.utils.FramebufferUtils;
import java.util.Objects;
import java.util.function.Supplier;
import java.io.IOException;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

public class ShaderManager implements ResourceManagerReloadListener {
   public static final ShaderManager INSTANCE = new ShaderManager();
   private static Object blockShader = null;
   private static Object borderShader = null;
   private static Object brightenLightmapShader = null;
   private static Object selectionOutlineShader = null;
   private static Object collisionMeshShader = null;
   private static Object positionColorNoVertexColorShader = null;
   private static Object instancedBlockShader = null;
   private static Object instancedLineShader = null;
   private static Object blitScreenOldShader = null;
   private static boolean selectionOutlineCleared = false;
   private static boolean selectionOutlineRenderedTo = false;
   private static boolean selectionOutlineCopiedDepth = false;
   private static RenderTarget selectionOutlineTarget;

   public static void preCopySelectionOutlineTarget() {
      if (!selectionOutlineCleared) {
         FramebufferUtils.clear(selectionOutlineTarget, 0);
         selectionOutlineCleared = true;
      }

      if (!selectionOutlineCopiedDepth) {
         FramebufferUtils.copyDepth(Minecraft.getInstance().getMainRenderTarget(), selectionOutlineTarget);
         selectionOutlineCopiedDepth = true;
      }
   }

   public static RenderTarget getSelectionOutlineTarget(boolean copyDepth) {
      if (!selectionOutlineCleared) {
         selectionOutlineCleared = true;
         FramebufferUtils.clear(selectionOutlineTarget, 0);
      }

      if (copyDepth && !selectionOutlineCopiedDepth) {
         FramebufferUtils.copyDepth(Minecraft.getInstance().getMainRenderTarget(), selectionOutlineTarget);
         selectionOutlineCopiedDepth = true;
      }

      selectionOutlineRenderedTo = true;
      return selectionOutlineTarget;
   }

   public static void blitSelectionOutlineTarget() {
      if (selectionOutlineRenderedTo) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderTarget outTarget = Minecraft.getInstance().getMainRenderTarget();
         outTarget.bindWrite(true);
         RenderSystem.assertOnRenderThread();
         GlStateManager._colorMask(true, true, true, false);
         GlStateManager._disableDepthTest();
         GlStateManager._depthMask(false);
         ShaderInstance shader = enableSelectionOutlineShader();
         RenderSystem.backupProjectionMatrix();
         Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, outTarget.width, outTarget.height, 0.0F, 0.1F, 1000.0F);
         RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
         shader.setSampler("InSampler", selectionOutlineTarget.getColorTextureId());
         shader.safeGetUniform("InSize").set((float)selectionOutlineTarget.width, (float)selectionOutlineTarget.height);
         shader.safeGetUniform("OutSize").set((float)outTarget.width, (float)outTarget.height);
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
         bufferBuilder.addVertex(0.0F, 0.0F, 500.0F);
         bufferBuilder.addVertex(0.0F, outTarget.height, 500.0F);
         bufferBuilder.addVertex(outTarget.width, outTarget.height, 500.0F);
         bufferBuilder.addVertex(outTarget.width, 0.0F, 500.0F);
         BufferUploader.drawWithShader(Objects.requireNonNull(provider.build()));
         RenderSystem.restoreProjectionMatrix();
         GlStateManager._depthMask(true);
         RenderSystem.enableDepthTest();
         GlStateManager._colorMask(true, true, true, true);
         selectionOutlineTarget.clear(Minecraft.ON_OSX);
         outTarget.bindWrite(true);
      }

      selectionOutlineCleared = false;
      selectionOutlineRenderedTo = false;
      selectionOutlineCopiedDepth = false;
   }

   public void register(RegisterShadersEvent event) throws IOException {
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:block"), AxiomVertexFormats.AXIOM_BLOCK),
         shaderInstance -> blockShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:border"), DefaultVertexFormat.POSITION_COLOR),
         shaderInstance -> borderShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:selection_outline"), DefaultVertexFormat.POSITION),
         shaderInstance -> selectionOutlineShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:collision_mesh"), DefaultVertexFormat.POSITION_COLOR),
         shaderInstance -> collisionMeshShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:position_color_no_vertex_color"), DefaultVertexFormat.POSITION_COLOR),
         shaderInstance -> positionColorNoVertexColorShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:instanced_block"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP),
         shaderInstance -> instancedBlockShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:instanced_lines"), DefaultVertexFormat.POSITION_COLOR_NORMAL),
         shaderInstance -> instancedLineShader = shaderInstance);
      event.registerShader(
         new ShaderInstance(event.getResourceProvider(), ResourceLocation.parse("axiom:blit_screen_old"), DefaultVertexFormat.POSITION_TEX_COLOR),
         shaderInstance -> blitScreenOldShader = shaderInstance);
   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      if (selectionOutlineTarget != null) {
         selectionOutlineTarget.destroyBuffers();
      }

      Window window = Minecraft.getInstance().getWindow();
      selectionOutlineTarget = VersionUtilsClient.helperCreateNewTextureTarget(null, window.getWidth(), window.getHeight(), true);
   }

   public void onResolutionChanged(int width, int height) {
      if (selectionOutlineTarget != null) {
         selectionOutlineTarget.resize(width, height, Minecraft.ON_OSX);
      }
   }

   private static ShaderInstance enableAndGetShader(Supplier<ShaderInstance> supplier) {
      RenderSystem.setShader(supplier);
      return supplier.get();
   }

   public static ShaderInstance enableLinesShader() {
      return enableAndGetShader(GameRenderer::getRendertypeLinesShader);
   }

   public static ShaderInstance enablePositionColorShader() {
      return enableAndGetShader(GameRenderer::getPositionColorShader);
   }

   public static ShaderInstance enablePositionTexShader() {
      return enableAndGetShader(GameRenderer::getPositionTexShader);
   }

   public static ShaderInstance enablePositionTexColorShader() {
      return enableAndGetShader(GameRenderer::getPositionTexColorShader);
   }

   public static ShaderInstance enableRendertypeSolidShader() {
      return enableAndGetShader(GameRenderer::getRendertypeSolidShader);
   }

   public static ShaderInstance enableRendertypeCutoutShader() {
      return enableAndGetShader(GameRenderer::getRendertypeCutoutShader);
   }

   public static ShaderInstance enableRendertypeCutoutMippedShader() {
      return enableAndGetShader(GameRenderer::getRendertypeCutoutMippedShader);
   }

   public static ShaderInstance enableRendertypeTranslucentShader() {
      return enableAndGetShader(GameRenderer::getRendertypeTranslucentShader);
   }

   public static ShaderInstance enableRendertypeTripwireShader() {
      return enableAndGetShader(GameRenderer::getRendertypeTripwireShader);
   }

   public static ShaderInstance enableAxiomBlockShader() {
      return enableAndGetShader(() -> (ShaderInstance)blockShader);
   }

   public static ShaderInstance enableBorderShader() {
      return enableAndGetShader(() -> (ShaderInstance)borderShader);
   }

   public static ShaderInstance enableBrightenLightmapShader() {
      throw new IllegalStateException("Brighten lightmap shader is unused on this version");
   }

   public static ShaderInstance enableSelectionOutlineShader() {
      return enableAndGetShader(() -> (ShaderInstance)selectionOutlineShader);
   }

   public static ShaderInstance enableCollisionMeshShader() {
      return enableAndGetShader(() -> (ShaderInstance)collisionMeshShader);
   }

   public static ShaderInstance enablePositionColorNoVertexColorShader() {
      return enableAndGetShader(() -> (ShaderInstance)positionColorNoVertexColorShader);
   }

   public static ShaderInstance enableBlitScreenOldShader() {
      return enableAndGetShader(() -> (ShaderInstance)blitScreenOldShader);
   }
}
