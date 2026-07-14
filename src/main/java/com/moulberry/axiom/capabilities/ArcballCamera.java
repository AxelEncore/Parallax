package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class ArcballCamera {
   private static Vec3 target = null;
   private static float targetDistance = 0.0F;

   public static void handlePlayerTravel(LocalPlayer localPlayer) {
      if (isLocked()) {
         if (!allowCameraLock(localPlayer)) {
            target = null;
            return;
         }

         targetDistance = (float)target.distanceTo(localPlayer.getEyePosition());
         lookAt(localPlayer, target);
         localPlayer.setOldPosAndRot();
      }
   }

   public static void handlePlayerTurn(LocalPlayer localPlayer) {
      if (isLocked()) {
         if (!allowCameraLock(localPlayer)) {
            target = null;
            return;
         }

         updatePosition(localPlayer);
         localPlayer.setOldPosAndRot();
      }
   }

   public static void addTargetDistance(int delta) {
      if (isLocked()) {
         if (!allowCameraLock(Minecraft.getInstance().player)) {
            target = null;
            return;
         }

         if (delta != 0) {
            float wheel = Math.signum((float)delta);
            float move = targetDistance * 0.05F;
            if (move > 4.0F) {
               move = 4.0F + (float)Math.sqrt(move - 4.0F);
            }

            move *= wheel;
            if (EditorUI.isMoveQuickDown()) {
               move *= 2.0F;
            }

            targetDistance = Math.max(1.0F, targetDistance - move);
         }

         updatePosition(Minecraft.getInstance().player);
      }
   }

   private static void updatePosition(LocalPlayer localPlayer) {
      Vec3 look = localPlayer.getLookAngle();
      Vec3 position = target.subtract(look.scale(targetDistance));
      Vec3 delta = position.subtract(localPlayer.getEyePosition());
      localPlayer.move(MoverType.SELF, delta);
      lookAt(localPlayer, target);
      localPlayer.setDeltaMovement(Vec3.ZERO);
   }

   public static void lock(Vec3 target, float distance) {
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (!isLocked() && allowCameraLock(localPlayer)) {
         ArcballCamera.target = target;
         targetDistance = distance;
         lookAt(localPlayer, target);
      }
   }

   private static void lookAt(LocalPlayer localPlayer, Vec3 target) {
      float yRot = localPlayer.getYRot();
      localPlayer.lookAt(Anchor.EYES, target);
      if (Math.cos(Math.toRadians(localPlayer.getXRot())) < 1.0E-5) {
         localPlayer.setYRot(yRot);
      } else {
         float clampedDelta = Mth.wrapDegrees(localPlayer.getYRot() - yRot);
         localPlayer.setYRot(yRot + clampedDelta);
      }
   }

   public static void unlock() {
      target = null;
   }

   public static boolean isLocked() {
      return target != null;
   }

   private static boolean allowCameraLock(LocalPlayer localPlayer) {
      return localPlayer != null && localPlayer.getAbilities().mayfly;
   }
}
