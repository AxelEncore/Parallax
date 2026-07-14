package com.moulberry.axiom.render.annotations;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.annotations.data.LineAnnotationData;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.tools.annotation.AnnotationByteOffset;
import com.moulberry.axiom.utils.RenderHelper;
import it.unimi.dsi.fastutil.bytes.ByteImmutableList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class LineAnnotation implements Annotation {
   private static final int SMOOTH_COUNT = 9;
   private final LineAnnotationData data;
   private float lastLineWidth = 0.0F;
   @Nullable
   private AxiomDrawBuffer vertexBuffer = null;
   private final SectionPos minSectionPos;
   private final SectionPos maxSectionPos;
   private boolean empty = false;

   public LineAnnotation(LineAnnotationData data) {
      this.data = data;
      int quantizedX = data.startQuantized().getX();
      int quantizedY = data.startQuantized().getY();
      int quantizedZ = data.startQuantized().getZ();
      int minSectionX = quantizedX >> 8;
      int minSectionY = quantizedY >> 8;
      int minSectionZ = quantizedZ >> 8;
      int maxSectionX = quantizedX >> 8;
      int maxSectionY = quantizedY >> 8;
      int maxSectionZ = quantizedZ >> 8;

      for (int i = 0; i < data.offsets().length; i++) {
         byte offsetId = data.offsets()[i];
         AnnotationByteOffset offset = AnnotationByteOffset.idToOffset(offsetId);
         quantizedX += offset.dx();
         quantizedY += offset.dy();
         quantizedZ += offset.dz();
         minSectionX = Math.min(minSectionX, quantizedX >> 8);
         minSectionY = Math.min(minSectionY, quantizedY >> 8);
         minSectionZ = Math.min(minSectionZ, quantizedZ >> 8);
         maxSectionX = Math.max(maxSectionX, quantizedX >> 8);
         maxSectionY = Math.max(maxSectionY, quantizedY >> 8);
         maxSectionZ = Math.max(maxSectionZ, quantizedZ >> 8);
      }

      this.minSectionPos = SectionPos.of(minSectionX, minSectionY, minSectionZ);
      this.maxSectionPos = SectionPos.of(maxSectionX, maxSectionY, maxSectionZ);
   }

   @Override
   public AnnotationData getData() {
      return this.data;
   }

   @Override
   public SectionPos getMinSectionY() {
      return this.minSectionPos;
   }

   @Override
   public SectionPos getMaxSection() {
      return this.maxSectionPos;
   }

   @Nullable
   @Override
   public Gizmo getGizmo() {
      return null;
   }

   @Override
   public void render(AxiomWorldRenderContext rc, UUID uuid, RenderTarget renderTarget) {
      this.drawPoints(rc, renderTarget);
   }

   private void drawPoints(AxiomWorldRenderContext rc, RenderTarget renderTarget) {
      if (this.data.startQuantized() != null && this.data.offsets().length != 0 && !this.empty) {
         Vec3 start = Vec3.atLowerCornerOf(this.data.startQuantized()).scale(0.0625);
         float lineWidth = RenderHelper.baseLineWidth * this.data.lineWidth() / 2.0F;
         if (this.vertexBuffer == null || this.lastLineWidth != lineWidth) {
            this.lastLineWidth = lineWidth;
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            build(rc.position().subtract(start), bufferBuilder, new ByteImmutableList(this.data.offsets()), this.data.colour(), lineWidth);
            MeshData meshData = bufferBuilder.build();
            if (meshData == null) {
               this.empty = true;
               return;
            }

            this.vertexBuffer = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
            this.vertexBuffer.upload(meshData);
         }

         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(start.x - rc.x(), start.y - rc.y(), start.z - rc.z());
         AxiomRenderer.setLineWidthLegacy(lineWidth);
         RenderHelper.pushModelViewMatrix(matrices.last().pose());
         AxiomRenderPipelines.LINE_ANNOTATION_PIPELINE.render(renderTarget, this.vertexBuffer);
         RenderHelper.popModelViewStack();
         matrices.popPose();
      }
   }

   public static void drawStatic(AxiomWorldRenderContext rc, Vec3i startQuantized, ByteList offsets, float lineWidth, int colour) {
      if (startQuantized != null && !offsets.isEmpty()) {
         float actualLineWidth = RenderHelper.baseLineWidth * lineWidth / 2.0F;
         Vec3 start = Vec3.atLowerCornerOf(startQuantized).scale(0.0625);
         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(start.x - rc.x(), start.y - rc.y(), start.z - rc.z());
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         build(rc.position().subtract(start), bufferBuilder, offsets, colour, actualLineWidth);
         MeshData meshData = bufferBuilder.build();
         if (meshData != null) {
            AxiomRenderer.setLineWidthLegacy(actualLineWidth);
            RenderHelper.pushModelViewMatrix(matrices.last().pose());
            AxiomRenderPipelines.LINE_ANNOTATION_PIPELINE.render(meshData);
            RenderHelper.popModelViewStack();
         }

         matrices.popPose();
      }
   }

   private static void build(Vec3 cameraPosition, BufferBuilder bufferBuilder, ByteList offsets, int colour, float lineWidth) {
      List<Vec3> rawPositions = new ArrayList<>(9);
      MutableBlockPos quantizedPosition = new MutableBlockPos();
      Vector3f lastPosition = null;
      Axis axis = null;
      int sumOffsetX = 0;
      int sumOffsetY = 0;
      int sumOffsetZ = 0;
      int sumOffsetLength = 0;
      float levelOfDetail = 1.0F;

      for (int i = 0; i < offsets.size(); i++) {
         byte offsetId = offsets.getByte(i);
         AnnotationByteOffset offset = AnnotationByteOffset.idToOffset(offsetId);
         int offsetX = offset.dx();
         int offsetY = offset.dy();
         int offsetZ = offset.dz();
         int length = Math.abs(offsetX) + Math.abs(offsetY) + Math.abs(offsetZ);
         sumOffsetX += offsetX;
         sumOffsetY += offsetY;
         sumOffsetZ += offsetZ;
         sumOffsetLength += length;
         if (!(sumOffsetLength < levelOfDetail) || i >= offsets.size() - 1) {
            offsetX = sumOffsetX;
            offsetY = sumOffsetY;
            offsetZ = sumOffsetZ;
            length = (int)Math.max(1.0, sumOffsetLength / Math.ceil(levelOfDetail));
            sumOffsetX = 0;
            sumOffsetY = 0;
            sumOffsetZ = 0;
            sumOffsetLength = 0;
            if (axis == null || axis != offset.axis()) {
               while (!rawPositions.isEmpty()) {
                  rawPositions.remove(0);
                  if (!rawPositions.isEmpty()) {
                     lastPosition = drawSegment(rawPositions, lastPosition, bufferBuilder, colour, lineWidth, levelOfDetail);
                  }
               }
            }

            axis = offset.axis();

            for (int j = 1; j <= length; j++) {
               float amount = (float)j / length;
               Vec3 partialOffset = new Vec3(
                  quantizedPosition.getX() + offsetX * amount, quantizedPosition.getY() + offsetY * amount, quantizedPosition.getZ() + offsetZ * amount
               );
               if (rawPositions.size() < 9) {
                  rawPositions.add(partialOffset);
               } else {
                  rawPositions.remove(0);
                  rawPositions.add(partialOffset);
               }

               lastPosition = drawSegment(rawPositions, lastPosition, bufferBuilder, colour, lineWidth, levelOfDetail);
            }

            int x = quantizedPosition.getX() + offsetX;
            int y = quantizedPosition.getY() + offsetY;
            int z = quantizedPosition.getZ() + offsetZ;
            quantizedPosition.set(x, y, z);
            if (lastPosition != null && cameraPosition != null) {
               double distanceSq = cameraPosition.distanceToSqr(lastPosition.x, lastPosition.y, lastPosition.z);
               double distance = Math.sqrt(distanceSq);
               levelOfDetail = (float)Math.max(1.0, distance) / 32.0F;
            }
         }
      }

      while (!rawPositions.isEmpty()) {
         rawPositions.remove(0);
         if (!rawPositions.isEmpty()) {
            lastPosition = drawSegment(rawPositions, lastPosition, bufferBuilder, colour, lineWidth, levelOfDetail);
         }
      }
   }

   private static Vector3f drawSegment(
      List<Vec3> quantizedPositions, Vector3f lastPosition, BufferBuilder bufferBuilder, int colour, float lineWidth, float levelOfDetail
   ) {
      if (quantizedPositions.isEmpty()) {
         return lastPosition;
      } else {
         double sumX = 0.0;
         double sumY = 0.0;
         double sumZ = 0.0;
         int sumCount = 0;

         for (int j = 0; j < quantizedPositions.size(); j++) {
            Vec3 pos = quantizedPositions.get(j);
            sumX += pos.x;
            sumY += pos.y;
            sumZ += pos.z;
            sumCount++;
         }

         float smoothedX = (float)(sumX / (sumCount * 16.0F));
         float smoothedY = (float)(sumY / (sumCount * 16.0F));
         float smoothedZ = (float)(sumZ / (sumCount * 16.0F));
         if (lastPosition == null) {
            return new Vector3f(smoothedX, smoothedY, smoothedZ);
         } else {
            float dx = smoothedX - lastPosition.x();
            float dy = smoothedY - lastPosition.y();
            float dz = smoothedZ - lastPosition.z();
            float distanceInv = 1.0F / (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx *= distanceInv;
            dy *= distanceInv;
            dz *= distanceInv;
            if (levelOfDetail > 0.5F) {
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(
                        lastPosition.x() - levelOfDetail * dx / 16.0F,
                        lastPosition.y() - levelOfDetail * dy / 16.0F,
                        lastPosition.z() - levelOfDetail * dz / 16.0F
                     )
                     .setColor(colour)
                     .setNormal(dx, dy, dz),
                  lineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(
                        smoothedX + levelOfDetail * dx / 16.0F, smoothedY + levelOfDetail * dy / 16.0F, smoothedZ + levelOfDetail * dz / 16.0F
                     )
                     .setColor(colour)
                     .setNormal(dx, dy, dz),
                  lineWidth
               );
            } else {
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(lastPosition.x(), lastPosition.y(), lastPosition.z()).setColor(colour).setNormal(dx, dy, dz), lineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(smoothedX, smoothedY, smoothedZ).setColor(colour).setNormal(dx, dy, dz), lineWidth
               );
            }

            lastPosition.x = smoothedX;
            lastPosition.y = smoothedY;
            lastPosition.z = smoothedZ;
            return lastPosition;
         }
      }
   }

   @Override
   public void sectionChanged() {
      this.close();
   }

   @Override
   public void close() {
      if (this.vertexBuffer != null) {
         this.vertexBuffer.close();
         this.vertexBuffer = null;
      }
   }
}
