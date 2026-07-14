package com.moulberry.axiom.mixin;

import com.moulberry.axiom.AxiomServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class MixinEntityServer {
   @Inject(
      method = {"isIgnoringBlockTriggers"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isIgnoringBlockTriggers(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this instanceof ServerPlayer serverPlayer && AxiomServer.isNoPhysicalTrigger(serverPlayer)) {
         cir.setReturnValue(true);
      }
   }
}
