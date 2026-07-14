package com.moulberry.axiom.mixin;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Minecraft.class},
   priority = 907
)
public class MixinMinecraftPickBlock {
   @Shadow
   @Nullable
   public HitResult hitResult;

   @Inject(
      method = {"pickBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pickBlock(CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         if (BuilderToolManager.isToolSlotActive()) {
            BuilderToolManager.middleClick(this.hitResult);
            ci.cancel();
         }

         if (UserAction.MIDDLE_MOUSE.call(null) == UserAction.ActionResult.USED_STOP) {
            ci.cancel();
         }
      }
   }
}
