package com.moulberry.axiom.utils;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class AxiomVertexFormats {
   public static final VertexFormat BLIT_SCREEN = DefaultVertexFormat.BLIT_SCREEN;
   public static final VertexFormat AXIOM_BLOCK = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .build();
   public static final VertexFormat COLLISION_MESH_VERTEX_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("UV0", VertexFormatElement.UV0)
      .build();
   public static final VertexFormat BORDER_VERTEX_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV2", VertexFormatElement.UV2)
      .build();
}
