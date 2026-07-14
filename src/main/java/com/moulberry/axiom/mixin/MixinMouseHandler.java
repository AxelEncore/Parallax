package com.moulberry.axiom.mixin;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.ContextMenuManager;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.capabilities.ArcballCamera;
import com.moulberry.axiom.editor.CustomImGuiImplGlfw;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MouseHandler.class})
public abstract class MixinMouseHandler {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private int activeButton;
   @Shadow
   private double mousePressedTime;
   @Shadow
   private double xpos;
   @Shadow
   private double ypos;
   @Unique
   private double accumulatedXScroll;
   @Unique
   private double accumulatedYScroll;

   @Inject(
      method = {"onPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onPress(long window, int button, int action, int mods, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         ContextMenuManager context = ContextMenuManager.getInstance();
         if (context.isActive()) {
            boolean clicked = action == 1;
            Key contextKey = ClientEvents.contextMenuKeyBind.key;
            if (contextKey.getType() != Type.MOUSE || contextKey.getValue() != button) {
               double mouseX = this.xpos * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
               double mouseY = this.ypos * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
               if (clicked) {
                  this.mousePressedTime = Blaze3D.getTime();
                  this.activeButton = button;
                  context.mouseClicked(mouseX, mouseY, button);
               } else {
                  context.mouseReleased(mouseX, mouseY, button);
                  this.activeButton = -1;
               }

               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"onScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onScroll(long window, double xScroll, double yScroll, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive() && Minecraft.getInstance().screen == null) {
         double yScrollWithSens = (this.minecraft.options.discreteMouseScroll().get() ? Math.signum(yScroll) : yScroll)
            * (Double)(Object)this.minecraft.options.mouseWheelSensitivity().get();
         double xScrollWithSens = (this.minecraft.options.discreteMouseScroll().get() ? Math.signum(xScroll) : xScroll)
            * (Double)(Object)this.minecraft.options.mouseWheelSensitivity().get();
         if (this.accumulatedYScroll != 0.0 && Math.signum(yScrollWithSens) != Math.signum(this.accumulatedYScroll)) {
            this.accumulatedYScroll = 0.0;
         }

         this.accumulatedYScroll += yScrollWithSens;
         int yScrollI = (int)this.accumulatedYScroll;
         this.accumulatedYScroll -= yScrollI;
         if (this.accumulatedXScroll != 0.0 && Math.signum(xScrollWithSens) != Math.signum(this.accumulatedXScroll)) {
            this.accumulatedXScroll = 0.0;
         }

         this.accumulatedXScroll += xScrollWithSens;
         int xScrollI = (int)this.accumulatedXScroll;
         this.accumulatedXScroll -= xScrollI;
         if (xScrollI != 0 || yScrollI != 0) {
            if (ArcballCamera.isLocked() && yScrollI != 0) {
               ArcballCamera.addTargetDistance(yScrollI);
               ci.cancel();
               return;
            }

            if (ContextMenuManager.getInstance().isActive()) {
               double mx = this.xpos * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
               double my = this.ypos * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
               ContextMenuManager.getInstance().mouseScrolled(mx, my, xScrollI, yScrollI);
               ci.cancel();
               return;
            }

            UserAction.ScrollAmount scrollAmount = new UserAction.ScrollAmount(xScrollI, yScrollI);
            if (UserAction.SCROLL.call(scrollAmount) == UserAction.ActionResult.USED_STOP) {
               ci.cancel();
               return;
            }
         }

         if (EditorUI.isActive()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"onMove"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onMove(long window, double x, double y, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         ContextMenuManager context = ContextMenuManager.getInstance();
         if (context.isActive()) {
            if (window != Minecraft.getInstance().getWindow().getWindow()) {
               return;
            }

            double mouseX = x * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
            double mouseY = y * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
            context.mouseMoved(mouseX, mouseY);
            boolean pressed = this.activeButton != -1 && this.mousePressedTime > 0.0;
            if (pressed) {
               double deltaX = (x - this.xpos) * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getScreenWidth();
               double deltaY = (y - this.ypos) * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getScreenHeight();
               context.mouseDragged(mouseX, mouseY, this.activeButton, deltaX, deltaY);
            }

            this.xpos = x;
            this.ypos = y;
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"isMouseGrabbed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isMouseGrabbed(CallbackInfoReturnable<Boolean> cir) {
      if (EditorUI.isActive() && EditorUI.imguiGlfw.getMouseHandledBy() != CustomImGuiImplGlfw.MouseHandledBy.GAME) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"grabMouse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void grabMouse(CallbackInfo ci) {
      if (EditorUI.isActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"releaseMouse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void releaseMouse(CallbackInfo ci) {
      if (EditorUI.isActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"turnPlayer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onTurnPlayer(CallbackInfo ci) {
      if (Minecraft.getInstance().player == null) {
         ci.cancel();
      }
   }
}
