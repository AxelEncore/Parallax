package com.moulberry.axiom.render.annotations;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.annotations.AnnotationHistoryElement;
import com.moulberry.axiom.annotations.AnnotationUpdateAction;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.annotations.data.TextAnnotationData;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TextAnnotation implements Annotation {
   private final TextAnnotationData data;
   private final String[] splitText;
   private final Gizmo gizmo;
   private final Quaternionf additionalRotation = new Quaternionf();

   public TextAnnotation(TextAnnotationData data) {
      this.data = data;
      this.splitText = data.text().split("\n");
      this.gizmo = new Gizmo(new Vec3(data.position()));
      this.gizmo.translationSnapping = 16;
      this.gizmo.enableRotation = true;
      this.gizmo.rotationSnapRadians = (float) (Math.PI / 180.0);
   }

   @Override
   public AnnotationData getData() {
      return this.data;
   }

   @Override
   public SectionPos getMinSectionY() {
      return SectionPos.of(new Vec3(this.data.position()));
   }

   @Override
   public SectionPos getMaxSection() {
      return SectionPos.of(new Vec3(this.data.position()));
   }

   @Nullable
   @Override
   public Gizmo getGizmo() {
      return this.gizmo;
   }

   @Override
   public void render(AxiomWorldRenderContext rc, UUID uuid, RenderTarget renderTarget) {
      float directionXRot;
      float directionYRot;
      switch (this.data.direction()) {
         case UP:
            directionXRot = -90.0F;
            directionYRot = this.data.fallbackYaw() - 180.0F;
            break;
         case DOWN:
            directionXRot = 90.0F;
            directionYRot = this.data.fallbackYaw() - 180.0F;
            break;
         default:
            directionXRot = 0.0F;
            directionYRot = this.data.direction().toYRot();
      }

      Quaternionf quaternionf = new Quaternionf();
      switch (this.data.billboardMode()) {
         case 0:
            quaternionf.rotateYXZ((float) (-Math.PI / 180.0) * directionYRot, (float) (Math.PI / 180.0) * directionXRot, 0.0F);
            break;
         case 1:
            quaternionf.rotateYXZ((float) (-Math.PI / 180.0) * directionYRot, (float) (Math.PI / 180.0) * -rc.xRot(), 0.0F);
            break;
         case 2:
            quaternionf.rotateYXZ((float) (-Math.PI / 180.0) * (rc.yRot() - 180.0F), (float) (Math.PI / 180.0) * directionXRot, 0.0F);
            break;
         default:
            quaternionf.rotateYXZ((float) (-Math.PI / 180.0) * (rc.yRot() - 180.0F), (float) (Math.PI / 180.0) * -rc.xRot(), 0.0F);
      }

      Gizmo.GizmoRotation gizmoRotation = this.gizmo.popRotation();
      if (gizmoRotation != null && gizmoRotation.radians() != 0.0F) {
         Quaternionf inverse = new Quaternionf(quaternionf).invert();
         quaternionf.premul(this.additionalRotation);
         quaternionf.premul(gizmoRotation.toQuaternion());
         Quaternionf previousRotation = new Quaternionf(this.additionalRotation);
         quaternionf.mul(inverse, this.additionalRotation);
         if (!Objects.equals(previousRotation, this.additionalRotation)) {
            Annotations.push(
               new AnnotationHistoryElement(
                  new AnnotationUpdateAction.RotateAnnotation(uuid, previousRotation),
                  new AnnotationUpdateAction.RotateAnnotation(uuid, new Quaternionf(this.additionalRotation))
               )
            );
         }
      } else {
         quaternionf.premul(this.additionalRotation);
         Gizmo.GizmoRotation peeked = this.gizmo.peekGizmoRotation();
         if (peeked != null) {
            quaternionf.premul(peeked.toQuaternion());
         }
      }

      if (!this.gizmo.isGrabbed()) {
         this.gizmo.moveToVecInstantly(new Vec3(this.data.position()));
         this.additionalRotation.set(this.data.rotation());
      }

      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      Vector3f position = this.gizmo.getInterpPosition().toVector3f();
      matrices.translate(position.x - rc.x(), position.y - rc.y(), position.z - rc.z());
      matrices.mulPose(quaternionf);
      matrices.scale(this.data.scale() / 16.0F, -this.data.scale() / 16.0F, this.data.scale() / 16.0F);
      Font font = Minecraft.getInstance().font;
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);

      for (int i = 0; i < this.splitText.length; i++) {
         String line = this.splitText[i];
         int width = font.width(line);
         float y = i * 9 - this.splitText.length * 9 / 2.0F;
         font.drawInBatch(
            line,
            -width / 2.0F,
            y,
            this.data.colour(),
            this.data.shadow(),
            matrices.last().pose(),
            Minecraft.getInstance().renderBuffers().bufferSource(),
            DisplayMode.POLYGON_OFFSET,
            0,
            15728880
         );
      }

      matrices.popPose();
   }

   @Override
   public void sectionChanged() {
   }

   @Override
   public void close() {
   }
}
