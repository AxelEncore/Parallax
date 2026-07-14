package com.moulberry.axiom.core_rendering;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;

public class AxiomRenderPipeline {
   String name;
   AxiomShader shader;
   boolean blend = true;
   AxiomBlending blendFunction = AxiomBlending.DEFAULT;
   boolean depthTest = true;
   boolean depthWrite = true;
   boolean writeAlpha = true;
   float lineWidthMultiplier = 0.0F;
   boolean cull = true;
   float polygonOffset = 0.0F;
   boolean disableFog = false;
   float alphaCutout = 0.0F;
   int cullFace = 1029;
   int depthFunc = 515;
   boolean useLightLayer = false;
   boolean mipmapTexture = true;

   public AxiomRenderPipeline(String name, AxiomShader shader) {
      this.name = name;
      this.shader = shader;
   }

   public void render(MeshData meshData) {
      this.render(Minecraft.getInstance().getMainRenderTarget(), meshData);
   }

   public void render(AxiomDrawBuffer drawBuffer) {
      this.render(Minecraft.getInstance().getMainRenderTarget(), drawBuffer);
   }

   public void render(RenderTarget target, MeshData meshData) {
      if (target == null) {
         target = Minecraft.getInstance().getMainRenderTarget();
      }

      AxiomRenderer.renderPipeline(this, target, meshData, true);
   }

   public void render(RenderTarget target, AxiomDrawBuffer drawBuffer) {
      if (target == null) {
         target = Minecraft.getInstance().getMainRenderTarget();
      }

      AxiomRenderer.renderPipeline(this, target, drawBuffer);
   }
}
