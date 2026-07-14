package com.moulberry.axiom.mixin.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.moulberry.axiom.hooks.BufferBuilderExt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({BufferBuilder.class})
public abstract class MixinBufferBuilder implements BufferBuilderExt {
   @Shadow
   private long vertexPointer;
   @Shadow
   @Final
   private ByteBufferBuilder buffer;

   @Override
   public long axiom$getVertexPointer() {
      return this.vertexPointer;
   }

   @Override
   public long axiom$reserve(int bytes) {
      this.vertexPointer = this.buffer.reserve(bytes);
      return this.vertexPointer;
   }

   @Override
   public ByteBufferBuilder axiom$getByteBufferBuilder() {
      return this.buffer;
   }
}
