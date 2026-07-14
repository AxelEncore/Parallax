package com.moulberry.axiom.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.GlobalCleaner;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class AutoCleaningDynamicTexture extends DynamicTexture {
   private AutoCleaningDynamicTexture.CleanState cleanState = null;

   public AutoCleaningDynamicTexture(NativeImage nativeImage) {
      super(nativeImage);
      if (this.cleanState == null) {
         this.cleanState = new AutoCleaningDynamicTexture.CleanState();
      }

      this.cleanState.nativeImage = this.getPixels();
      GlobalCleaner.INSTANCE.register(this, this.cleanState);
   }

   public AutoCleaningDynamicTexture(int width, int height, boolean cleared) {
      super(width, height, cleared);
      if (this.cleanState == null) {
         this.cleanState = new AutoCleaningDynamicTexture.CleanState();
      }

      this.cleanState.nativeImage = this.getPixels();
      GlobalCleaner.INSTANCE.register(this, this.cleanState);
   }

   public void setPixels(NativeImage nativeImage) {
      super.setPixels(nativeImage);
      if (this.cleanState == null) {
         this.cleanState = new AutoCleaningDynamicTexture.CleanState();
      }

      this.cleanState.nativeImage = this.getPixels();
   }

   public int getId() {
      int textureId = super.getId();
      if (this.cleanState == null) {
         this.cleanState = new AutoCleaningDynamicTexture.CleanState();
      }

      if (this.cleanState.texture == null || this.cleanState.texture.glId() != textureId) {
         this.cleanState.texture = new AxiomGpuTexture(textureId);
      }

      return textureId;
   }

   public void releaseId() {
      super.releaseId();
      if (this.cleanState != null) {
         this.cleanState.texture = null;
      }
   }

   public void close() {
      super.close();
      if (this.cleanState != null) {
         this.cleanState.nativeImage = null;
         this.cleanState.texture = null;
         this.cleanState = null;
      }
   }

   private static class CleanState implements Runnable {
      private NativeImage nativeImage = null;
      private AxiomGpuTexture texture = null;

      @Override
      public void run() {
         if (this.nativeImage != null) {
            this.nativeImage.close();
            this.nativeImage = null;
         }

         if (this.texture != null) {
            EditorUI.deferredClose(this.texture);
            this.texture = null;
         }
      }
   }
}
