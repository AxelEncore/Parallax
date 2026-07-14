package com.moulberry.axiom.render.annotations;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.MissingTextureImage;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.annotations.AnnotationHistoryElement;
import com.moulberry.axiom.annotations.AnnotationUpdateAction;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.annotations.data.ImageAnnotationData;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.Authorization;
import com.moulberry.axiom.utils.ColourUtils;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ImageAnnotation implements Annotation {
   private final ImageAnnotationData data;
   private final Gizmo gizmo;
   private final Quaternionf additionalRotation = new Quaternionf();
   private DynamicTexture dynamicTexture = null;
   private CompletableFuture<BufferedImage> future;
   private boolean error = false;

   public ImageAnnotation(ImageAnnotationData data) {
      this.data = data;
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
      if (this.error) {
         this.renderImage(rc, uuid, this.data.width(), this.data.width(), MissingTextureImage.getTextureId(), renderTarget);
      } else if (this.dynamicTexture == null) {
         this.createTexture();
         this.renderImage(rc, uuid, this.data.width(), this.data.width(), MissingTextureImage.getTextureId(), renderTarget);
      } else if (this.dynamicTexture.getPixels() != null) {
         float height = this.data.width() * this.dynamicTexture.getPixels().getHeight() / this.dynamicTexture.getPixels().getWidth();
         this.renderImage(rc, uuid, this.data.width(), height, new AxiomGpuTexture(this.dynamicTexture.getId()), renderTarget);
      }
   }

   private void createTexture() {
      if (this.future != null) {
         if (this.future.isDone()) {
            this.tryUploadtexture();
         }
      } else {
         this.future = downloadTexture(this.data.imageUrl());
      }
   }

   private void tryUploadtexture() {
      BufferedImage image;
      try {
         image = this.future.join();
      } catch (Exception var8) {
         Axiom.LOGGER.error("Failed to download annotation image", var8);
         this.error = true;
         return;
      }

      if (image == null) {
         this.error = true;
      } else {
         int width = image.getWidth();
         int height = image.getHeight();
         NativeImage nativeImage = new NativeImage(width, height, true);

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               int argb = image.getRGB(x, y);
               nativeImage.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
            }
         }

         this.dynamicTexture = VersionUtilsClient.helperCreateFromNativeImageDynamicTexture(null, nativeImage);
      }
   }

   private static CompletableFuture<BufferedImage> downloadTexture(String imageUrl) {
      CompletableFuture<BufferedImage> future = new CompletableFuture<>();
      Util.backgroundExecutor().execute(() -> {
         HttpURLConnection connection = null;
         Axiom.LOGGER.info("Downloading image annotation from: {}", imageUrl);

         try {
            connection = (HttpURLConnection)new URL(imageUrl).openConnection();
            connection.setRequestProperty("User-Agent", Authorization.getUserAgent());
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            if (connection.getResponseCode() / 100 == 2) {
               future.complete(ImageIO.read(connection.getInputStream()));
               connection = null;
               return;
            }

            future.completeExceptionally(new RuntimeException("Unable to download image: " + connection.getResponseCode()));
         } catch (Throwable var7) {
            future.completeExceptionally(var7);
            return;
         } finally {
            if (connection != null) {
               connection.disconnect();
            }
         }
      });
      return future;
   }

   private void renderImage(AxiomWorldRenderContext rc, UUID uuid, float width, float height, AxiomGpuTexture texture, RenderTarget renderTarget) {
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
            quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * directionYRot, (float) (Math.PI / 180.0) * directionXRot, 0.0F);
            break;
         case 1:
            quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * directionYRot, (float) (Math.PI / 180.0) * -rc.xRot(), 0.0F);
            break;
         case 2:
            quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * (rc.yRot() - 180.0F), (float) (Math.PI / 180.0) * directionXRot, 0.0F);
            break;
         default:
            quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * (rc.yRot() - 180.0F), (float) (Math.PI / 180.0) * -rc.xRot(), 0.0F);
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

      float halfWidth = width / 2.0F;
      float halfHeight = height / 2.0F;
      Vector3f a = quaternionf.transform(new Vector3f(-halfWidth, halfHeight, 0.0F));
      Vector3f b = quaternionf.transform(new Vector3f(halfWidth, halfHeight, 0.0F));
      Vector3f c = quaternionf.transform(new Vector3f(halfWidth, -halfHeight, 0.0F));
      Vector3f d = quaternionf.transform(new Vector3f(-halfWidth, -halfHeight, 0.0F));
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      Vec3 position = this.gizmo.getInterpPosition();
      matrices.translate(position.x - rc.x(), position.y - rc.y(), position.z - rc.z());
      Matrix4f pose = matrices.last().pose();
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferBuilder.addVertex(pose, a.x, a.y, a.z).setUv(0.0F, 0.0F);
      bufferBuilder.addVertex(pose, b.x, b.y, b.z).setUv(1.0F, 0.0F);
      bufferBuilder.addVertex(pose, c.x, c.y, c.z).setUv(1.0F, 1.0F);
      bufferBuilder.addVertex(pose, d.x, d.y, d.z).setUv(0.0F, 1.0F);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, this.data.opacity());
      AxiomRenderer.setMainTexture(texture);
      AxiomRenderPipelines.IMAGE_ANNOTATION_PIPELINE.render(renderTarget, provider.build());
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.popPose();
   }

   @Override
   public void sectionChanged() {
   }

   @Override
   public void close() {
      if (this.dynamicTexture != null) {
         this.dynamicTexture.close();
         this.dynamicTexture = null;
      }

      this.future = null;
   }
}
