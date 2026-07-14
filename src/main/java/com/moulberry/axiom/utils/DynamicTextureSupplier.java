package com.moulberry.axiom.utils;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.moulberry.axiom.GlobalCleaner;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jetbrains.annotations.Nullable;

public class DynamicTextureSupplier implements AutoCloseable {
   private NativeImage nativeImage;
   private DynamicTextureSupplier.CleanState cleanState;
   private DynamicTexture dynamicTexture = null;
   private boolean closed = false;

   public DynamicTextureSupplier(NativeImage nativeImage) {
      this.nativeImage = nativeImage;
      this.cleanState = new DynamicTextureSupplier.CleanState();
      this.cleanState.nativeImage = nativeImage;
      GlobalCleaner.INSTANCE.register(this, this.cleanState);
   }

   @Nullable
   public DynamicTexture get() {
      if (this.closed) {
         return null;
      } else {
         if (this.dynamicTexture == null && RenderSystem.isOnRenderThread()) {
            this.dynamicTexture = new DynamicTexture(this.nativeImage);
            this.cleanState.dynamicTexture = this.dynamicTexture;
         }

         return this.dynamicTexture;
      }
   }

   public NativeImage getPixels() {
      return this.nativeImage;
   }

   @Override
   public void close() {
      this.closed = true;
      if (this.dynamicTexture != null) {
         this.dynamicTexture.close();
         this.dynamicTexture = null;
      }

      if (this.nativeImage != null) {
         this.nativeImage.close();
         this.nativeImage = null;
      }

      if (this.cleanState != null) {
         this.cleanState.nativeImage = null;
         this.cleanState.dynamicTexture = null;
         this.cleanState = null;
      }
   }

   private static class CleanState implements Runnable {
      private NativeImage nativeImage = null;
      private DynamicTexture dynamicTexture = null;

      @Override
      public void run() {
         if (this.nativeImage != null) {
            this.nativeImage.close();
            this.nativeImage = null;
         }

         if (this.dynamicTexture != null) {
            EditorUI.deferredClose(this.dynamicTexture);
            this.dynamicTexture = null;
         }
      }
   }
}
