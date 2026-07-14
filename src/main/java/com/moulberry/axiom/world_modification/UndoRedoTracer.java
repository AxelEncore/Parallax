package com.moulberry.axiom.world_modification;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class UndoRedoTracer {
   private final Vec3[] points;
   private final int colour;
   private final boolean reverse;
   private int progress;

   public UndoRedoTracer(Vec3 target, int colour, boolean reverse) {
      LocalPlayer player = Minecraft.getInstance().player;
      Vec3 look = getLookVector(player);
      Vec3 chest = player.position().add(0.0, 1.0, 0.0);
      Vec3 p1 = chest.subtract(look);
      float distance = (float)target.distanceTo(p1);
      Vec3 p2 = player.position().add(0.0, 1.0, 0.0).add(look.scale(distance / 3.0F));
      this.points = bezierQuad(p1, p2, target, 70);
      this.colour = colour;
      this.reverse = reverse;
   }

   public boolean tick() {
      return ++this.progress >= 20;
   }

   public void render(AxiomWorldRenderContext rc) {
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder vertices = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Vec3 first = this.points[0];
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(first.x - rc.x(), first.y - rc.y(), first.z - rc.z());
      Pose pose = matrices.last();
      Vec3 last = null;
      float lastAlpha = 0.0F;
      int pointIndex = (int)((this.progress - 5 + rc.partialTick()) * 5.0F);

      for (int i = Math.max(pointIndex, 0); i <= pointIndex + 40 && i < this.points.length; i++) {
         int j = i;
         if (this.reverse) {
            j = this.points.length - 1 - i;
         }

         Vec3 point = this.points[j];
         float alpha = Math.min(1.0F, 1.0F - Math.abs(i - pointIndex - 20) / 20.0F);
         if (last == null) {
            last = point;
            lastAlpha = alpha;
         } else {
            Vec3 delta = last.subtract(point).normalize();
            VersionUtilsClient.legacySetLineWidthIgnored(
               vertices.addVertex(pose.pose(), (float)(last.x() - first.x), (float)(last.y() - first.y), (float)(last.z() - first.z))
                  .setColor((int)(lastAlpha * 255.0F) << 24 | this.colour)
                  .setNormal(pose, (float)delta.x(), (float)delta.y(), (float)delta.z()),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               vertices.addVertex(pose.pose(), (float)(point.x() - first.x), (float)(point.y() - first.y), (float)(point.z() - first.z))
                  .setColor((int)(alpha * 255.0F) << 24 | this.colour)
                  .setNormal(pose, (float)delta.x(), (float)delta.y(), (float)delta.z()),
               RenderHelper.baseLineWidth
            );
            last = point;
            lastAlpha = alpha;
         }
      }

      matrices.popPose();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
   }

   private static Vec3 getLookVector(LocalPlayer player) {
      float f = player.getXRot() * (float) (Math.PI / 180.0);
      float g = -player.getYHeadRot() * (float) (Math.PI / 180.0);
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   private static Vec3[] bezierQuad(Vec3 p1, Vec3 p2, Vec3 p3, int num) {
      Vec3[] points = new Vec3[num];

      for (int i = 0; i < num; i++) {
         double f = i / (num - 1.0);
         double c1 = (1.0 - f) * (1.0 - f);
         double c2 = 2.0 * (1.0 - f) * f;
         double c3 = f * f;
         points[i] = new Vec3(p1.x() * c1 + p2.x() * c2 + p3.x() * c3, p1.y() * c1 + p2.y() * c2 + p3.y() * c3, p1.z() * c1 + p2.z() * c2 + p3.z() * c3);
      }

      return points;
   }
}
