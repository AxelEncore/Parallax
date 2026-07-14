package com.moulberry.axiom.mixin.compat;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@IfModLoaded("resolutioncontrol")
@Mixin({Minecraft.class})
public class MixinResolutionControlMinecraft {
   @Inject(
      method = {"resizeDisplay"},
      at = {@At("RETURN")}
   )
   public void onResolutionChanged(CallbackInfo ci) {
      ResolutionControlMod.getInstance().onResolutionChanged();
   }
}
