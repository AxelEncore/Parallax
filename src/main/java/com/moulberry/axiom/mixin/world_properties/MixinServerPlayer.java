package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.hooks.ServerPlayerExt;
import com.moulberry.axiom.world_properties.AxiomGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules.BooleanValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayer.class})
public abstract class MixinServerPlayer implements ServerPlayerExt {
   @Unique
   private boolean noPhysicalTrigger = false;

   @Shadow
   public abstract ServerLevel serverLevel();

   @Inject(
      method = {"isInvulnerableTo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isInvulnerableTo(CallbackInfoReturnable<Boolean> cir) {
      ServerLevel level = this.serverLevel();
      if (level != null && ((BooleanValue)level.getGameRules().getRule(AxiomGameRules.RULE_PLAYERINVULNERABILITY)).get()) {
         cir.setReturnValue(true);
      }
   }

   @Override
   public void axiom$setNoPhysicalTrigger(boolean noPhysicalTrigger) {
      this.noPhysicalTrigger = noPhysicalTrigger;
   }

   @Override
   public boolean axiom$isNoPhysicalTrigger() {
      return this.noPhysicalTrigger;
   }
}
