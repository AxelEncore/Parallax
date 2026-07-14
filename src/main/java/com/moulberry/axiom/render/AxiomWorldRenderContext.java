package com.moulberry.axiom.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public record AxiomWorldRenderContext(
   Camera rawCameraDontUse,
   PoseStack poseStack,
   float partialTick,
   long nanos,
   Vec3 position,
   BlockPos blockPosition,
   float xRot,
   float yRot,
   Quaternionfc rotation,
   Matrix4fc projection
) {
   public AxiomWorldRenderContext(Camera camera, float tickDelta, PoseStack poseStack, Matrix4fc projection) {
      this(
         camera,
         poseStack,
         tickDelta,
         Util.getNanos(),
         camera.getPosition(),
         camera.getBlockPosition(),
         camera.getXRot(),
         camera.getYRot(),
         camera.rotation(),
         projection
      );
   }

   private static PoseStack createPoseStack(Matrix4fc matrix4fc) {
      PoseStack poseStack = new PoseStack();
      if (matrix4fc instanceof Matrix4f matrix4f) {
         poseStack.mulPose(matrix4f);
      } else {
         poseStack.mulPose(new Matrix4f(matrix4fc));
      }

      return poseStack;
   }

   public double x() {
      return this.position.x;
   }

   public double y() {
      return this.position.y;
   }

   public double z() {
      return this.position.z;
   }
}
