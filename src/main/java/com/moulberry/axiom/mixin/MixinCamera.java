package com.moulberry.axiom.mixin;

import com.moulberry.axiom.capabilities.NoClip;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public class MixinCamera {
   @Inject(
      method = {"getMaxZoom"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getMaxZoom(float d, CallbackInfoReturnable<Float> cir) {
      if (NoClip.canNoClip(Minecraft.getInstance().player)) {
         cir.setReturnValue(d);
      }
   }
}
