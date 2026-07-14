package com.moulberry.axiom.mixin.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ByteBufferBuilder.class})
public class MixinByteBufferBuilder {
   @ModifyVariable(
      method = {"ensureCapacity"},
      at = @At("HEAD"),
      index = 1,
      argsOnly = true
   )
   public int modifyCapacity(int value) {
      return value + 256;
   }
}
