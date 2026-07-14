package com.moulberry.axiom.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;

public class ProjectionMatrixBackup {
   private Matrix4f projection;
   private VertexSorting projectionType;

   public static ProjectionMatrixBackup create() {
      ProjectionMatrixBackup backup = new ProjectionMatrixBackup();
      backup.projection = new Matrix4f(RenderSystem.getProjectionMatrix());
      backup.projectionType = RenderSystem.getVertexSorting();
      return backup;
   }

   public void restore() {
      RenderSystem.setProjectionMatrix(this.projection, this.projectionType);
   }
}
