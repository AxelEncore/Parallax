package com.moulberry.axiom.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class InjectPoseVertexConsumer implements VertexConsumer {
   private final Matrix4f pose;
   private final Matrix3f normal;
   private final VertexConsumer delegate;

   public InjectPoseVertexConsumer(PoseStack poseStack, VertexConsumer delegate) {
      this.pose = poseStack.last().pose();
      this.normal = poseStack.last().normal();
      this.delegate = delegate;
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      Vector4f vector4f = this.pose.transform(new Vector4f(x, y, z, 1.0F));
      return this.delegate.addVertex(vector4f.x, vector4f.y, vector4f.z);
   }

   public VertexConsumer addVertex(Matrix4f matrix4f, float x, float y, float z) {
      Vector4f vector4f = this.pose.transform(new Vector4f(x, y, z, 1.0F));
      return this.delegate.addVertex(matrix4f, vector4f.x, vector4f.y, vector4f.z);
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      return this.delegate.setColor(r, g, b, a);
   }

   public VertexConsumer setUv(float u, float v) {
      return this.delegate.setUv(u, v);
   }

   public VertexConsumer setUv1(int u, int v) {
      return this.delegate.setUv1(u, v);
   }

   public VertexConsumer setUv2(int u, int v) {
      return this.delegate.setUv2(u, v);
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      Vector3f vector3f = this.normal.transform(new Vector3f(x, y, z));
      return this.delegate.setNormal(vector3f.x, vector3f.y, vector3f.z);
   }
}
