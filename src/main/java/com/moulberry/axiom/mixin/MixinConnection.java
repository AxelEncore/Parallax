package com.moulberry.axiom.mixin;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipelineException;
import io.netty.channel.ConnectTimeoutException;
import java.nio.channels.ClosedChannelException;
import net.minecraft.network.Connection;
import net.minecraft.network.SkipPacketException;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Connection.class})
public abstract class MixinConnection {
   @Shadow
   @Final
   private PacketFlow receiving;
   @Unique
   private static long lastStackTrace = 0L;

   @Shadow
   public abstract boolean isConnected();

   @Inject(
      method = {"disconnect(Lnet/minecraft/network/DisconnectionDetails;)V"},
      at = {@At("HEAD")}
   )
   public void beforeDisconnect(CallbackInfo ci) {
      if (this.receiving == PacketFlow.CLIENTBOUND && this.isConnected()) {
         ClientEvents.beforeDisconnect();
      }
   }

   @Inject(
      method = {"exceptionCaught"},
      at = {@At("HEAD")}
   )
   public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable, CallbackInfo ci) {
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         throwable = ignoreOrUnwrapException(throwable);
         if (throwable == null) {
            return;
         }

         long time = System.currentTimeMillis();
         long delta = time - lastStackTrace;
         if (delta < 0L || delta > 1000L) {
            lastStackTrace = time;
            Axiom.LOGGER.error("Exception in connection. Logging for debug purposes", throwable);
         }
      }
   }

   @Unique
   @Nullable
   private static Throwable ignoreOrUnwrapException(Throwable e) {
      if (e instanceof SkipPacketException || e instanceof ClosedChannelException) {
         return null;
      } else {
         return !(e instanceof ChannelException) && !(e instanceof ConnectTimeoutException) && !(e instanceof ChannelPipelineException)
            ? e
            : ignoreOrUnwrapException(e.getCause());
      }
   }
}
