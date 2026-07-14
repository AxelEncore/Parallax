package com.moulberry.axiom.mixin;

import com.mojang.blaze3d.platform.Window;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.WindowExt;
import com.moulberry.axiom.utils.WindowSizeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Window.class})
public abstract class MixinWindow implements WindowExt {
   @Shadow
   private int framebufferWidth;
   @Shadow
   private int framebufferHeight;
   @Shadow
   private int width;
   @Shadow
   private int height;
   @Shadow
   private double guiScale;
   @Shadow
   private int guiScaledWidth;
   @Shadow
   private int guiScaledHeight;

   @Shadow
   protected abstract void onResize(long var1, int var3, int var4);

   @Shadow
   public abstract long getWindow();

   @Shadow
   protected abstract void onFramebufferResize(long var1, int var3, int var4);

   @Override
   public void axiom$resize(long window, int width, int height) {
      this.onResize(window, width, height);
   }

   @Override
   public void axiom$resizeFramebuffer(long window, int width, int height) {
      this.onFramebufferResize(window, width, height);
   }

   @Unique
   private float calculateWidthScaleFactor() {
      return Math.max(0.125F, Math.min(8.0F, (float)this.framebufferWidth / this.width));
   }

   @Unique
   private float calculateHeightScaleFactor() {
      return Math.max(0.125F, Math.min(8.0F, (float)this.framebufferHeight / this.height));
   }

   @Inject(
      method = {"getWidth"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getWidth(CallbackInfoReturnable<Integer> cir) {
      if (EditorUI.isActive()) {
         cir.setReturnValue(EditorUI.getNewGameWidth(this.calculateWidthScaleFactor()));
      }
   }

   @Inject(
      method = {"getHeight"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getHeight(CallbackInfoReturnable<Integer> cir) {
      if (EditorUI.isActive()) {
         cir.setReturnValue(EditorUI.getNewGameHeight(this.calculateHeightScaleFactor()));
      }
   }

   @Inject(
      method = {"getScreenWidth"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getScreenWidth(CallbackInfoReturnable<Integer> cir) {
      if (EditorUI.isActive()) {
         cir.setReturnValue(EditorUI.getNewGameWidth(1.0F));
      }
   }

   @Inject(
      method = {"getScreenHeight"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getScreenHeight(CallbackInfoReturnable<Integer> cir) {
      if (EditorUI.isActive()) {
         cir.setReturnValue(EditorUI.getNewGameHeight(1.0F));
      }
   }

   @Inject(
      method = {"onResize"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onResizeInject(long l, int i, int j, CallbackInfo ci) {
      if (EditorUI.isActive()) {
         if (l != this.getWindow()) {
            ci.cancel();
         } else {
            WindowSizeTracker.dirty();
         }
      }
   }

   @Inject(
      method = {"calculateScale"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void calculateScale(int scale, boolean forceEven, CallbackInfoReturnable<Integer> cir) {
      if (EditorUI.isActive()) {
         int fbw = EditorUI.getNewGameWidth(this.calculateWidthScaleFactor());
         int fbh = EditorUI.getNewGameHeight(this.calculateHeightScaleFactor());
         int j = 1;

         while (j != scale && j < fbw && j < fbh && fbw / (j + 1) >= 320 && fbh / (j + 1) >= 240) {
            j++;
         }

         if (forceEven && j % 2 != 0) {
            j++;
         }

         cir.setReturnValue(j);
      }
   }

   @Inject(
      method = {"setGuiScale"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setGuiScale(double d, CallbackInfo ci) {
      if (EditorUI.isActive()) {
         int fbw = EditorUI.getNewGameWidth(this.calculateWidthScaleFactor());
         int fbh = EditorUI.getNewGameHeight(this.calculateHeightScaleFactor());
         this.guiScale = d;
         int i = (int)(fbw / d);
         this.guiScaledWidth = fbw / d > i ? i + 1 : i;
         int j = (int)(fbh / d);
         this.guiScaledHeight = fbh / d > j ? j + 1 : j;
         ci.cancel();
      }
   }
}
