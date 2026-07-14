package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.configuration.FlightDirection;
import com.moulberry.axiom.editor.EditorUI;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class EnhancedFlight {
   public static float getFlightSpeed(LocalPlayer player) {
      AxiomConfig.SubcategoryMovement config = Axiom.configuration.movement;
      double momentum = EditorUI.isActive() && !config.syncIngameMovementWithEditorUI ? 0.0 : Math.cbrt(config.flightMomentum / 100.0F);
      double baseMult = (1.0 - momentum * 0.91) / 0.08999999999999997;
      float sprintMultiplier = !Minecraft.getInstance().options.keySprint.isDown() && !player.isSprinting() ? 1.0F : 2.0F;
      return (float)(player.getAbilities().getFlyingSpeed() * sprintMultiplier * baseMult);
   }

   public static void doFlight(LocalPlayer player, Vec3 movementInput, float flyingSpeed, Consumer<Vec3> superTravel) {
      AxiomConfig.SubcategoryMovement config = Axiom.configuration.movement;
      boolean cameraDirection = EditorUI.isActive() && !config.syncIngameMovementWithEditorUI || config.flightDirection == FlightDirection.CAMERA;
      boolean originalHasNoGravity = player.isNoGravity();
      double oldX = player.getX();
      double oldY = player.getY();
      double oldZ = player.getZ();
      if (cameraDirection) {
         double sin = -Math.sin(Math.toRadians(player.getXRot()));
         double cos = Math.cos(Math.toRadians(player.getXRot()));
         movementInput = new Vec3(movementInput.x, sin * movementInput.z, cos * movementInput.z);
         player.setNoGravity(true);
      }

      float movementY = 0.0F;
      if (player.input.shiftKeyDown) {
         movementY--;
      }

      if (player.input.jumping) {
         movementY++;
      }

      if (movementY != 0.0F) {
         player.move(MoverType.SELF, new Vec3(0.0, flyingSpeed * movementY * 0.98, 0.0));
         if (movementY < 0.0F && !VersionUtils.isAlwaysFlying(Minecraft.getInstance().gameMode)) {
            double expectedY = oldY + flyingSpeed * movementY * 0.98;
            if (Math.abs(player.getY() - expectedY) > 1.0E-5) {
               player.setOnGround(true);
               player.getAbilities().flying = false;
               player.onUpdateAbilities();
            }
         }
      }

      double beforeY = player.getDeltaMovement().y;
      superTravel.accept(movementInput);
      double momentum = EditorUI.isActive() && !config.syncIngameMovementWithEditorUI ? 0.0 : Math.cbrt(config.flightMomentum / 100.0F);
      Vec3 deltaMovement = player.getDeltaMovement();
      if (cameraDirection) {
         player.setDeltaMovement(deltaMovement.x * momentum, deltaMovement.y * 0.92857 * momentum, deltaMovement.z * momentum);
      } else {
         player.setDeltaMovement(deltaMovement.x * momentum, beforeY * 0.6 * momentum, deltaMovement.z * momentum);
      }

      if (cameraDirection && movementY >= 0.0F) {
         player.setOnGround(false);
      }

      player.setNoGravity(originalHasNoGravity);
      player.resetFallDistance();
      if (player.isFallFlying()) {
         player.stopFallFlying();
      }
   }
}
