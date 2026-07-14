package com.moulberry.axiom.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class ProjectedText {
   private static ProjectionMatrixBackup projectionMatrixBackup = null;
   private static Font font;
   private static float scale = 0.0F;
   private static float width = 0.0F;
   private static float height = 0.0F;

   public static void setupProjectedText() {
      if (projectionMatrixBackup != null) {
         if (Axiom.enableAssertions) {
            throw new RuntimeException("finishProjectedText() called without setupProjectedText()");
         }
      } else {
         font = Minecraft.getInstance().font;
         scale = 2.0F * Math.max(1.0F, Math.min(EditorUI.getIO().getDisplayFramebufferScaleX(), EditorUI.getIO().getDisplayFramebufferScaleY()));
         width = Minecraft.getInstance().getWindow().getWidth() / scale;
         height = Minecraft.getInstance().getWindow().getHeight() / scale;
         projectionMatrixBackup = ProjectionMatrixBackup.create();
         RenderHelper.setOrthoProjectionMatrix(width, height);
         Matrix4fStack poseStack = RenderSystem.getModelViewStack();
         poseStack.pushMatrix();
         poseStack.identity();
         poseStack.translate(0.0F, 0.0F, -1001.0F);
         RenderHelper.tryApplyModelViewMatrix();
         RenderHelper.pushDisableFog();
      }
   }

   public static void renderProjectedText(String text, PoseStack matrix, Matrix4fc projection, float x, float y, float z) {
      if (projectionMatrixBackup == null) {
         if (Axiom.enableAssertions) {
            throw new RuntimeException("renderProjectedText() called without setupProjectedText()");
         }
      } else {
         Vector4f transformed = matrix.last().pose().transform(new Vector4f(x, y, z, 1.0F));
         transformed = projection.transform(transformed);
         int stringWidth = font.width(text);
         if (!(transformed.w < 0.0F)) {
            float screenX = Math.round(((transformed.x / transformed.w * 0.5F + 0.5F) * width - stringWidth / 2.0F) * scale) / scale;
            float screenY = Math.round(((-transformed.y / transformed.w * 0.5F + 0.5F) * height - 9.0F / 2.0F) * scale) / scale;
            font.drawInBatch8xOutline(
               Component.literal(text).getVisualOrderText(),
               screenX,
               screenY,
               -3351041,
               -13420737,
               new Matrix4f(),
               Minecraft.getInstance().renderBuffers().bufferSource(),
               15728880
            );
         }
      }
   }

   public static void finishProjectedText() {
      if (projectionMatrixBackup == null) {
         if (Axiom.enableAssertions) {
            throw new RuntimeException("finishProjectedText() called without setupProjectedText()");
         }
      } else {
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
         RenderHelper.popDisableFog();
         Matrix4fStack poseStack = RenderSystem.getModelViewStack();
         poseStack.popMatrix();
         RenderHelper.tryApplyModelViewMatrix();
         projectionMatrixBackup.restore();
         projectionMatrixBackup = null;
      }
   }
}
