package com.moulberry.axiom.mixin;

import com.moulberry.axiom.i18n.LocalizationSource;
import java.util.function.Consumer;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {BuiltInPackSource.class},
   priority = 900
)
public class MixinBuiltInPackSource {
   @Inject(
      method = {"loadPacks"},
      at = {@At("RETURN")}
   )
   public void loadPacks(Consumer<Pack> consumer, CallbackInfo ci) {
      if ((Object)this instanceof ClientPackSource) {
         LocalizationSource.INSTANCE.loadPacks(consumer);
      }
   }
}
