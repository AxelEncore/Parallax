package com.moulberry.axiom.hooks;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;

public interface BufferBuilderExt {
   long axiom$getVertexPointer();

   long axiom$reserve(int var1);

   ByteBufferBuilder axiom$getByteBufferBuilder();
}
