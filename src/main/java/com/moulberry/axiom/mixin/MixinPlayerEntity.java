package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.capabilities.ArcballCamera;
import com.moulberry.axiom.capabilities.EnhancedFlight;
import com.moulberry.axiom.capabilities.NoClip;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.configuration.FlightDirection;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class MixinPlayerEntity extends LivingEntity {
   protected MixinPlayerEntity() {
      super(null, null);
   }

   @Shadow
   public abstract void travel(Vec3 var1);

   @Shadow
   protected abstract float getFlyingSpeed();

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z",
         ordinal = 0
      )}
   )
   public boolean tick_isSpectator(Player instance, Operation<Boolean> original) {
      return (Object)this instanceof LocalPlayer player && NoClip.canNoClip(player) ? true : (Boolean)original.call(new Object[]{instance});
   }

   @Inject(
      method = {"updatePlayerPose"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void updatePlayerPose(CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer player && NoClip.canNoClip(player)) {
         this.setPose(Pose.STANDING);
         ci.cancel();
      }
   }

   @Inject(
      method = {"getFlyingSpeed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getFlyingSpeed(CallbackInfoReturnable<Float> cir) {
      if ((Object)this instanceof LocalPlayer player) {
         if (!AxiomClient.isAxiomActive()) {
            return;
         }

         AxiomConfig.SubcategoryMovement config = Axiom.configuration.movement;
         boolean doAirplaneFlight = EditorUI.isActive() && !config.syncIngameMovementWithEditorUI
            || config.flightDirection != FlightDirection.HORIZONTAL
            || config.flightMomentum < 98;
         if (doAirplaneFlight && player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying()) {
            cir.setReturnValue(EnhancedFlight.getFlightSpeed(player));
         }
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void travel(Vec3 movementInput, CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer player) {
         if (!AxiomClient.isAxiomActive()) {
            return;
         }

         if (ArcballCamera.isLocked()) {
            ci.cancel();
            return;
         }

         AxiomConfig.SubcategoryMovement config = Axiom.configuration.movement;
         boolean doAirplaneFlight = EditorUI.isActive() && !config.syncIngameMovementWithEditorUI
            || config.flightDirection != FlightDirection.HORIZONTAL
            || config.flightMomentum < 98;
         if (doAirplaneFlight && player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying()) {
            EnhancedFlight.doFlight(player, movementInput, this.getFlyingSpeed(), x$0 -> super.travel(x$0));
            ci.cancel();
         }
      }
   }
}
