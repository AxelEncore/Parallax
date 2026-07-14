package com.moulberry.axiom.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.keybinds.KeybindCategory;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.kyrptonaught.cmdkeybind.MacroTypes.BaseMacro;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@IfModLoaded("cmdkeybind")
@Mixin({BaseMacro.class})
public class MixinBaseMacro {
   @Shadow
   @Final
   private Key modifierKey;
   @Shadow
   @Final
   private Key primaryKey;

   @ModifyReturnValue(
      method = {"isTriggered"},
      remap = false,
      at = {@At("RETURN")}
   )
   public boolean isTriggered(boolean triggered) {
      if (triggered && AxiomClient.isAxiomActive() && EditorUI.isActive()) {
         boolean shiftMod = this.modifierKey.getValue() == 340 || this.modifierKey.getValue() == 344;
         boolean ctrlMod = this.modifierKey.getValue() == 341 || this.modifierKey.getValue() == 345;
         boolean altMod = this.modifierKey.getValue() == 342 || this.modifierKey.getValue() == 346;
         boolean superMod = this.modifierKey.getValue() == 343 || this.modifierKey.getValue() == 347;

         for (KeybindCategory category : Keybinds.categories) {
            if (category.preventPassToGame()) {
               for (Keybind keybind : category.keybinds()) {
                  if (keybind.wouldBePressed(this.primaryKey.getValue(), shiftMod, ctrlMod, altMod, superMod)) {
                     return false;
                  }
               }
            }
         }
      }

      return triggered;
   }
}
