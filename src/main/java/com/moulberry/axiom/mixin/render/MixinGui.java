package com.moulberry.axiom.mixin.render;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ContextMenuManager;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.CustomImGuiImplGlfw;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.screen.SwitchHotbarScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Gui.class})
public abstract class MixinGui {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private ItemStack lastToolHighlight;
   @Shadow
   private int toolHighlightTimer;
   @Unique
   private int builderToolHighlight = -1;
   @Unique
   private int builderToolHighlightTimer = 0;

   @Shadow
   public abstract Font getFont();

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
      if (AxiomClient.isAxiomActive()) {
         int screenWidth = guiGraphics.guiWidth();
         int screenHeight = guiGraphics.guiHeight();
         ScreenRenderHook.render(guiGraphics, screenWidth, screenHeight, tickDelta);
      }
   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         if (ContextMenuManager.getInstance().isActive()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"canRenderCrosshairForSpectator"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void canRenderCrosshairForSpectator(HitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
      if (EditorUI.isActive() && EditorUI.imguiGlfw.getMouseHandledBy() == CustomImGuiImplGlfw.MouseHandledBy.GAME) {
         cir.setReturnValue(true);
      }
   }

   @WrapWithCondition(
      method = {"renderItemHotbar"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
         ordinal = 1
      )}
   )
   public boolean blitSelectedHotbar(GuiGraphics instance, ResourceLocation resourceLocation, int i, int j, int k, int l) {
      return !BuilderToolManager.isToolSlotActive() && !DisplayEntityManipulator.hasActiveGizmo() && !MarkerEntityManipulator.hasActiveGizmo();
   }

   @Inject(
      method = {"renderItemHotbar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
      if (AxiomClient.isAxiomActive()) {
         if (EditorUI.isActive()) {
            ci.cancel();
         } else {
            if (ContextMenuManager.getInstance().getActiveScreen() instanceof SwitchHotbarScreen) {
               ci.cancel();
            } else {
               int screenWidth = guiGraphics.guiWidth();
               int screenHeight = guiGraphics.guiHeight();
               ScreenRenderHook.renderHotbar(guiGraphics, screenWidth, screenHeight, tickDelta);
            }
         }
      }
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("RETURN")}
   )
   public void tick(CallbackInfo ci) {
      if (BuilderToolManager.isToolSlotActive()) {
         this.lastToolHighlight = ItemStack.EMPTY;
         this.toolHighlightTimer = 0;
         int selected = BuilderToolManager.getToolSlotSelected();
         if (this.builderToolHighlight != selected) {
            this.builderToolHighlight = selected;
            this.builderToolHighlightTimer = (int)(40.0 * (Double)(Object)this.minecraft.options.notificationDisplayTime().get());
         }

         if (this.builderToolHighlightTimer > 0) {
            this.builderToolHighlightTimer--;
         }
      } else {
         this.builderToolHighlight = -1;
         this.builderToolHighlightTimer = 0;
      }
   }

   @Inject(
      method = {"renderSelectedItemName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
      if (!EditorUI.isActive() && !DisplayEntityManipulator.hasActiveGizmo() && !MarkerEntityManipulator.hasActiveGizmo()) {
         if (BuilderToolManager.isToolSlotActive()) {
            ci.cancel();
            if (this.builderToolHighlightTimer > 0 && this.builderToolHighlight >= 0) {
               int screenWidth = guiGraphics.guiWidth();
               int screenHeight = guiGraphics.guiHeight();
               Component component = Component.literal(BuilderToolManager.getToolName(this.builderToolHighlight));
               int width = this.getFont().width(component);
               int x = (screenWidth - width) / 2;
               int y = screenHeight - 59;
               if (!this.minecraft.gameMode.canHurtPlayer()) {
                  y += 14;
               }

               int opacity = (int)(this.builderToolHighlightTimer * 256.0F / 10.0F);
               if (opacity > 255) {
                  opacity = 255;
               }

               if (opacity > 0) {
                  guiGraphics.fill(x - 2, y - 2, x + width + 2, y + 9 + 2, this.minecraft.options.getBackgroundColor(0));
                  guiGraphics.drawString(this.getFont(), component, x, y, 16777215 | opacity << 24);
               }
            }
         }
      } else {
         ci.cancel();
      }
   }
}
