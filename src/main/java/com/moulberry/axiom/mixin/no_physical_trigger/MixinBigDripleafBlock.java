package com.moulberry.axiom.mixin.no_physical_trigger;

import com.moulberry.axiom.AxiomServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BigDripleafBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BigDripleafBlock.class})
public class MixinBigDripleafBlock {
   @Inject(
      method = {"canEntityTilt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void canEntityTilt(BlockPos blockPos, Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof ServerPlayer serverPlayer && AxiomServer.isNoPhysicalTrigger(serverPlayer)) {
         cir.setReturnValue(false);
      }
   }
}
