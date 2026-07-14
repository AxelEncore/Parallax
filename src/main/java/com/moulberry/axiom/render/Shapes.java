package com.moulberry.axiom.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomRenderPipeline;
import com.moulberry.axiom.utils.RenderHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

public class Shapes {
   public static void blockOutline(BufferBuilder bufferBuilder, Pose pose, VoxelShape voxelShape, int colour) {
      if (voxelShape.isEmpty()) {
         voxelShape = net.minecraft.world.phys.shapes.Shapes.block();
      }

      voxelShape.forAllEdges(
         (x1, y1, z1, x2, y2, z2) -> {
            float dx = (float)(x2 - x1);
            float dy = (float)(y2 - y1);
            float dz = (float)(z2 - z1);
            float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            float var19;
            float var20;
            float var21;
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, (float)x1, (float)y1, (float)z1)
                  .setColor(colour)
                  .setNormal(pose, var19 = dx / length, var20 = dy / length, var21 = dz / length),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(colour).setNormal(pose, var19, var20, var21), RenderHelper.baseLineWidth
            );
         }
      );
   }

   public static void shadedBox(VertexConsumerProvider provider, Matrix4f matrix4f, float sizeX, float sizeY, float sizeZ, int colour) {
      float alpha = (colour >> 24 & 0xFF) / 255.0F;
      float red = (colour >> 16 & 0xFF) / 255.0F;
      float green = (colour >> 8 & 0xFF) / 255.0F;
      float blue = (colour & 0xFF) / 255.0F;
      float XF = 0.7F;
      float YPF = 1.0F;
      float YNF = 0.6F;
      float ZF = 0.87F;
      BufferBuilder bufferBuilder = provider.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
   }

   public static void shadedBoxTriangles(BufferBuilder bufferBuilder, Matrix4f matrix4f, float sizeX, float sizeY, float sizeZ, int colour) {
      float alpha = (colour >> 24 & 0xFF) / 255.0F;
      float red = (colour >> 16 & 0xFF) / 255.0F;
      float green = (colour >> 8 & 0xFF) / 255.0F;
      float blue = (colour & 0xFF) / 255.0F;
      float XF = 0.7F;
      float YPF = 1.0F;
      float YNF = 0.6F;
      float ZF = 0.87F;
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, sizeZ).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, sizeZ).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, 0.0F, 0.0F).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, 0.0F).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, sizeZ).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, sizeX, sizeY, 0.0F).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, 0.0F, sizeY, sizeZ).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
   }

   public static void lineBox(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      float red,
      float green,
      float blue,
      float alpha,
      float xAxisRed,
      float yAxisGreen,
      float zAxisBlue,
      float lineWidth
   ) {
      Pose pose = poseStack.last();
      Matrix4f matrix4f = pose.pose();
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, minY, minZ).setColor(red, yAxisGreen, zAxisBlue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, minY, minZ).setColor(red, yAxisGreen, zAxisBlue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, minY, minZ).setColor(xAxisRed, green, zAxisBlue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, maxY, minZ).setColor(xAxisRed, green, zAxisBlue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, minY, minZ).setColor(xAxisRed, yAxisGreen, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, minY, maxZ).setColor(xAxisRed, yAxisGreen, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, minY, minZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(pose, -1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(pose, -1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, -1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, -1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, -1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, minY, minZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, -1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, minX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 1.0F, 0.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, minY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 1.0F, 0.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, maxY, minZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F), lineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         vertexConsumer.addVertex(matrix4f, maxX, maxY, maxZ).setColor(red, green, blue, alpha).setNormal(pose, 0.0F, 0.0F, 1.0F), lineWidth
      );
   }

   public static void line(BufferBuilder bufferBuilder, Pose pose, Vec3 from, Vec3 to) {
      line(bufferBuilder, pose, 1.0F, 1.0F, 1.0F, (float)from.x, (float)from.y, (float)from.z, (float)to.x, (float)to.y, (float)to.z);
   }

   public static void line(BufferBuilder bufferBuilder, Pose pose, float red, float green, float blue, Vec3 from, Vec3 to) {
      line(bufferBuilder, pose, red, green, blue, (float)from.x, (float)from.y, (float)from.z, (float)to.x, (float)to.y, (float)to.z);
   }

   public static void line(
      BufferBuilder bufferBuilder, Pose pose, float red, float green, float blue, float fromX, float fromY, float fromZ, float toX, float toY, float toZ
   ) {
      float dx = toX - fromX;
      float dy = toY - fromY;
      float dz = toZ - fromZ;
      float distanceInv = 1.0F / (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      dx *= distanceInv;
      dy *= distanceInv;
      dz *= distanceInv;
      Matrix4f transform = pose.pose();
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(transform, fromX, fromY, fromZ).setColor(red, green, blue, 1.0F).setNormal(pose, dx, dy, dz), RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(transform, toX, toY, toZ).setColor(red, green, blue, 1.0F).setNormal(pose, dx, dy, dz), RenderHelper.baseLineWidth
      );
   }

   public static void drawCone(
      VertexConsumerProvider provider, Matrix4f matrix4f, Vec3 base, Vec3 tip, int dir, float radius, int colour, AxiomRenderPipeline pipeline
   ) {
      fan(provider, matrix4f, base, tip, dir, false, radius, colour);
      pipeline.render(provider.build());
      fan(provider, matrix4f, base, base, dir, true, radius, colour);
      pipeline.render(provider.build());
   }

   public static void fan(VertexConsumerProvider provider, Matrix4f matrix4f, Vec3 base, Vec3 tip, int dir, boolean invert, float radius, int colour) {
      BufferBuilder bufferBuilder = provider.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.addVertex(matrix4f, (float)tip.x, (float)tip.y, (float)tip.z).setColor(colour | 0xFF000000);

      for (int i = 0; i <= 30; i++) {
         float angle = (invert ? -1 : 1) * (float)Math.toRadians(i * 12);
         float x = Mth.sin(angle) * radius;
         float z = Mth.cos(angle) * radius;
         if (dir == 0) {
            bufferBuilder.addVertex(matrix4f, (float)base.x, (float)base.y + z, (float)base.z + x).setColor(colour);
         } else if (dir == 1) {
            bufferBuilder.addVertex(matrix4f, (float)base.x + x, (float)base.y, (float)base.z + z).setColor(colour);
         } else {
            bufferBuilder.addVertex(matrix4f, (float)base.x + z, (float)base.y + x, (float)base.z).setColor(colour);
         }
      }
   }

   public static void shadedCone(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3 base, Vec3 tip, int dir, float radius, int colour) {
      boolean invert;
      if (dir == 0) {
         invert = base.x < tip.x;
      } else if (dir == 1) {
         invert = base.y < tip.y;
      } else {
         invert = base.z < tip.z;
      }

      shadedFan(bufferBuilder, matrix4f, base, tip, dir, invert, radius, colour);
      shadedFan(bufferBuilder, matrix4f, base, base, dir, !invert, radius, colour);
   }

   public static void shadedFan(BufferBuilder bufferBuilder, Matrix4f matrix4f, Vec3 base, Vec3 tip, int dir, boolean invert, float radius, int colour) {
      float alpha = (colour >> 24 & 0xFF) / 255.0F;
      float red = (colour >> 16 & 0xFF) / 255.0F;
      float green = (colour >> 8 & 0xFF) / 255.0F;
      float blue = (colour & 0xFF) / 255.0F;
      float XF = 0.7F;
      float YPF = 1.0F;
      float YNF = 0.6F;
      float ZF = 0.87F;
      float lastX = 0.0F * radius;
      float lastZ = 1.0F * radius;
      float lastShade = 1.0F;

      for (int i = 1; i <= 31; i++) {
         float angle = (invert ? -1 : 1) * (float)Math.toRadians(i * 12);
         float x = Mth.sin(angle) * radius;
         float z = Mth.cos(angle) * radius;
         Vec3 normal;
         if (dir == 0) {
            Vec3 one = new Vec3((float)base.x - tip.x, (float)base.y + z - tip.y, (float)base.z + x - tip.z);
            Vec3 two = new Vec3((float)base.x - tip.x, (float)base.y + lastZ - tip.y, (float)base.z + lastX - tip.z);
            normal = one.cross(two).normalize();
         } else if (dir == 1) {
            Vec3 one = new Vec3((float)base.x + x - tip.x, (float)base.y - tip.y, (float)base.z + z - tip.z);
            Vec3 two = new Vec3((float)base.x + lastX - tip.x, (float)base.y - tip.y, (float)base.z + lastZ - tip.z);
            normal = one.cross(two).normalize();
         } else {
            Vec3 one = new Vec3((float)base.x + z - tip.x, (float)base.y + x - tip.y, (float)base.z - tip.z);
            Vec3 two = new Vec3((float)base.x + lastZ - tip.x, (float)base.y + lastX - tip.y, (float)base.z - tip.z);
            normal = one.cross(two).normalize();
         }

         float shade = (float)(normal.x * normal.x) * 0.7F;
         if (normal.y > 0.0) {
            shade += (float)(normal.y * normal.y) * 1.0F;
         } else {
            shade += (float)(normal.y * normal.y) * 0.6F;
         }

         shade += (float)(normal.z * normal.z) * 0.87F;
         if (i > 1) {
            float avgShade = (lastShade + shade) / 2.0F;
            bufferBuilder.addVertex(matrix4f, (float)tip.x, (float)tip.y, (float)tip.z).setColor(red * avgShade, green * avgShade, blue * avgShade, alpha);
            if (dir == 0) {
               bufferBuilder.addVertex(matrix4f, (float)base.x, (float)base.y + z, (float)base.z + x).setColor(red * shade, green * shade, blue * shade, alpha);
               bufferBuilder.addVertex(matrix4f, (float)base.x, (float)base.y + lastZ, (float)base.z + lastX)
                  .setColor(red * lastShade, green * lastShade, blue * lastShade, alpha);
            } else if (dir == 1) {
               bufferBuilder.addVertex(matrix4f, (float)base.x + x, (float)base.y, (float)base.z + z).setColor(red * shade, green * shade, blue * shade, alpha);
               bufferBuilder.addVertex(matrix4f, (float)base.x + lastX, (float)base.y, (float)base.z + lastZ)
                  .setColor(red * lastShade, green * lastShade, blue * lastShade, alpha);
            } else {
               bufferBuilder.addVertex(matrix4f, (float)base.x + z, (float)base.y + x, (float)base.z).setColor(red * shade, green * shade, blue * shade, alpha);
               bufferBuilder.addVertex(matrix4f, (float)base.x + lastZ, (float)base.y + lastX, (float)base.z)
                  .setColor(red * lastShade, green * lastShade, blue * lastShade, alpha);
            }
         }

         lastShade = shade;
         lastX = x;
         lastZ = z;
      }
   }
}
