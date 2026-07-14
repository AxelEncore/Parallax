package com.moulberry.axiom.core_rendering;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;

public class AxiomDrawBuffer implements AutoCloseable {
   AxiomBufferUsage bufferUsage;
   VertexBuffer vertexBuffer;

   public AxiomDrawBuffer(AxiomBufferUsage bufferUsage) {
      this.bufferUsage = bufferUsage;
      this.vertexBuffer = null;
   }

   @Override
   public void close() {
      if (this.vertexBuffer != null) {
         this.vertexBuffer.close();
         this.vertexBuffer = null;
      }
   }

   public void upload(MeshData meshData) {
      if (this.vertexBuffer == null) {
         this.vertexBuffer = new VertexBuffer(this.bufferUsage.bufferUsage());
      }

      this.vertexBuffer.bind();
      this.vertexBuffer.upload(meshData);
      VertexBuffer.unbind();
   }

   public void setIndexBuffer(Result result) {
      if (this.vertexBuffer == null) {
         this.vertexBuffer = new VertexBuffer(this.bufferUsage.bufferUsage());
      }

      this.vertexBuffer.bind();
      this.vertexBuffer.uploadIndexBuffer(result);
      VertexBuffer.unbind();
   }

   public VertexBuffer getInnerVertexBuffer() {
      return this.vertexBuffer;
   }

   public void draw() {
      this.vertexBuffer.bind();
      this.vertexBuffer.draw();
      VertexBuffer.unbind();
   }
}
