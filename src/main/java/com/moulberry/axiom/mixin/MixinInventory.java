package com.moulberry.axiom.mixin;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.HotbarManager;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Inventory.class})
public class MixinInventory {
   @Shadow
   public int selected;

   @Inject(
      method = {"swapPaint"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void swapPaintHead(double d, CallbackInfo ci) {
      if (EditorUI.isActive()) {
         ci.cancel();
      }

      int dir = (int)Math.signum(d);
      int newSelected = this.selected - dir;
      if (newSelected < 0) {
         newSelected += 9;
      }

      if (newSelected >= 9) {
         newSelected -= 9;
      }

      int overrideSlot = BuilderToolManager.scroll(this.selected, newSelected, dir);
      if (overrideSlot >= 0) {
         this.selected = overrideSlot;
         ci.cancel();
      }
   }

   @WrapOperation(
      method = {"setPickedItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"
      )}
   )
   public int setPickedItem(Inventory instance, ItemStack itemStack, Operation<Integer> original) {
      int slot = (Integer)original.call(new Object[]{instance, itemStack});
      if (Axiom.configuration.contextMenu.autoSwapToOtherHotbarWithItem && slot == -1) {
         int matchingHotbar = HotbarManager.findHotbarWithMatchingItem(itemStack);
         if (matchingHotbar >= 0) {
            ScreenRenderHook.setOverlayText(Component.literal(AxiomI18n.get("axiom.hardcoded.switched_to_hotbar") + (matchingHotbar + 1)));
            HotbarManager.setActiveHotbarIndex(matchingHotbar);
            slot = (Integer)original.call(new Object[]{instance, itemStack});
         }
      }

      return slot;
   }
}
