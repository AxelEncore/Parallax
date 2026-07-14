package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.mixin.MultiPlayerGameModeAccessor;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Bulldozer {
   private static int instabreakCountdown;
   private static Vec3 lastView;

   public static void resetInstabreakCountdown() {
      instabreakCountdown = 10;
   }

   public static void increaseInstabreakCountdown() {
      if (instabreakCountdown < 10) {
         instabreakCountdown++;
      }
   }

   public static void setLastView(Vec3 lastView) {
      Bulldozer.lastView = lastView;
   }

   public static boolean handleInstabreak(ClientLevel level, LocalPlayer localPlayer, MultiPlayerGameMode gameMode) {
      int infiniteReachLimit = ClientRestrictions.getInfiniteReachLimit();
      if (instabreakCountdown > 0 || lastView == null || Capability.INFINITE_REACH.isEnabled() && (infiniteReachLimit < 0 || infiniteReachLimit > 16)) {
         int delay = ((MultiPlayerGameModeAccessor)gameMode).getDestroyDelay();
         int newDelay = Math.max(0, delay - 5 + instabreakCountdown / 2);
         ((MultiPlayerGameModeAccessor)gameMode).setDestroyDelay(newDelay);
         if (instabreakCountdown > 0) {
            instabreakCountdown--;
         }

         return false;
      } else {
         float range = (float)localPlayer.blockInteractionRange();
         if (Capability.INFINITE_REACH.isEnabled() && infiniteReachLimit > range) {
            range = infiniteReachLimit;
         }

         boolean swung = false;

         for (int i = 0; i < 100; i++) {
            Vec3 eye = localPlayer.getEyePosition(i / 100.0F);
            Vec3 view = lastView.lerp(localPlayer.getViewVector(1.0F), i / 100.0F);
            Vec3 end = eye.add(view.x * range, view.y * range, view.z * range);
            BlockHitResult hitResult = level.clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, localPlayer));
            if (hitResult.getType() == Type.BLOCK) {
               ((MultiPlayerGameModeAccessor)gameMode).setDestroyDelay(0);
               gameMode.continueDestroyBlock(hitResult.getBlockPos(), hitResult.getDirection());
               if (!swung) {
                  swung = true;
                  localPlayer.swing(InteractionHand.MAIN_HAND);
               }
            }
         }

         return true;
      }
   }
}
