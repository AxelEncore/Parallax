package com.moulberry.axiom.core_rendering;

import com.mojang.blaze3d.platform.GlStateManager;

public class AxiomGpuTexture implements AutoCloseable {
   private final int glId;

   public AxiomGpuTexture(int glId) {
      this.glId = glId;
   }

   public int glId() {
      return this.glId;
   }

   @Override
   public void close() {
      GlStateManager._deleteTexture(this.glId);
   }

   @Override
   public String toString() {
      return "AxiomGpuTexture{glId=" + this.glId + "}";
   }
}
