package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.hooks.ServerGamePacketListenerImplExt;
import com.moulberry.axiom.packets.AxiomClientboundAckWorldProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerGamePacketListenerImpl.class})
public abstract class MixinServerGamePacketListenerImpl implements ServerGamePacketListenerImplExt {
   @Shadow
   public ServerPlayer player;
   @Unique
   private int ackWorldPropertiesUpTo = -1;

   @Override
   public void ackWorldPropertiesUpTo(int updateId) {
      this.ackWorldPropertiesUpTo = Math.max(this.ackWorldPropertiesUpTo, updateId);
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      if (this.ackWorldPropertiesUpTo > -1) {
         new AxiomClientboundAckWorldProperties(this.ackWorldPropertiesUpTo).send(this.player);
         this.ackWorldPropertiesUpTo = -1;
      }
   }
}
