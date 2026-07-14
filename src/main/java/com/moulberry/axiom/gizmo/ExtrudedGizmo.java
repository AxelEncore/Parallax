package com.moulberry.axiom.gizmo;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectionf;
import org.joml.Vector3f;

public class ExtrudedGizmo {
   private final Vector3f normal;
   private final BlockPos from;
   public final Gizmo extrudeGizmoFrom;

   public ExtrudedGizmo(LocalPlayer player, Gizmo extrudeGizmoFrom) {
      this.normal = player.getLookAngle().toVector3f().mul(-1.0F);
      this.from = extrudeGizmoFrom.getTargetPosition();
      this.extrudeGizmoFrom = extrudeGizmoFrom;
   }

   public void render(AxiomWorldRenderContext rc, Vec3 lookDirection) {
      BlockPos target = this.getBlockPos(lookDirection);
      if (target != null) {
         Vec3 offset = Vec3.atCenterOf(target);
         float distanceMultiplier = (float)rc.position().distanceTo(offset) / 20.0F;
         if (distanceMultiplier < 0.5F) {
            distanceMultiplier = 0.5F;
         }

         float boxSize = 0.3F * distanceMultiplier;
         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(offset.x - rc.x(), offset.y - rc.y(), offset.z - rc.z());
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         Pose pose = matrices.last();
         Vec3 fromDelta = this.from.getCenter().subtract(offset);
         float length = (float)fromDelta.length();
         float nx = (float)fromDelta.x / length;
         float ny = (float)fromDelta.y / length;
         float nz = (float)fromDelta.z / length;
         VersionUtilsClient.legacySetLineWidthIgnored(
            bufferBuilder.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(pose, nx, ny, nz), RenderHelper.baseLineWidth
         );
         VersionUtilsClient.legacySetLineWidthIgnored(
            bufferBuilder.addVertex(pose, (float)fromDelta.x, (float)fromDelta.y, (float)fromDelta.z).setColor(-16777216).setNormal(pose, nx, ny, nz),
            RenderHelper.baseLineWidth
         );
         AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(provider.build());
         matrices.translate(-boxSize, -boxSize, -boxSize);
         Shapes.shadedBox(provider, matrices.last().pose(), 2.0F * boxSize, 2.0F * boxSize, 2.0F * boxSize, -1);
         AxiomRenderPipelines.POSITION_COLOR_IGNORE_DEPTH.render(provider.build());
         matrices.popPose();
      }
   }

   @Nullable
   public BlockPos getBlockPos(Vec3 lookDirection) {
      LocalPlayer player = Minecraft.getInstance().player;
      Vector3f origin = player.getEyePosition().toVector3f();
      Vector3f ray = lookDirection.toVector3f();
      Vector3f pointOnPlane = Vec3.atCenterOf(this.from).toVector3f();
      float t = Intersectionf.intersectRayPlane(origin, ray, pointOnPlane, this.normal, 1.0E-5F);
      if (t < 0.0F) {
         return null;
      } else {
         Vector3f intersection = origin.add(ray.mul(t));
         int x = (int)Math.floor(intersection.x);
         int y = (int)Math.floor(intersection.y);
         int z = (int)Math.floor(intersection.z);
         return new BlockPos(x, y, z);
      }
   }
}
