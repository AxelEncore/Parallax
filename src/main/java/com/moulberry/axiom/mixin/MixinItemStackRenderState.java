package com.moulberry.axiom.mixin;

import com.moulberry.axiom.Dummy;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Dummy.class})
public class MixinItemStackRenderState {
   @Mixin({Dummy.class})
   private static class MixinLayerRenderState {
   }
}
