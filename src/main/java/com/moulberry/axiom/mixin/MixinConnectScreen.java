package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.ClientEvents;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ConnectScreen.class})
public class MixinConnectScreen {
   @Inject(
      method = {"startConnecting"},
      at = {@At("HEAD")}
   )
   private static void startConnecting(CallbackInfo ci, @Local(argsOnly = true) ServerAddress serverAddress) {
      ClientEvents.lastServerAddress = serverAddress;
   }
}
