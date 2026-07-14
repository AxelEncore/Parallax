package com.moulberry.axiom.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.utils.FramebufferUtils;
import com.moulberry.axiom.utils.WindowSizeTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {RenderTarget.class},
   priority = 800
)
public abstract class MixinRenderTarget {
   @Inject(
      method = {"blitToScreen(IIZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void blitToScreen(CallbackInfo ci) {
      if ((Object)this == Minecraft.getInstance().getMainRenderTarget() && EditorUI.isActive() && EditorUI.frameWidth > 1 && EditorUI.frameHeight > 1) {
         Window window = Minecraft.getInstance().getWindow();
         float frameTop = (float)EditorUI.frameY / EditorUI.viewportSizeY;
         float frameLeft = (float)EditorUI.frameX / EditorUI.viewportSizeX;
         float frameWidth = (float)Math.max(1, EditorUI.frameWidth) / EditorUI.viewportSizeX;
         float frameHeight = (float)Math.max(1, EditorUI.frameHeight) / EditorUI.viewportSizeY;
         int framebufferWidth = WindowSizeTracker.getWidth(window);
         int framebufferHeight = WindowSizeTracker.getHeight(window);
         FramebufferUtils.blitToScreenPartial(
            (RenderTarget)(Object)this, framebufferWidth, framebufferHeight, frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight
         );
         ci.cancel();
      }
   }
}
