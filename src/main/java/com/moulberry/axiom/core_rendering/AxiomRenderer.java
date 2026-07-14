package com.moulberry.axiom.core_rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.moulberry.axiom.render.ShaderManager;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class AxiomRenderer {
   private static final Map<VertexFormat, VertexBuffer> availableImmediateBuffers = new HashMap<>();
   private static final Map<MeshData, VertexBuffer> rememberedVertexBuffers = new HashMap<>();

   public static void setMainTexture(ResourceLocation resourceLocation) {
      RenderSystem.setShaderTexture(0, resourceLocation);
   }

   public static void setMainTexture(AxiomGpuTexture axiomGpuTexture) {
      RenderSystem.setShaderTexture(0, axiomGpuTexture.glId());
   }

   public static void dirtyDynamicUniforms() {
   }

   public static void setShaderColour(float r, float g, float b, float a) {
      RenderSystem.setShaderColor(r, g, b, a);
   }

   public static void enableScissor(int x, int y, int width, int height) {
      RenderSystem.enableScissor(x, y, width, height);
   }

   public static void setLineWidthLegacy(float f) {
      RenderSystem.lineWidth(f);
   }

   public static void disableScissor() {
      RenderSystem.disableScissor();
   }

   public static void forgetRememberedVertexBuffers() {
      for (Entry<MeshData, VertexBuffer> entry : rememberedVertexBuffers.entrySet()) {
         VertexFormat format = entry.getKey().drawState().format();
         if (!availableImmediateBuffers.containsKey(format)) {
            availableImmediateBuffers.put(format, entry.getValue());
         } else {
            entry.getValue().close();
         }
      }

      rememberedVertexBuffers.clear();
   }

   public static void renderPipeline(AxiomRenderPipeline pipeline, RenderTarget target, MeshData meshData, boolean closeMeshData) {
      if (meshData != null) {
         boolean rememberMeshData = !closeMeshData;
         VertexFormat format = meshData.drawState().format();
         int lastFbo = GL11.glGetInteger(36006);
         ShaderInstance shader = setupPipeline(pipeline);
         if (target != null && target != Minecraft.getInstance().getMainRenderTarget()) {
            target.bindWrite(true);
         }

         boolean usingImmediate = false;
         boolean usingRemembered = false;
         VertexBuffer vertexBuffer;
         if (rememberedVertexBuffers.containsKey(meshData)) {
            vertexBuffer = rememberedVertexBuffers.remove(meshData);
            usingRemembered = true;
         } else if (!rememberMeshData) {
            vertexBuffer = format.getImmediateDrawVertexBuffer();
            usingImmediate = true;
         } else if (availableImmediateBuffers.containsKey(format)) {
            vertexBuffer = availableImmediateBuffers.remove(format);
         } else {
            vertexBuffer = new VertexBuffer(AxiomBufferUsage.STREAM_WRITE.bufferUsage());
         }

         vertexBuffer.bind();
         if (!usingRemembered) {
            vertexBuffer.upload(meshData);
         }

         vertexBuffer.draw();
         VertexBuffer.unbind();
         finish(pipeline, shader);
         if (!usingImmediate) {
            if (rememberMeshData) {
               rememberedVertexBuffers.put(meshData, vertexBuffer);
            } else if (!availableImmediateBuffers.containsKey(format)) {
               availableImmediateBuffers.put(format, vertexBuffer);
            } else {
               vertexBuffer.close();
            }
         }

         GlStateManager._glBindFramebuffer(36160, lastFbo);
      }
   }

   public static void renderPipeline(AxiomRenderPipeline pipeline, RenderTarget target, AxiomDrawBuffer axiomDrawBuffer) {
      if (axiomDrawBuffer.vertexBuffer != null) {
         int lastFbo = GL11.glGetInteger(36006);
         ShaderInstance shader = setupPipeline(pipeline);
         if (target != null && target != Minecraft.getInstance().getMainRenderTarget()) {
            target.bindWrite(true);
         }

         axiomDrawBuffer.draw();
         finish(pipeline, shader);
         GlStateManager._glBindFramebuffer(36160, lastFbo);
      }
   }

   public static void renderPipeline(AxiomRenderPipeline pipeline, RenderTarget target, List<AxiomDraw> axiomDraws) {
      if (!axiomDraws.isEmpty()) {
         int lastFbo = GL11.glGetInteger(36006);
         ShaderInstance shader = setupPipeline(pipeline);
         if (target != null && target != Minecraft.getInstance().getMainRenderTarget()) {
            target.bindWrite(true);
         }

         for (AxiomDraw axiomDraw : axiomDraws) {
            if (axiomDraw.modelViewMatrix() != null && shader.MODEL_VIEW_MATRIX != null) {
               shader.MODEL_VIEW_MATRIX.set(axiomDraw.modelViewMatrix());
               shader.MODEL_VIEW_MATRIX.upload();
            }

            if (axiomDraw.modelOffset() != null && shader.CHUNK_OFFSET != null) {
               shader.CHUNK_OFFSET.set(axiomDraw.modelOffset());
               shader.CHUNK_OFFSET.upload();
            }

            axiomDraw.drawBuffer().draw();
         }

         finish(pipeline, shader);
         if (shader.CHUNK_OFFSET != null) {
            shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
         }

         GlStateManager._glBindFramebuffer(36160, lastFbo);
      }
   }

   private static ShaderInstance setupPipeline(AxiomRenderPipeline pipeline) {
      pipeline.shader.setupRenderState();

      ShaderInstance shaderProgram = switch (pipeline.shader) {
         case BLIT_SCREEN_OLD -> {
            ShaderInstance shader = ShaderManager.enableBlitScreenOldShader();
            GlStateManager._colorMask(true, true, true, false);
            yield shader;
         }
         case LINES -> ShaderManager.enableLinesShader();
         case AXIOM_BLOCK -> ShaderManager.enableAxiomBlockShader();
         case POSITION_COLOR -> ShaderManager.enablePositionColorShader();
         case POSITION_TEX_COLOR -> ShaderManager.enablePositionTexColorShader();
         case POSITION_TEX -> ShaderManager.enablePositionTexShader();
         case TERRAIN_SOLID -> ShaderManager.enableRendertypeSolidShader();
         case TERRAIN_CUTOUT -> ShaderManager.enableRendertypeCutoutShader();
         case TERRAIN_CUTOUT_MIPPED -> ShaderManager.enableRendertypeCutoutMippedShader();
         case TERRAIN_TRANSLUCENT -> ShaderManager.enableRendertypeTranslucentShader();
         case TERRAIN_TRIPWIRE -> ShaderManager.enableRendertypeTripwireShader();
         case POSITION_COLOR_NO_VERTEX_COLOR -> ShaderManager.enablePositionColorNoVertexColorShader();
         case BIOME_OVERLAY -> ShaderManager.enableBorderShader();
         case COLLISION_MESH_OVERLAY -> ShaderManager.enableCollisionMeshShader();
         case BRIGHTEN_LIGHT_TEXTURE -> ShaderManager.enableBrightenLightmapShader();
      };
      if (pipeline.polygonOffset != 0.0F) {
         RenderSystem.enablePolygonOffset();
         RenderSystem.polygonOffset(pipeline.polygonOffset, pipeline.polygonOffset);
      }

      if (pipeline.blend) {
         RenderSystem.enableBlend();
         switch (pipeline.blendFunction) {
            case DEFAULT:
               RenderSystem.defaultBlendFunc();
               break;
            case ONE_ZERO:
               RenderSystem.blendFuncSeparate(1, 0, 1, 0);
               break;
            case ONE_ONE_MINUS_SRC_ALPHA:
               RenderSystem.blendFuncSeparate(1, 771, 1, 771);
         }
      } else {
         RenderSystem.disableBlend();
      }

      if (pipeline.depthTest) {
         RenderSystem.enableDepthTest();
         RenderSystem.depthFunc(pipeline.depthFunc);
      } else {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.depthMask(pipeline.depthWrite);
      if (pipeline.cull) {
         RenderSystem.enableCull();
      } else {
         RenderSystem.disableCull();
      }

      if (pipeline.lineWidthMultiplier != 0.0) {
         setLineWidthLegacy(RenderHelper.baseLineWidth * pipeline.lineWidthMultiplier);
      }

      if (pipeline.disableFog) {
         RenderHelper.pushDisableFog();
      }

      if (pipeline.useLightLayer) {
         Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
      }

      GL11.glCullFace(pipeline.cullFace);
      Window window = Minecraft.getInstance().getWindow();
      RenderHelper.setupShader(shaderProgram, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), window.getWidth(), window.getHeight());
      return shaderProgram;
   }

   private static void finish(AxiomRenderPipeline pipeline, ShaderInstance shader) {
      RenderHelper.finishShader(shader);
      pipeline.shader.clearRenderState();
      if (pipeline.disableFog) {
         RenderHelper.popDisableFog();
      }

      if (pipeline.useLightLayer) {
         Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
      }

      if (pipeline.polygonOffset != 0.0) {
         RenderSystem.disablePolygonOffset();
      }

      if (!pipeline.blend) {
         RenderSystem.enableBlend();
      }

      if (!pipeline.depthTest) {
         RenderSystem.enableDepthTest();
      }

      if (!pipeline.cull) {
         RenderSystem.enableCull();
      }

      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(true);
      RenderSystem.depthFunc(515);
      RenderSystem.colorMask(true, true, true, true);
      GL11.glCullFace(1029);
   }
}
