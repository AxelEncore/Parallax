package com.moulberry.axiom.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import org.jetbrains.annotations.Nullable;

public interface VertexConsumerProvider {
   BufferBuilder begin(Mode var1, VertexFormat var2);

   @Nullable
   MeshData build();

   void close();

   void clear();

   static VertexConsumerProvider owned(int capacity) {
      return new VertexConsumerProvider.OwnedVertexConsumerProvider(capacity);
   }

   static VertexConsumerProvider shared() {
      return new VertexConsumerProvider.SharedVertexConsumerProvider();
   }

   public static class OwnedVertexConsumerProvider implements VertexConsumerProvider {
      private final ByteBufferBuilder byteBufferBuilder;
      private BufferBuilder bufferBuilder;

      public OwnedVertexConsumerProvider(int capacity) {
         this.byteBufferBuilder = new ByteBufferBuilder(capacity);
      }

      @Override
      public BufferBuilder begin(Mode mode, VertexFormat vertexFormat) {
         this.bufferBuilder = new BufferBuilder(this.byteBufferBuilder, mode, vertexFormat);
         return this.bufferBuilder;
      }

      @Override
      public MeshData build() {
         return this.bufferBuilder.build();
      }

      @Override
      public void close() {
         this.byteBufferBuilder.close();
         this.bufferBuilder = null;
      }

      @Override
      public void clear() {
         this.byteBufferBuilder.discard();
         this.bufferBuilder = null;
      }
   }

   public static class SharedVertexConsumerProvider implements VertexConsumerProvider {
      private BufferBuilder bufferBuilder;

      @Override
      public BufferBuilder begin(Mode mode, VertexFormat vertexFormat) {
         this.bufferBuilder = Tesselator.getInstance().begin(mode, vertexFormat);
         return this.bufferBuilder;
      }

      @Override
      public MeshData build() {
         return this.bufferBuilder.build();
      }

      @Override
      public void close() {
         this.clear();
         this.bufferBuilder = null;
      }

      @Override
      public void clear() {
         Tesselator.getInstance().clear();
      }
   }
}
