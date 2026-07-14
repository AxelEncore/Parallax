package com.moulberry.axiom.mixin.no_physical_trigger;

import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.AxiomServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameEventDispatcher.class})
public class MixinGameEventDispatcher {
   @Inject(
      method = {"post"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void post(CallbackInfo ci, @Local(argsOnly = true) Context context) {
      if (context.sourceEntity() instanceof ServerPlayer serverPlayer && AxiomServer.isNoPhysicalTrigger(serverPlayer)) {
         ci.cancel();
      }
   }
}
