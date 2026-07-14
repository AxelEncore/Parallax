package com.moulberry.axiom;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.versioning.input_events.LegacyCharacterEvent;
import com.moulberry.axiom.versioning.input_events.LegacyKeyEvent;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class VersionUtilsClient {
   public static Vec3 Camera_position(Camera camera) {
      return camera.getPosition();
   }

   public static VertexFormat DefaultVertexFormat_POSITION_COLOR_NORMAL_LINE_WIDTH() {
      return DefaultVertexFormat.POSITION_COLOR_NORMAL;
   }

   public static VertexConsumer VertexConsumer_setLineWidth(VertexConsumer vertexConsumer, float lineWidth) {
      return legacySetLineWidthIgnored(vertexConsumer, lineWidth);
   }

   public static VertexConsumer legacySetLineWidthIgnored(VertexConsumer vertexConsumer, float lineWidth) {
      return vertexConsumer;
   }

   public static boolean legacyGuiEventListenerKeyPressed(GuiEventListener guiEventListener, LegacyKeyEvent keyEvent) {
      return guiEventListener.keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
   }

   public static boolean GuiEventListener_keyPressed(GuiEventListener guiEventListener, LegacyKeyEvent keyEvent) {
      return legacyGuiEventListenerKeyPressed(guiEventListener, keyEvent);
   }

   public static boolean legacyGuiEventListenerCharTyped(GuiEventListener guiEventListener, LegacyCharacterEvent characterEvent) {
      return guiEventListener.charTyped(characterEvent.character(), characterEvent.modifiers());
   }

   public static boolean GuiEventListener_charTyped(GuiEventListener guiEventListener, LegacyCharacterEvent characterEvent) {
      return legacyGuiEventListenerCharTyped(guiEventListener, characterEvent);
   }

   public static boolean legacyKeyMappingMatches(KeyMapping keyMapping, LegacyKeyEvent keyEvent) {
      return keyMapping.matches(keyEvent.key(), keyEvent.scancode());
   }

   public static boolean KeyMapping_matches(KeyMapping keyMapping, LegacyKeyEvent keyEvent) {
      return legacyKeyMappingMatches(keyMapping, keyEvent);
   }

   public static boolean InputQuirks_REPLACE_CTRL_KEY_WITH_CMD_KEY() {
      return Minecraft.ON_OSX;
   }

   public static Entity Minecraft_getCameraEntity(Minecraft minecraft) {
      return minecraft.cameraEntity;
   }

   public static boolean wasteFirstBoolean(Object ignored, boolean value) {
      return value;
   }

   public static boolean Minecraft_hasShiftDown(Minecraft minecraft) {
      return wasteFirstBoolean(minecraft, Screen.hasShiftDown());
   }

   public static boolean Minecraft_hasControlDown(Minecraft minecraft) {
      return wasteFirstBoolean(minecraft, Screen.hasControlDown());
   }

   public static boolean Minecraft_hasAltDown(Minecraft minecraft) {
      return wasteFirstBoolean(minecraft, Screen.hasAltDown());
   }

   public static long Window_handle(Window window) {
      return window.getWindow();
   }

   public static float getPartialTick(Minecraft minecraft) {
      return minecraft.getTimer().getGameTimeDeltaPartialTick(true);
   }

   public static void RenderPass_drawIndexed(Dummy renderPass, int baseVertex, int idk, int indexCount, int instanceCount) {
      legacyDrawIndexed(renderPass, baseVertex, idk, indexCount, instanceCount);
   }

   public static void legacyDrawIndexed(Dummy renderPass, int baseVertex, int idk, int indexCount, int instanceCount) {
   }

   public static AxiomGpuTexture GpuTextureView_texture(AxiomGpuTexture textureView) {
      return textureView;
   }

   public static AxiomGpuTexture RenderTarget_getDepthTextureView(RenderTarget renderTarget) {
      return new AxiomGpuTexture(renderTarget.getDepthTextureId());
   }

   public static AxiomGpuTexture RenderTarget_getColorTextureView(RenderTarget renderTarget) {
      return new AxiomGpuTexture(renderTarget.getColorTextureId());
   }

   public static AxiomGpuTexture AbstractTexture_getTextureView(AbstractTexture abstractTexture) {
      return new AxiomGpuTexture(abstractTexture.getId());
   }

   public static void RenderSystem_setShaderTexture(int binding, AxiomGpuTexture texture) {
      RenderSystem.setShaderTexture(binding, texture.glId());
   }

   public static AxiomGpuTexture RenderSystem_getShaderTexture(int binding) {
      return new AxiomGpuTexture(RenderSystem.getShaderTexture(binding));
   }

   public static AxiomGpuTexture RenderTarget_getDepthTexture(RenderTarget renderTarget) {
      return new AxiomGpuTexture(renderTarget.getDepthTextureId());
   }

   public static AxiomGpuTexture RenderTarget_getColorTexture(RenderTarget renderTarget) {
      return new AxiomGpuTexture(renderTarget.getColorTextureId());
   }

   public static AxiomGpuTexture DynamicTexture_getTexture(DynamicTexture dynamicTexture) {
      return new AxiomGpuTexture(dynamicTexture.getId());
   }

   public static DynamicTexture DynamicTexture_newFromNativeImage(Supplier<String> name, NativeImage nativeImage) {
      return helperCreateFromNativeImageDynamicTexture(name, nativeImage);
   }

   public static DynamicTexture helperCreateFromNativeImageDynamicTexture(Supplier<String> name, NativeImage nativeImage) {
      return new DynamicTexture(nativeImage);
   }

   public static DynamicTexture DynamicTexture_newWithSize(String name, int width, int height, boolean clear) {
      return helperCreateNewWithSizeDynamicTexture(name, width, height, clear);
   }

   public static DynamicTexture helperCreateNewWithSizeDynamicTexture(String name, int width, int height, boolean clear) {
      return new DynamicTexture(width, height, clear);
   }

   public static TextureTarget TextureTarget_new(String name, int width, int height, boolean useDepth) {
      return helperCreateNewTextureTarget(name, width, height, useDepth);
   }

   public static TextureTarget helperCreateNewTextureTarget(String name, int width, int height, boolean useDepth) {
      return new TextureTarget(width, height, useDepth, Minecraft.ON_OSX);
   }

   private static void RenderTarget_resize(RenderTarget renderTarget, int width, int height) {
      renderTarget.resize(width, height, Minecraft.ON_OSX);
   }

   private static int NativeImage_getPixel(NativeImage nativeImage, int x, int y) {
      return ColourUtils.abgrToArgb(nativeImage.getPixelRGBA(x, y));
   }

   private static void NativeImage_setPixel(NativeImage nativeImage, int x, int y, int argb) {
      nativeImage.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
   }

   private static void GlStateManager_clear(int clearFlags) {
      GlStateManager._clear(clearFlags, Minecraft.ON_OSX);
   }

   private static int CompiledShaderProgram_getProgramId(ShaderInstance compiledShaderProgram) {
      return compiledShaderProgram.getId();
   }

   private static void GuiGraphicsExtractor_blitSprite(
      GuiGraphics guiGraphics, Dummy pipeline, ResourceLocation resourceLocation, int x, int y, int width, int height, int colour
   ) {
      genericBlitSprite(guiGraphics, pipeline, resourceLocation, x, y, width, height, colour);
   }

   private static void GuiGraphicsExtractor_blitSprite(
      GuiGraphics guiGraphics, Dummy pipeline, ResourceLocation resourceLocation, int x, int y, int width, int height
   ) {
      genericBlitSprite(guiGraphics, pipeline, resourceLocation, x, y, width, height);
   }

   public static void genericBlitSprite(GuiGraphics guiGraphics, Dummy pipeline, ResourceLocation resourceLocation, int x, int y, int width, int height) {
      guiGraphics.blitSprite(resourceLocation, x, y, width, height);
   }

   public static void genericBlitSprite(
      GuiGraphics guiGraphics, Dummy pipeline, ResourceLocation resourceLocation, int x, int y, int width, int height, int colour
   ) {
      guiGraphics.blitSprite(resourceLocation, x, y, width, height);
   }

   public static void legacyBlitSprite(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int x, int y, int width, int height, int colour) {
      ResourceLocation newLocation = ResourceLocation.fromNamespaceAndPath(
         resourceLocation.getNamespace(), "textures/gui/sprites/" + resourceLocation.getPath() + ".png"
      );
      genericInnerBlit(guiGraphics, newLocation, x, x + width, y, y + height, 0.0F, 1.0F, 0.0F, 1.0F, colour);
   }

   public static void GuiGraphicsExtractor_blit(
      GuiGraphics guiGraphics, Dummy pipeline, ResourceLocation resourceLocation, int x, int y, float texX, float texY, int w, int h, int atlasW, int atlasH
   ) {
      genericBlit(guiGraphics, pipeline, resourceLocation, x, y, texX, texY, w, h, atlasW, atlasH, -1);
   }

   public static void GuiGraphicsExtractor_blit(
      GuiGraphics guiGraphics,
      Dummy pipeline,
      ResourceLocation resourceLocation,
      int x,
      int y,
      float texX,
      float texY,
      int w,
      int h,
      int atlasW,
      int atlasH,
      int colour
   ) {
      genericBlit(guiGraphics, pipeline, resourceLocation, x, y, texX, texY, w, h, atlasW, atlasH, colour);
   }

   public static void genericBlit(
      GuiGraphics guiGraphics,
      Dummy pipeline,
      ResourceLocation resourceLocation,
      int x,
      int y,
      float texX,
      float texY,
      int w,
      int h,
      int atlasW,
      int atlasH,
      int colour
   ) {
      genericBlit(guiGraphics, pipeline, resourceLocation, x, y, texX, texY, w, h, w, h, atlasW, atlasH, colour);
   }

   public static void GuiGraphicsExtractor_blit(
      GuiGraphics guiGraphics,
      Dummy pipeline,
      ResourceLocation resourceLocation,
      int x,
      int y,
      float texX,
      float texY,
      int w,
      int h,
      int texW,
      int texH,
      int atlasW,
      int atlasH
   ) {
      genericBlit(guiGraphics, pipeline, resourceLocation, x, y, texX, texY, w, h, texW, texH, atlasW, atlasH, -1);
   }

   public static void GuiGraphicsExtractor_blit(
      GuiGraphics guiGraphics,
      Dummy pipeline,
      ResourceLocation resourceLocation,
      int x,
      int y,
      float texX,
      float texY,
      int w,
      int h,
      int texW,
      int texH,
      int atlasW,
      int atlasH,
      int colour
   ) {
      genericBlit(guiGraphics, pipeline, resourceLocation, x, y, texX, texY, w, h, texW, texH, atlasW, atlasH, colour);
   }

   public static void genericBlit(
      GuiGraphics guiGraphics,
      Dummy pipeline,
      ResourceLocation resourceLocation,
      int x,
      int y,
      float texX,
      float texY,
      int w,
      int h,
      int texW,
      int texH,
      int atlasW,
      int atlasH,
      int colour
   ) {
      genericInnerBlit(guiGraphics, resourceLocation, x, x + w, y, y + h, texX / atlasW, (texX + texW) / atlasW, texY / atlasH, (texY + texH) / atlasH, colour);
   }

   private static void genericInnerBlit(
      GuiGraphics guiGraphics, ResourceLocation resourceLocation, int left, int right, int top, int bottom, float u0, float u1, float v0, float v1, int colour
   ) {
      Matrix4f matrix = guiGraphics.pose().last().pose();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      AxiomRenderer.setMainTexture(resourceLocation);
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      provider.clear();
      BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bufferBuilder.addVertex(matrix, left, top, 0.0F).setUv(u0, v0).setColor(colour);
      bufferBuilder.addVertex(matrix, left, bottom, 0.0F).setUv(u0, v1).setColor(colour);
      bufferBuilder.addVertex(matrix, right, bottom, 0.0F).setUv(u1, v1).setColor(colour);
      bufferBuilder.addVertex(matrix, right, top, 0.0F).setUv(u1, v0).setColor(colour);
      AxiomRenderPipelines.POSITION_TEX_COLOR.render(bufferBuilder.build());
   }

   public static void blit256(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int x, int y, int u, int v, int w, int h) {
      guiGraphics.blit(resourceLocation, x, y, u, v, w, h);
   }

   private static VertexConsumer BufferBuilder_addVertex(VertexConsumer bufferBuilder, Pose pose, float x, float y, float z) {
      return bufferBuilder.addVertex(pose, x, y, z);
   }

   private static VertexConsumer BufferBuilder_addVertex(VertexConsumer bufferBuilder, Matrix4f matrix, float x, float y, float z) {
      return bufferBuilder.addVertex(matrix, x, y, z);
   }

   public static VertexConsumer helperOldAddVertex(VertexConsumer bufferBuilder, Matrix4f pose, float x, float y, float z) {
      throw new AssertionError();
   }

   private static VertexConsumer BufferBuilder_addVertex(VertexConsumer bufferBuilder, float x, float y, float z) {
      return bufferBuilder.addVertex(x, y, z);
   }

   public static VertexConsumer helperOldAddVertex(VertexConsumer bufferBuilder, float x, float y, float z) {
      throw new AssertionError();
   }

   private static VertexConsumer VertexConsumer_setUv(VertexConsumer bufferBuilder, float u, float v) {
      return bufferBuilder.setUv(u, v);
   }

   private static VertexConsumer VertexConsumer_setUv1(VertexConsumer bufferBuilder, int u, int v) {
      return bufferBuilder.setUv1(u, v);
   }

   private static VertexConsumer VertexConsumer_setUv2(VertexConsumer bufferBuilder, int u, int v) {
      return bufferBuilder.setUv2(u, v);
   }

   private static VertexConsumer VertexConsumer_setNormal(VertexConsumer bufferBuilder, float x, float y, float z) {
      return bufferBuilder.setNormal(x, y, z);
   }

   private static VertexConsumer VertexConsumer_setNormal(VertexConsumer vertexConsumer, Pose pose, float x, float y, float z) {
      return vertexConsumer.setNormal(pose, x, y, z);
   }

   private static VertexConsumer VertexConsumer_setColor(VertexConsumer bufferBuilder, int color) {
      return bufferBuilder.setColor(color);
   }

   private static VertexConsumer VertexConsumer_setColor(VertexConsumer bufferBuilder, int r, int g, int b, int a) {
      return bufferBuilder.setColor(r, g, b, a);
   }

   private static VertexConsumer VertexConsumer_setColor(VertexConsumer bufferBuilder, float r, float g, float b, float a) {
      return bufferBuilder.setColor(r, g, b, a);
   }

   private static MeshData BufferBuilder_build(BufferBuilder bufferBuilder) {
      return bufferBuilder.build();
   }

   public static MeshData helperOldBufferBuilderEndOrDiscard(BufferBuilder bufferBuilder) {
      throw new AssertionError();
   }

   private static MeshData BufferBuilder_buildOrThrow(BufferBuilder bufferBuilder) {
      return bufferBuilder.buildOrThrow();
   }

   public static MeshData helperOldBufferBuilderEnd(BufferBuilder bufferBuilder) {
      throw new AssertionError();
   }

   private static void MeshData_close(MeshData meshData) {
      meshData.close();
   }

   public static void PoseStack_mulPose(PoseStack poseStack, Matrix4fc matrix4fc) {
      poseStack.mulPose(matrix4fcToMatrix4f(matrix4fc));
   }

   public static Matrix4f matrix4fcToMatrix4f(Matrix4fc matrix4fc) {
      return matrix4fc instanceof Matrix4f matrix4f ? matrix4f : new Matrix4f(matrix4fc);
   }

   private static void clearFocus(Screen screen) {
      screen.clearFocus();
   }

   public static void helperClearFocus(Screen screen) {
      ComponentPath componentPath = screen.getCurrentFocusPath();
      if (componentPath != null) {
         componentPath.applyFocus(false);
      }
   }
}
