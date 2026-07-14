package com.moulberry.axiom.core_rendering;

import com.mojang.blaze3d.vertex.VertexBuffer.Usage;

public record AxiomBufferUsage(int usageFlags) {
   private static final int USAGE_MAP_READ = 1;
   private static final int USAGE_MAP_WRITE = 2;
   private static final int USAGE_HINT_CLIENT_STORAGE = 4;
   public static AxiomBufferUsage STREAM_READ = new AxiomBufferUsage(5);
   public static AxiomBufferUsage STATIC_READ = new AxiomBufferUsage(1);
   public static AxiomBufferUsage STREAM_WRITE = new AxiomBufferUsage(6);
   public static AxiomBufferUsage STATIC_WRITE = new AxiomBufferUsage(2);

   public Usage bufferUsage() {
      boolean dynamic = (this.usageFlags & 4) != 0;
      return dynamic ? Usage.DYNAMIC : Usage.STATIC;
   }
}
