package com.moulberry.axiom.mixin;

import com.moulberry.axiom.capabilities.ArcballCamera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public class MixinEntityClient {
   @Inject(
      method = {"turn"},
      at = {@At("RETURN")}
   )
   public void onTurn(double d, double e, CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer localPlayer && !localPlayer.isPassenger()) {
         ArcballCamera.handlePlayerTurn(localPlayer);
      }
   }
}
