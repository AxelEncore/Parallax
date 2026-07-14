package com.moulberry.axiom.editor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import com.moulberry.axiom.GlobalCleaner;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.FramebufferUtils;
import com.moulberry.axiom.utils.ProjectionMatrixBackup;
import com.moulberry.axiom.utils.RenderHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30;

public class BlueprintPreview {
   private ChunkedBlockRegion blockRegion = null;
   private BlueprintPreview.CleanState state = null;
   private float yaw = 135.0F;
   private float pitch = 30.0F;
   private float snappedYaw = 135.0F;
   private float snappedPitch = 30.0F;
   private boolean wasSnapped = false;

   public int render(int size, boolean grayBackground, boolean boundingBox) {
      if (this.blockRegion.count() >= 16777216) {
         return -1;
      } else if (this.blockRegion.isEmpty()) {
         return -1;
      } else {
         if (this.state == null) {
            this.state = new BlueprintPreview.CleanState();
            GlobalCleaner.INSTANCE.register(this, this.state);
         }

         this.state.textureTarget = FramebufferUtils.resizeOrCreateFramebuffer(this.state.textureTarget, size, size);
         RenderTarget textureTarget = this.state.textureTarget;
         if (grayBackground) {
            FramebufferUtils.clear(textureTarget, 1280068684);
         } else {
            FramebufferUtils.clear(textureTarget, 0);
         }

         int sizeX = this.blockRegion.max().getX() + 1 - this.blockRegion.min().getX();
         int sizeY = this.blockRegion.max().getY() + 1 - this.blockRegion.min().getY();
         int sizeZ = this.blockRegion.max().getZ() + 1 - this.blockRegion.min().getZ();
         float centerX = (this.blockRegion.min().getX() + this.blockRegion.max().getX() + 1) / 2.0F;
         float centerY = (this.blockRegion.min().getY() + this.blockRegion.max().getY() + 1) / 2.0F;
         float centerZ = (this.blockRegion.min().getZ() + this.blockRegion.max().getZ() + 1) / 2.0F;
         Quaternionf quaternionf = new Quaternionf();
         quaternionf.rotateAxis((float)Math.toRadians(this.snappedPitch), new Vector3f(1.0F, 0.0F, 0.0F));
         quaternionf.rotateAxis((float)Math.toRadians(this.snappedYaw), new Vector3f(0.0F, 1.0F, 0.0F));
         Matrix4f projectionMatrix = new Matrix4f().setPerspective((float)Math.toRadians(60.0), 1.0F, 0.1F, 1000.0F);
         FrustumIntersection intersection = new FrustumIntersection();
         intersection.set(projectionMatrix);
         float tanHalfFov = (float)Math.tan(Math.toRadians(60.0) * 0.5);
         float distance = 0.0F;

         for (int t1 = 0; t1 < 8; t1++) {
            Vector3f vec = new Vector3f(
               this.blockRegion.min().getX() - centerX + sizeX * (t1 & 1),
               this.blockRegion.min().getY() - centerY + sizeY * (t1 & 2) / 2.0F,
               this.blockRegion.min().getZ() - centerZ + sizeZ * (t1 & 4) / 4.0F
            );
            vec.rotate(quaternionf);

            for (int t2 = t1 + 1; t2 < 8; t2++) {
               Vector3f vec2 = new Vector3f(
                  this.blockRegion.min().getX() - centerX + sizeX * (t2 & 1),
                  this.blockRegion.min().getY() - centerY + sizeY * (t2 & 2) / 2.0F,
                  this.blockRegion.min().getZ() - centerZ + sizeZ * (t2 & 4) / 4.0F
               );
               vec2.rotate(quaternionf);
               distance = Math.max(distance, Math.abs(vec.x - vec2.x) / (2.0F * tanHalfFov) + Math.abs(vec.z + vec2.z) / 2.0F);
               distance = Math.max(distance, Math.abs(vec.y - vec2.y) / (2.0F * tanHalfFov) + Math.abs(vec.z + vec2.z) / 2.0F);
            }
         }

         distance *= -1.01F;
         float diagonalMultiplier = (float)Math.sin((Math.PI / 2) - Math.toRadians(60.0) * 0.5);
         float[] mins = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};

         for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
               for (int z = 0; z <= 1; z++) {
                  Vector3f vec = new Vector3f(
                     this.blockRegion.min().getX() - centerX + sizeX * x,
                     this.blockRegion.min().getY() - centerY + sizeY * y,
                     this.blockRegion.min().getZ() - centerZ + sizeZ * z
                  );
                  vec.rotate(quaternionf);

                  for (int i = 0; i < 4; i++) {
                     float to = intersection.distanceToPlane(vec.x, vec.y, vec.z + distance, vec.x, vec.y, vec.z + distance, i) / diagonalMultiplier;
                     if (to < mins[i]) {
                        mins[i] = to;
                     }
                  }
               }
            }
         }

         PoseStack modelViewStack = new PoseStack();
         float translationX = (mins[1] - mins[0]) / 2.0F;
         float translationY = (mins[3] - mins[2]) / 2.0F;
         modelViewStack.translate(translationX, translationY, distance);
         modelViewStack.mulPose(quaternionf);
         RenderHelper.pushModelViewMatrix(modelViewStack.last().pose());
         ProjectionMatrixBackup backup = ProjectionMatrixBackup.create();
         RenderHelper.setPerspectiveProjectionMatrix(0.1F, 1000.0F, 60.0F, size, size);
         AxiomWorldRenderContext rc = new AxiomWorldRenderContext(
            null, modelViewStack, 0.0F, Util.getNanos(), Vec3.ZERO, BlockPos.ZERO, 0.0F, 0.0F, new Quaternionf(), projectionMatrix
         );
         this.blockRegion.render(rc, new Vec3(-centerX, -centerY, -centerZ), null, 1.0F, 0.0F, false, textureTarget);
         if (boundingBox) {
            float BOUNDING_BOX_LINE_WIDTH = 4.0F;
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            Shapes.lineBox(
               new PoseStack(),
               bufferBuilder,
               this.blockRegion.min().getX() - centerX,
               this.blockRegion.min().getY() - centerY,
               this.blockRegion.min().getZ() - centerZ,
               this.blockRegion.max().getX() + 1 - centerX,
               this.blockRegion.max().getY() + 1 - centerY,
               this.blockRegion.max().getZ() + 1 - centerZ,
               1.0F,
               1.0F,
               1.0F,
               0.5F,
               0.0F,
               0.0F,
               0.0F,
               4.0F
            );
            AxiomRenderer.setLineWidthLegacy(4.0F);
            AxiomRenderPipelines.LINES_IGNORE_DEPTH_WITH_CUSTOM_WIDTH.render(textureTarget, bufferBuilder.build());
            AxiomRenderer.setLineWidthLegacy(RenderHelper.baseLineWidth);
         }

         backup.restore();
         RenderHelper.popModelViewStack();
         int textureId = FramebufferUtils.getRawColorTextureId(this.state.textureTarget);
         GlStateManager._bindTexture(textureId);
         GL30.glTexParameteri(3553, 10241, 9729);
         GL30.glTexParameteri(3553, 10240, 9729);
         return textureId;
      }
   }

   public CompletableFuture<NativeImage> toNativeImage(int size, boolean crop) {
      return FramebufferUtils.downloadAsync(this.state.textureTarget).thenApply(nativeImage -> {
         int minX = size * 10;
         int maxX = 0;
         int minY = size * 10;
         int maxY = 0;
         if (crop) {
            for (int x = 0; x < size * 10; x++) {
               for (int y = 0; y < size * 10; y++) {
                  int argb = ColourUtils.abgrToArgb(nativeImage.getPixelRGBA(x, y));
                  int alpha = argb >> 24 & 0xFF;
                  if (alpha > 20) {
                     if (x < minX) {
                        minX = x;
                     }

                     if (y < minY) {
                        minY = y;
                     }

                     if (x + 1 > maxX) {
                        maxX = x + 1;
                     }

                     if (y + 1 > maxY) {
                        maxY = y + 1;
                     }
                  }
               }
            }
         }

         if (minX > maxX) {
            minX = 0;
            maxX = size * 10;
         }

         if (minY > maxY) {
            minY = 0;
            maxY = size * 10;
         }

         int newWidth = maxX - minX;
         int newHeight = maxY - minY;
         if (newWidth < newHeight) {
            minX -= (newHeight - newWidth) / 2;
            if (minX < 0) {
               minX = 0;
            }

            newWidth = maxX - minX;
            maxX += newHeight - newWidth;
            if (maxX > size * 10) {
               maxX = size * 10;
            }
         } else if (newWidth > newHeight) {
            minY -= (newWidth - newHeight) / 2;
            if (minY < 0) {
               minY = 0;
            }

            newHeight = maxY - minY;
            maxY += newWidth - newHeight;
            if (maxY > size * 10) {
               maxY = size * 10;
            }
         }

         BufferedImage bufferedImage = new BufferedImage(maxX - minX, maxY - minY, 2);

         for (int x = minX; x < maxX; x++) {
            for (int yx = minY; yx < maxY; yx++) {
               bufferedImage.setRGB(x - minX, yx - minY, ColourUtils.abgrToArgb(nativeImage.getPixelRGBA(x, yx)));
            }
         }

         ResampleOp resampleOp = new ResampleOp(size, size);
         resampleOp.setFilter(ResampleFilters.getLanczos3Filter());
         BufferedImage scaledImage = resampleOp.filter(bufferedImage, null);
         nativeImage.close();
         nativeImage = new NativeImage(size, size, false);

         for (int x = 0; x < size; x++) {
            for (int yx = 0; yx < size; yx++) {
               int argb = scaledImage.getRGB(x, yx);
               int alpha = argb >> 24 & 0xFF;
               if (alpha <= 20) {
                  nativeImage.setPixelRGBA(x, yx, ColourUtils.argbToAbgr(0));
               } else {
                  nativeImage.setPixelRGBA(x, yx, ColourUtils.argbToAbgr(argb));
               }
            }
         }

         return nativeImage;
      });
   }

   public void mouseMoved(float dx, float dy, boolean snap) {
      ImVec2 size = ImGui.getMainViewport().getSize();
      float speed = snap ? 360.0F : 180.0F;
      float min = Math.min(size.x, size.y);
      if (!snap && this.wasSnapped) {
         this.yaw = this.snappedYaw;
         this.pitch = this.snappedPitch;
         this.wasSnapped = false;
      } else {
         this.wasSnapped = snap;
         this.yaw = (this.yaw + dx / min * speed) % 360.0F;
         this.pitch = Mth.clamp(this.pitch + dy / min * speed, -90.0F, 90.0F);
         if (this.yaw < -180.0F) {
            this.yaw += 360.0F;
         }

         if (this.yaw >= 180.0F) {
            this.yaw -= 360.0F;
         }

         if (snap) {
            this.snappedYaw = snap(this.yaw);
            this.snappedPitch = snap(this.pitch);
         } else {
            this.snappedYaw = Math.round(this.yaw / 0.01F) * 0.01F;
            this.snappedPitch = Math.round(this.pitch / 0.01F) * 0.01F;
         }
      }
   }

   private static float snap(float in) {
      in %= 360.0F;
      if (in < -180.0F) {
         in += 360.0F;
      }

      if (in > 180.0F) {
         in -= 360.0F;
      }

      float[] snapTo = new float[]{
         -180.0F, -150.0F, -135.0F, -120.0F, -90.0F, -60.0F, -45.0F, -30.0F, 0.0F, 30.0F, 45.0F, 60.0F, 90.0F, 120.0F, 135.0F, 150.0F, 180.0F
      };
      int closestIndex = 0;
      float closestDistance = 360.0F;

      for (int i = 0; i < snapTo.length; i++) {
         float distance = Math.abs(in - snapTo[i]);
         if (distance < closestDistance) {
            closestDistance = distance;
            closestIndex = i;
         }
      }

      return snapTo[closestIndex];
   }

   public void mouseReleased() {
      this.yaw = this.snappedYaw;
      this.pitch = this.snappedPitch;
      this.wasSnapped = false;
   }

   public void clear() {
      this.blockRegion = null;
   }

   public float getYaw() {
      return this.snappedYaw;
   }

   public float getPitch() {
      return this.snappedPitch;
   }

   public void setYaw(float yaw, boolean snap) {
      if (snap) {
         this.yaw = this.snappedYaw = snap(yaw);
      } else {
         this.yaw = this.snappedYaw = Math.round(yaw / 0.01F) * 0.01F;
      }
   }

   public void setPitch(float pitch, boolean snap) {
      if (snap) {
         this.pitch = this.snappedPitch = snap(pitch);
      } else {
         this.pitch = this.snappedPitch = Math.round(pitch / 0.01F) * 0.01F;
      }
   }

   public void setBlockRegion(ChunkedBlockRegion blockRegion) {
      this.blockRegion = blockRegion;
      this.yaw = 135.0F;
      this.pitch = 30.0F;
      this.snappedYaw = 135.0F;
      this.snappedPitch = 30.0F;
      this.wasSnapped = false;
   }

   private static final class CleanState implements Runnable, AutoCloseable {
      private RenderTarget textureTarget = null;

      @Override
      public void run() {
         EditorUI.deferredClose(this);
      }

      @Override
      public void close() {
         this.textureTarget.destroyBuffers();
      }
   }
}
