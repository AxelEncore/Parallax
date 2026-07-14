package com.moulberry.axiom.mixin.no_physical_trigger;

import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.capabilities.Capability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RedStoneOreBlock.class, SculkSensorBlock.class, SculkShriekerBlock.class, TurtleEggBlock.class})
public class MixinCancelStepOn {
   @Inject(
      method = {"stepOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void stepOn(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true) Entity entity) {
      if (level.isClientSide) {
         if (Capability.PHANTOM.isEnabled()) {
            ci.cancel();
         }
      } else if (entity instanceof ServerPlayer serverPlayer && AxiomServer.isNoPhysicalTrigger(serverPlayer)) {
         ci.cancel();
      }
   }
}
