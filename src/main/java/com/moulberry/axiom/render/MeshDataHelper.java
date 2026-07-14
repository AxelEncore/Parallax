package com.moulberry.axiom.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import com.mojang.blaze3d.vertex.MeshData.SortState;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.hooks.BufferBuilderExt;
import org.jetbrains.annotations.Nullable;

public class MeshDataHelper {
   public static void discard(@Nullable MeshData meshData) {
      if (meshData != null) {
         meshData.close();
      }
   }

   @Nullable
   public static MeshDataHelper.MeshDataAndSortState buildAndSort(BufferBuilder bufferBuilder, VertexSorting vertexSorting) {
      MeshData meshData = bufferBuilder.build();
      if (meshData == null) {
         return null;
      } else {
         SortState sortState = meshData.sortQuads(((BufferBuilderExt)bufferBuilder).axiom$getByteBufferBuilder(), vertexSorting);
         return new MeshDataHelper.MeshDataAndSortState(meshData, new SortStateWrapper(sortState));
      }
   }

   public static SortStateWrapper resort(BufferBuilder bufferBuilder, SortStateWrapper sortState, AxiomDrawBuffer drawBuffer, VertexSorting vertexSorting) {
      if (sortState == null) {
         return null;
      } else {
         Result result = sortState.inner().buildSortedIndexBuffer(((BufferBuilderExt)bufferBuilder).axiom$getByteBufferBuilder(), vertexSorting);
         if (result != null) {
            drawBuffer.setIndexBuffer(result);
         }

         return sortState;
      }
   }

   public record MeshDataAndSortState(MeshData meshData, SortStateWrapper sortStateWrapper) {
   }
}
