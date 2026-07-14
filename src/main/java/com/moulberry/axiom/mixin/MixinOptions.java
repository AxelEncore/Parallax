package com.moulberry.axiom.mixin;

import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Options.class})
public class MixinOptions {
   @Inject(
      method = {"getCameraType"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getCameraType(CallbackInfoReturnable<CameraType> cir) {
      if (EditorUI.isActive()) {
         cir.setReturnValue(CameraType.FIRST_PERSON);
      }
   }
}
