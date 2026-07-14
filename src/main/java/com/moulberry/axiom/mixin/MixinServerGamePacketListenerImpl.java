package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ServerGamePacketListenerImpl.class})
public class MixinServerGamePacketListenerImpl {
   @Shadow
   public ServerPlayer player;

   @WrapOperation(
      method = {"handleMovePlayer"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/server/level/ServerPlayer;noPhysics:Z"
      )}
   )
   public boolean handleMovePlayer_noPhysics(ServerPlayer instance, Operation<Boolean> original) {
      return AxiomServer.canUseAxiom(this.player, AxiomPermission.PLAYER_BYPASS_MOVEMENT_RESTRICTIONS) && instance.getAbilities().flying
         ? true
         : (Boolean)original.call(new Object[]{instance});
   }

   @WrapOperation(
      method = {"handleMovePlayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;isSingleplayerOwner()Z"
      )}
   )
   public boolean handleMovePlayerIsSingleplayerOwner(ServerGamePacketListenerImpl instance, Operation<Boolean> original) {
      return AxiomServer.canUseAxiom(this.player, AxiomPermission.PLAYER_BYPASS_MOVEMENT_RESTRICTIONS)
         ? true
         : (Boolean)original.call(new Object[]{instance});
   }
}
