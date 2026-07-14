package com.moulberry.axiom.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.GeneralGameFeatures;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.GameRendererExt;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class MixinGameRenderer implements GameRendererExt {
   @Shadow
   @Final
   Minecraft minecraft;

   @Shadow
   public abstract void resetData();

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/Options;pauseOnLostFocus:Z",
         opcode = 180
      )}
   )
   public boolean getPauseOnLostFocus(Options instance, Operation<Boolean> original) {
      return !EditorUI.isActive() && (Boolean)original.call(new Object[]{instance});
   }

   @Inject(
      method = {"renderItemInHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderItemInHand(CallbackInfo ci) {
      if (EditorUI.isActive()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"pick(F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pickHead(float f, CallbackInfo ci) {
      GeneralGameFeatures.injectPickHead(this.minecraft, f, ci);
   }

   @Inject(
      method = {"pick(F)V"},
      at = {@At("RETURN")}
   )
   public void pickReturn(float f, CallbackInfo ci) {
      GeneralGameFeatures.injectPickReturn(this.minecraft, f, ci);
   }

   @Inject(
      method = {"shouldRenderBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
      if (EditorUI.isActive()) {
         ReplaceMode.releaseChunkRenderOverrider();
         cir.setReturnValue(false);
      } else if (DisplayEntityManipulator.hoveredGizmoPart()) {
         cir.setReturnValue(false);
      } else if (MarkerEntityManipulator.hoveredGizmoPart()) {
         cir.setReturnValue(false);
      }

      if (BuilderToolManager.isToolSlotActive()) {
         ReplaceMode.releaseChunkRenderOverrider();
         HitResult hitResult = this.minecraft.hitResult;
         cir.setReturnValue(
            hitResult != null && hitResult.getType() == Type.BLOCK && BuilderToolManager.shouldRenderBlockOutline(((BlockHitResult)hitResult).getBlockPos())
         );
      }
   }
}
