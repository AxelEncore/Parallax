package com.moulberry.axiom.mixin;

import com.moulberry.axiom.hooks.ConnectionProtocolExt;
import net.minecraft.network.ConnectionProtocol;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ConnectionProtocol.class})
public class MixinConnectionProtocol implements ConnectionProtocolExt {
   @Override
   public int axiom$getCustomPacketPayloadId() {
      throw new UnsupportedOperationException();
   }
}
