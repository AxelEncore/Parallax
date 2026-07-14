package com.moulberry.axiom.render.regions;

import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Vector3f;

public class InvertedVertexSorting {
   public static VertexSorting byDistance(float f, float g, float h) {
      return byDistance(new Vector3f(f, g, h));
   }

   public static VertexSorting byDistance(Vector3f vector3f) {
      return VertexSorting.byDistance(other -> -vector3f.distanceSquared(other));
   }
}
