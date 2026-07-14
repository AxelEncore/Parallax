package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public class MixinItemInHandRenderer {
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   private float mainHandHeight;

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
      )}
   )
   public ItemStack getMainHandItem(LocalPlayer localPlayer, Operation<ItemStack> original) {
      boolean noHeldItem = BuilderToolManager.isToolSlotActive() || DisplayEntityManipulator.hasActiveGizmo() || MarkerEntityManipulator.hasActiveGizmo();
      return noHeldItem ? ItemStack.EMPTY : (ItemStack)original.call(new Object[]{localPlayer});
   }

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   public void tickEnd(CallbackInfo ci) {
      boolean noHeldItem = BuilderToolManager.isToolSlotActive() || DisplayEntityManipulator.hasActiveGizmo() || MarkerEntityManipulator.hasActiveGizmo();
      if (noHeldItem && this.mainHandItem.isEmpty()) {
         this.mainHandHeight = 0.0F;
      }
   }
}
