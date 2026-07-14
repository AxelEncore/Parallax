package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.AxiomClient;
import net.minecraft.client.player.LocalPlayer;

public class NoClip {
   public static boolean canNoClip(LocalPlayer player) {
      if (player == null || !AxiomClient.isAxiomActive()) {
         return false;
      } else {
         return !Capability.NO_CLIP.isEnabled()
            ? false
            : player.getAbilities().flying && !player.isPassenger() && !player.isFallFlying() && !player.isSleeping() && !player.isSwimming();
      }
   }
}
