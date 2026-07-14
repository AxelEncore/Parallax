package com.moulberry.axiom.mixin;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({KeyMapping.class})
public class MixinKeyMapping {
   @Inject(
      method = {"isDown"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isDown(CallbackInfoReturnable<Boolean> cir) {
      if (EditorUI.isActive()) {
         if (EditorUI.getIO().getWantTextInput()) {
            cir.setReturnValue(false);
            return;
         }

         KeyMapping thisMapping = (KeyMapping)(Object)this;
         Type type = thisMapping.key.getType();
         int value = thisMapping.key.getValue();
         if (Keybinds.hasShiftCtrl
            && type == Type.KEYSYM
            && (value == 340 || value == 344)
            && VersionUtilsClient.wasteFirstBoolean(Minecraft.getInstance(), Screen.hasControlDown())) {
            cir.setReturnValue(false);
            return;
         }

         if (Keybinds.hasShiftAlt
            && type == Type.KEYSYM
            && (value == 340 || value == 344)
            && VersionUtilsClient.wasteFirstBoolean(Minecraft.getInstance(), Screen.hasAltDown())) {
            cir.setReturnValue(false);
            return;
         }

         Options options = Minecraft.getInstance().options;
         if (!Keybinds.useVanillaMovement) {
            if (thisMapping == options.keySprint) {
               cir.setReturnValue(Keybinds.MOVE_QUICK.isDownIgnoreMods());
            } else if (thisMapping == options.keyUp) {
               cir.setReturnValue(Keybinds.MOVE_FORWARD.isDownIgnoreMods());
            } else if (thisMapping == options.keyLeft) {
               cir.setReturnValue(Keybinds.MOVE_LEFT.isDownIgnoreMods());
            } else if (thisMapping == options.keyDown) {
               cir.setReturnValue(Keybinds.MOVE_BACKWARD.isDownIgnoreMods());
            } else if (thisMapping == options.keyRight) {
               cir.setReturnValue(Keybinds.MOVE_RIGHT.isDownIgnoreMods());
            } else if (thisMapping == options.keyJump) {
               cir.setReturnValue(Keybinds.MOVE_UP.isDownIgnoreMods());
            } else if (thisMapping == options.keyShift) {
               cir.setReturnValue(Keybinds.MOVE_DOWN.isDownIgnoreMods());
            }
         }
      }
   }

   @Inject(
      method = {"consumeClick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isPressed(CallbackInfoReturnable<Boolean> cir) {
      if (EditorUI.isActive()) {
         Options options = Minecraft.getInstance().options;
         if (!Keybinds.useVanillaMovement) {
            if ((Object)this == options.keySprint) {
               cir.setReturnValue(Keybinds.MOVE_QUICK.isPressedIgnoreMods(false));
            } else if ((Object)this == options.keyUp) {
               cir.setReturnValue(Keybinds.MOVE_FORWARD.isPressedIgnoreMods(false));
            } else if ((Object)this == options.keyLeft) {
               cir.setReturnValue(Keybinds.MOVE_LEFT.isPressedIgnoreMods(false));
            } else if ((Object)this == options.keyDown) {
               cir.setReturnValue(Keybinds.MOVE_BACKWARD.isPressedIgnoreMods(false));
            } else if ((Object)this == options.keyRight) {
               cir.setReturnValue(Keybinds.MOVE_RIGHT.isPressedIgnoreMods(false));
            } else if ((Object)this == options.keyJump) {
               cir.setReturnValue(Keybinds.MOVE_UP.isPressedIgnoreMods(false));
            } else if ((Object)this == options.keyShift) {
               cir.setReturnValue(Keybinds.MOVE_DOWN.isPressedIgnoreMods(false));
            }
         }
      }
   }
}
