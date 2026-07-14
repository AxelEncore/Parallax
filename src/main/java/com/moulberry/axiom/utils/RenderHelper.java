package com.moulberry.axiom.utils;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

public class RenderHelper {
   public static float baseLineWidth = 2.5F;
   private static float previousFogStart = 0.0F;

   public static void updateBaseLineWidth(Window window) {
      double screenScaleFactor = window.framebufferWidth * window.framebufferHeight / 2073600.0;
      baseLineWidth = Math.max(2.5F, (float)(Math.sqrt(screenScaleFactor) * 2.5));
   }

   public static void tryApplyModelViewMatrix() {
      RenderSystem.applyModelViewMatrix();
      AxiomRenderer.dirtyDynamicUniforms();
   }

   public static void tryFlush(GuiGraphics guiGraphics) {
      guiGraphics.flush();
   }

   private static boolean isZZeroToOne() {
      return false;
   }

   public static void setOrthoProjectionMatrix(float width, float height) {
      RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
   }

   public static void setPerspectiveProjectionMatrix(float zNear, float zFar, float fov, float width, float height) {
      Matrix4f matrix = new Matrix4f();
      matrix.perspective((float)Math.toRadians(fov), width / height, zNear, zFar, isZZeroToOne());
      RenderSystem.setProjectionMatrix(matrix, VertexSorting.DISTANCE_TO_ORIGIN);
   }

   public static void setupShader(ShaderInstance shaderInstance, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
      Window window = Minecraft.getInstance().getWindow();
      setupShader(shaderInstance, modelViewMatrix, projectionMatrix, window.getWidth(), window.getHeight());
   }

   public static void setupShader(ShaderInstance shaderInstance, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float screenWidth, float screenHeight) {
      RenderSystem.assertOnRenderThread();

      for (int index = 0; index < 12; index++) {
         bindSampler(shaderInstance, "Sampler" + index, RenderSystem.getShaderTexture(index));
      }

      if (shaderInstance.MODEL_VIEW_MATRIX != null) {
         shaderInstance.MODEL_VIEW_MATRIX.set(modelViewMatrix);
      }

      if (shaderInstance.PROJECTION_MATRIX != null) {
         shaderInstance.PROJECTION_MATRIX.set(projectionMatrix);
      }

      if (shaderInstance.COLOR_MODULATOR != null) {
         shaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
      }

      if (shaderInstance.GLINT_ALPHA != null) {
         shaderInstance.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
      }

      if (shaderInstance.FOG_START != null) {
         shaderInstance.FOG_START.set(RenderSystem.getShaderFogStart());
      }

      if (shaderInstance.FOG_END != null) {
         shaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
      }

      if (shaderInstance.FOG_COLOR != null) {
         shaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
      }

      if (shaderInstance.FOG_SHAPE != null) {
         shaderInstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
      }

      if (shaderInstance.TEXTURE_MATRIX != null) {
         shaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
      }

      if (shaderInstance.GAME_TIME != null) {
         shaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
      }

      if (shaderInstance.SCREEN_SIZE != null) {
         shaderInstance.SCREEN_SIZE.set(screenWidth, screenHeight);
      }

      if (shaderInstance.LINE_WIDTH != null) {
         shaderInstance.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
      }

      RenderSystem.setupShaderLights(shaderInstance);
      shaderInstance.apply();
   }

   public static void finishShader(ShaderInstance shaderInstance) {
      shaderInstance.clear();
   }

   public static void pushDisableFog() {
      previousFogStart = RenderSystem.getShaderFogStart();
      RenderSystem.setShaderFogStart(Float.MAX_VALUE);
   }

   public static void popDisableFog() {
      RenderSystem.setShaderFogStart(previousFogStart);
   }

   public static void pushModelViewMatrix(Matrix4f matrix4f) {
      Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushMatrix();
      modelViewStack.set(matrix4f);
      tryApplyModelViewMatrix();
   }

   public static void pushModelViewStackWithIdentity() {
      Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushMatrix();
      modelViewStack.identity();
      tryApplyModelViewMatrix();
   }

   public static void popModelViewStack() {
      Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.popMatrix();
      tryApplyModelViewMatrix();
   }

   public static void setupFlatLighting() {
      RenderSystem.setShaderLights(new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f(0.0F, 0.0F, 1.0F));
   }

   public static void setup3DLighting() {
      Lighting.setupFor3DItems();
   }

   public static void bindSampler(ShaderInstance compiledShaderProgram, String name, int rawTextureId) {
      compiledShaderProgram.setSampler(name, rawTextureId);
   }

   public static void bindSampler(ShaderInstance compiledShaderProgram, String name, AxiomGpuTexture textureId) {
      compiledShaderProgram.setSampler(name, textureId.glId());
   }
}
