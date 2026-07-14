package com.moulberry.axiom.mixin.no_physical_trigger;

import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.capabilities.Capability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FarmBlock.class, TurtleEggBlock.class})
public class MixinCancelFallOn {
   @Inject(
      method = {"fallOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void fallOn(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true) Entity entity) {
      if (level.isClientSide) {
         if (Capability.PHANTOM.isEnabled()) {
            ci.cancel();
         }
      } else if (entity instanceof ServerPlayer serverPlayer && AxiomServer.isNoPhysicalTrigger(serverPlayer)) {
         ci.cancel();
      }
   }
}
