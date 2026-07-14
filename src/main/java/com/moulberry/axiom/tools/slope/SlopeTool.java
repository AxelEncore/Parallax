package com.moulberry.axiom.tools.slope;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.editor.widgets.FalloffWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.HeightmapApplier;
import com.moulberry.axiom.tools.SimpleRadiusAdjustment;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ProjectedText;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SlopeTool implements Tool {
   private final ChunkedBooleanRegion previewRegion = new ChunkedBooleanRegion();
   private int lastPreviewRadius = -1;
   private int lastPreviewHeight = -1;
   private AxiomDrawBuffer gridBuffer = null;
   private final HeightmapApplier heightmapApplier = new HeightmapApplier();
   private boolean usingTool = false;
   private BlockPos lastPosition = null;
   private MaskElement cachedSourceMask = null;
   private MaskContext cachedMaskContext = null;
   private final float[] falloffLut = new float[1057];
   private int falloffLutSize = this.falloffLut.length;
   private BlockPos currentTargetPos = null;
   private BlockPos pendingTargetPos = null;
   private Vec3 pendingTargetPosVec3 = null;
   private float coneSlope = 0.0F;
   private Vec3 planeNormal = null;
   private double planarK = 0.0;
   private static final int MODE_RAISE = 0;
   private static final int MODE_LOWER = 1;
   private static final int MODE_BOTH_RAISE_LOWER = 2;
   private static final int SHAPE_PLANE = 0;
   private static final int SHAPE_CONE = 1;
   private final int[] radius = new int[]{8};
   private final int[] height = new int[]{63};
   private final float[] smoothing = new float[]{1.0F};
   private final int[] raiseLowerMode = new int[]{0};
   private int shape = 0;
   private final FalloffWidget falloffWidget = new FalloffWidget();
   private final PresetWidget presetWidget = new PresetWidget(this, "slope");
   private final float[] baseRadiusAdjustment = new float[]{0.0F};

   @Override
   public void reset() {
      if (this.usingTool) {
         this.usingTool = false;
         ChunkRenderOverrider.release("slope_tool");
      }

      this.cachedSourceMask = null;
      this.cachedMaskContext = null;
      this.previewRegion.clear();
      this.lastPreviewRadius = -1;
      this.lastPreviewHeight = -1;
      this.pendingTargetPos = null;
      this.pendingTargetPosVec3 = null;
      this.lastPosition = null;
      this.planeNormal = null;
      this.planarK = 0.0;
      this.coneSlope = 0.0F;
      this.heightmapApplier.reset(this.smoothing[0]);
      if (this.gridBuffer != null) {
         this.gridBuffer.close();
         this.gridBuffer = null;
      }
   }

   @Override
   public void toolDeselected() {
      this.falloffWidget.unload();
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case ESCAPE:
         case DELETE:
            if (this.pendingTargetPos != null || this.usingTool) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
         case RIGHT_MOUSE:
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, false);
            if (result == null) {
               return UserAction.ActionResult.USED_STOP;
            } else if (this.pendingTargetPos == null) {
               this.pendingTargetPos = result.blockPos();
               this.pendingTargetPosVec3 = result.worldPos();
               return UserAction.ActionResult.USED_STOP;
            } else {
               this.currentTargetPos = this.pendingTargetPos;
               this.reset();
               Vec3 first = Vec3.atCenterOf(this.currentTargetPos);
               Vec3 second = Vec3.atCenterOf(result.blockPos());
               if (this.shape == 0) {
                  if (first.distanceTo(second) < 0.5) {
                     this.planeNormal = new Vec3(0.0, 1.0, 0.0);
                  } else {
                     double dx = first.x - second.x;
                     double dy = first.y - second.y;
                     double dz = first.z - second.z;
                     if (dy == 0.0) {
                        this.planeNormal = new Vec3(0.0, 1.0, 0.0);
                     } else {
                        double horzDist = Math.sqrt(dx * dx + dz * dz);
                        this.planeNormal = new Vec3(dx / horzDist, -1.0 / (dy / horzDist), dz / horzDist).normalize();
                     }
                  }

                  this.planarK = -(first.x * this.planeNormal.x + first.y * this.planeNormal.y + first.z * this.planeNormal.z);
               } else {
                  double dx = first.x - second.x;
                  double dy = first.y - second.y;
                  double dz = first.z - second.z;
                  this.coneSlope = (float)(dy / Math.sqrt(dx * dx + dz * dz));
               }

               int radius = this.radius[0];
               this.falloffLutSize = Math.min(this.falloffLut.length, radius * radius + radius + 1);
               FloatUnaryOperator operator = this.falloffWidget.getFalloffFunction();

               for (int i = 0; i < this.falloffLutSize; i++) {
                  double distance = Math.sqrt(i) / Math.sqrt(this.falloffLutSize - 1);
                  this.falloffLut[i] = operator.apply((float)distance);
               }

               if (!this.usingTool) {
                  this.usingTool = true;
                  ChunkRenderOverrider.acquire("slope_tool");
               }

               return UserAction.ActionResult.USED_STOP;
            }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      Selection.render(rc, 4);
      if (!this.usingTool) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, false, false);
         if (result == null) {
            return;
         }

         int radius = this.radius[0];
         int height = this.height[0];
         if (this.lastPreviewRadius != radius || this.lastPreviewHeight != height) {
            this.lastPreviewRadius = radius;
            this.lastPreviewHeight = height;
            this.previewRegion.clear();
            float maxRadiusSq = (radius + 0.5F) * (radius + 0.5F);

            for (int x = -radius; x <= radius; x++) {
               for (int z = -radius; z <= radius; z++) {
                  if (x * x + z * z < maxRadiusSq) {
                     for (int y = -height; y <= height; y++) {
                        this.previewRegion.add(x, y, z);
                     }
                  }
               }
            }
         }

         if (this.pendingTargetPos != null) {
            this.previewRegion.render(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 2);
            this.renderTargetTrianglePreview(rc, result);
         } else {
            this.previewRegion.render(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
         }
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         String countString = NumberFormat.getInstance().format((long)this.heightmapApplier.blockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.slope_tool", countString);
         RegionHelper.pushBlockRegionChange(this.heightmapApplier.blockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            return;
         }

         int radiusx = this.radius[0];
         int heightx = this.height[0];
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         Vec3 lookFrom = player.getEyePosition();
         Vec3 look = Tool.getLookDirection();
         if (look != null) {
            double t;
            if (this.shape == 0) {
               float y0 = -((float)((lookFrom.x * this.planeNormal.x + lookFrom.z * this.planeNormal.z + this.planarK) / this.planeNormal.y));
               double denom = this.planeNormal.x * look.x + this.planeNormal.y * look.y + this.planeNormal.z * look.z;
               if (Math.abs(denom) > 1.0E-5F) {
                  t = (y0 - lookFrom.y) * this.planeNormal.y / denom;
               } else {
                  t = Double.NaN;
               }
            } else {
               Vec3 from = lookFrom.subtract(this.currentTargetPos.getX() + 0.5F, this.currentTargetPos.getY() + 0.5F, this.currentTargetPos.getZ() + 0.5F);
               double c = from.y * from.y - this.coneSlope * this.coneSlope * (from.x * from.x + from.z * from.z);
               double b = 2.0 * look.y * from.y - this.coneSlope * this.coneSlope * (2.0 * look.x * from.x + 2.0 * look.z * from.z);
               double a = look.y * look.y - this.coneSlope * this.coneSlope * (look.x * look.x + look.z * look.z);
               if (Math.abs(a) < 1.0E-5) {
                  t = Double.NaN;
               } else {
                  double discrim = b * b - 4.0 * a * c;
                  if (Math.abs(discrim) < 1.0E-5) {
                     t = -b / (2.0 * a);
                     if (t < 0.0 || from.y + t * look.y < 0.0 == this.coneSlope < 0.0F) {
                        t = Double.NaN;
                     }
                  } else if (discrim < 0.0) {
                     t = Double.NaN;
                  } else {
                     double sqrt = Math.sqrt(discrim);
                     double t1 = (-b + sqrt) / (2.0 * a);
                     double t2 = (-b - sqrt) / (2.0 * a);
                     if (t1 < 0.0 || from.y + t1 * look.y < 0.0 == this.coneSlope < 0.0F) {
                        t1 = Double.NaN;
                     }

                     if (t2 < 0.0 || from.y + t2 * look.y < 0.0 == this.coneSlope < 0.0F) {
                        t2 = Double.NaN;
                     }

                     if (!Double.isFinite(t1)) {
                        t = t2;
                     } else if (!Double.isFinite(t2)) {
                        t = t1;
                     } else {
                        t = Math.min(t1, t2);
                     }
                  }
               }
            }

            if (t > 0.0 && t < 1024.0) {
               int intersectionX = (int)Math.round(lookFrom.x + t * look.x);
               int intersectionY = (int)Math.round(lookFrom.y + t * look.y);
               int intersectionZ = (int)Math.round(lookFrom.z + t * look.z);
               if (this.lastPosition == null) {
                  this.apply(level, radiusx, heightx, mutableBlockPos, intersectionX, intersectionY, intersectionZ);
                  this.lastPosition = new BlockPos(intersectionX, intersectionY, intersectionZ);
               } else {
                  BlockPos newPosition = new BlockPos(intersectionX, intersectionY, intersectionZ);
                  Rasterization3D.ddaSkipFrom(this.lastPosition, newPosition, (x, y, zx) -> this.apply(level, radiusx, heightx, mutableBlockPos, x, y, zx));
                  this.lastPosition = newPosition;
               }
            } else {
               this.lastPosition = null;
            }
         }

         if (this.currentTargetPos != null) {
            if (this.gridBuffer == null) {
               this.renderGridBuffer();
            }

            PoseStack matrix = rc.poseStack();
            matrix.pushPose();
            matrix.translate(this.currentTargetPos.getX() - rc.x(), this.currentTargetPos.getY() - rc.y(), this.currentTargetPos.getZ() - rc.z());
            RenderHelper.pushModelViewMatrix(matrix.last().pose());
            RenderHelper.pushDisableFog();
            AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(this.gridBuffer);
            RenderHelper.popDisableFog();
            RenderHelper.popModelViewStack();
            matrix.popPose();
         }

         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.heightmapApplier.update();
         this.heightmapApplier.blockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         this.heightmapApplier.removeRegion.render(rc, Vec3.ZERO, 8);
      }
   }

   private void renderTargetTrianglePreview(AxiomWorldRenderContext rc, RayCaster.RaycastResult result) {
      Vec3 from = this.pendingTargetPosVec3;
      Vec3 to = result.worldPos();
      Vec3 mid = from.lerp(to, 0.5);
      from = from.subtract(mid);
      to = to.subtract(mid);
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double horzDist = Math.sqrt(dx * dx + dz * dz);
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      float nx = (float)(dx / dist);
      float ny = (float)(dy / dist);
      float nz = (float)(dz / dist);
      float nxh = (float)(dx / horzDist);
      float nzh = (float)(dz / horzDist);
      PoseStack matrix = rc.poseStack();
      matrix.pushPose();
      matrix.translate(mid.x - rc.x(), mid.y - rc.y(), mid.z - rc.z());
      Pose pose = matrix.last();
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)from.x, (float)from.y, (float)from.z).setColor(-2147418368).setNormal(pose, nx, ny, nz),
         RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)to.x, (float)to.y, (float)to.z).setColor(-2147418368).setNormal(pose, nx, ny, nz), RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)from.x, (float)from.y, (float)from.z).setColor(-2147418368).setNormal(pose, 0.0F, 1.0F, 0.0F),
         RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)from.x, (float)to.y, (float)from.z).setColor(-2147418368).setNormal(pose, 0.0F, 1.0F, 0.0F),
         RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)from.x, (float)to.y, (float)from.z).setColor(-2147418368).setNormal(pose, nxh, 0.0F, nzh),
         RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)to.x, (float)to.y, (float)to.z).setColor(-2147418368).setNormal(pose, nxh, 0.0F, nzh), RenderHelper.baseLineWidth
      );
      MeshData meshData = bufferBuilder.build();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
      AxiomRenderer.setShaderColour(0.66F, 0.66F, 0.66F, 0.875F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      this.renderInfoText(rc, result, from, to);
      matrix.popPose();
   }

   private void renderInfoText(AxiomWorldRenderContext rc, RayCaster.RaycastResult result, Vec3 from, Vec3 to) {
      ProjectedText.setupProjectedText();
      int targetHeight = Math.abs(this.pendingTargetPos.getY() - result.getBlockPos().getY());
      if (targetHeight > 0) {
         ProjectedText.renderProjectedText(
            String.valueOf(targetHeight), rc.poseStack(), rc.projection(), (float)from.x, (float)(from.y + to.y) / 2.0F, (float)from.z
         );
      }

      int dx = this.pendingTargetPos.getX() - result.getBlockPos().getX();
      int dz = this.pendingTargetPos.getZ() - result.getBlockPos().getZ();
      float targetHorizontalDistance = (float)Math.sqrt(dx * dx + dz * dz);
      if (targetHorizontalDistance > 0.0F) {
         ProjectedText.renderProjectedText(
            String.format("%.2f", targetHorizontalDistance),
            rc.poseStack(),
            rc.projection(),
            (float)(from.x + to.x) / 2.0F,
            (float)to.y,
            (float)(from.z + to.z) / 2.0F
         );
      }

      float targetAngle = (float)Math.abs(Math.atan2(targetHeight, targetHorizontalDistance));
      ProjectedText.renderProjectedText(
         String.format("%.2f°", Math.toDegrees(targetAngle)),
         rc.poseStack(),
         rc.projection(),
         (float)(from.x + to.x) / 2.0F,
         (float)(from.y + to.y) / 2.0F,
         (float)(from.z + to.z) / 2.0F
      );
      ProjectedText.finishProjectedText();
   }

   private void renderGridBuffer() {
      if (this.gridBuffer == null) {
         this.gridBuffer = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
      }

      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      int horzColour = -1438588720;
      int vertColour = -2147418368;
      if (this.shape == 0) {
         double angle = Math.atan2(this.planeNormal.x, this.planeNormal.z);

         for (int h = -256; h <= 256; h += 8) {
            float x1 = (float)(h * Math.sin(angle) - 256.0 * Math.cos(angle));
            float x2 = (float)(h * Math.sin(angle) + 256.0 * Math.cos(angle));
            float z1 = (float)(h * Math.cos(angle) + 256.0 * Math.sin(angle));
            float z2 = (float)(h * Math.cos(angle) - 256.0 * Math.sin(angle));
            x1 = (float)(x1 + (this.currentTargetPos.getX() + 0.5));
            x2 = (float)(x2 + (this.currentTargetPos.getX() + 0.5));
            z1 = (float)(z1 + (this.currentTargetPos.getZ() + 0.5));
            z2 = (float)(z2 + (this.currentTargetPos.getZ() + 0.5));
            float y1 = -((float)(((x1 + 0.5) * this.planeNormal.x + (z1 + 0.5) * this.planeNormal.z + this.planarK) / this.planeNormal.y))
               - this.currentTargetPos.getY();
            float y2 = -((float)(((x2 + 0.5) * this.planeNormal.x + (z2 + 0.5) * this.planeNormal.z + this.planarK) / this.planeNormal.y))
               - this.currentTargetPos.getY();
            x1 -= this.currentTargetPos.getX();
            x2 -= this.currentTargetPos.getX();
            z1 -= this.currentTargetPos.getZ();
            z2 -= this.currentTargetPos.getZ();
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float distance = (float)Math.sqrt(dx * dx + dz * dz + dy * dy);
            float nx = dx / distance;
            float ny = dy / distance;
            float nz = dz / distance;
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1, y1, z1).setColor(horzColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.25F, y1 + dy * 0.25F, z1 + dz * 0.25F).setColor(horzColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.25F, y1 + dy * 0.25F, z1 + dz * 0.25F).setColor(horzColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.5F, y1 + dy * 0.5F, z1 + dz * 0.5F).setColor(horzColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.5F, y1 + dy * 0.5F, z1 + dz * 0.5F).setColor(horzColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.75F, y1 + dy * 0.75F, z1 + dz * 0.75F).setColor(horzColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.75F, y1 + dy * 0.75F, z1 + dz * 0.75F).setColor(horzColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x2, y2, z2).setColor(horzColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
         }

         angle++;

         for (int v = -256; v <= 256; v += 8) {
            float x1 = (float)(v * Math.sin(angle) - 256.0 * Math.cos(angle));
            float x2 = (float)(v * Math.sin(angle) + 256.0 * Math.cos(angle));
            float z1 = (float)(v * Math.cos(angle) + 256.0 * Math.sin(angle));
            float z2 = (float)(v * Math.cos(angle) - 256.0 * Math.sin(angle));
            x1 = (float)(x1 + (this.currentTargetPos.getX() + 0.5));
            x2 = (float)(x2 + (this.currentTargetPos.getX() + 0.5));
            z1 = (float)(z1 + (this.currentTargetPos.getZ() + 0.5));
            z2 = (float)(z2 + (this.currentTargetPos.getZ() + 0.5));
            float y1 = -((float)(((x1 + 0.5) * this.planeNormal.x + (z1 + 0.5) * this.planeNormal.z + this.planarK) / this.planeNormal.y))
               - this.currentTargetPos.getY();
            float y2 = -((float)(((x2 + 0.5) * this.planeNormal.x + (z2 + 0.5) * this.planeNormal.z + this.planarK) / this.planeNormal.y))
               - this.currentTargetPos.getY();
            x1 -= this.currentTargetPos.getX();
            x2 -= this.currentTargetPos.getX();
            z1 -= this.currentTargetPos.getZ();
            z2 -= this.currentTargetPos.getZ();
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float distance = (float)Math.sqrt(dx * dx + dz * dz + dy * dy);
            float nx = dx / distance;
            float ny = dy / distance;
            float nz = dz / distance;
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1, y1, z1).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.25F, y1 + dy * 0.25F, z1 + dz * 0.25F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.25F, y1 + dy * 0.25F, z1 + dz * 0.25F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.5F, y1 + dy * 0.5F, z1 + dz * 0.5F).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.5F, y1 + dy * 0.5F, z1 + dz * 0.5F).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.75F, y1 + dy * 0.75F, z1 + dz * 0.75F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.75F, y1 + dy * 0.75F, z1 + dz * 0.75F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x2, y2, z2).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
         }
      } else {
         for (int angle = 0; angle < 360; angle += 8) {
            float sin = (float)Math.sin(Math.toRadians(angle));
            float cos = (float)Math.cos(Math.toRadians(angle));
            float sinPlus8 = (float)Math.sin(Math.toRadians(angle + 8));
            float cosPlus8 = (float)Math.cos(Math.toRadians(angle + 8));

            for (int v = 8; v <= 256; v += 8) {
               float y = 0.5F - this.coneSlope * v;
               float x1 = 0.5F + v * sin;
               float z1 = 0.5F + v * cos;
               float x2 = 0.5F + v * sinPlus8;
               float z2 = 0.5F + v * cosPlus8;
               float dx = x2 - x1;
               float dz = z2 - z1;
               float distance = (float)Math.sqrt(dx * dx + dz * dz);
               float nx = dx / distance;
               float nz = dz / distance;
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1, y, z1).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1 + dx * 0.25F, y, z1 + dz * 0.25F).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1 + dx * 0.25F, y, z1 + dz * 0.25F).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1 + dx * 0.5F, y, z1 + dz * 0.5F).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1 + dx * 0.5F, y, z1 + dz * 0.5F).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1 + dx * 0.75F, y, z1 + dz * 0.75F).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x1 + dx * 0.75F, y, z1 + dz * 0.75F).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
               VersionUtilsClient.legacySetLineWidthIgnored(
                  bufferBuilder.addVertex(x2, y, z2).setColor(horzColour).setNormal(nx, 0.0F, nz), RenderHelper.baseLineWidth
               );
            }

            float x1 = 0.5F;
            float y1 = 0.5F;
            float z1 = 0.5F;
            float x2 = x1 + 256.0F * sin;
            float y2 = y1 - this.coneSlope * 256.0F;
            float z2 = z1 + 256.0F * cos;
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dz = z2 - z1;
            float distance = (float)Math.sqrt(dx * dx + dz * dz + dy * dy);
            float nx = dx / distance;
            float ny = dy / distance;
            float nz = dz / distance;
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1, y1, z1).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.25F, y1 + dy * 0.25F, z1 + dz * 0.25F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.25F, y1 + dy * 0.25F, z1 + dz * 0.25F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.5F, y1 + dy * 0.5F, z1 + dz * 0.5F).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.5F, y1 + dy * 0.5F, z1 + dz * 0.5F).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.75F, y1 + dy * 0.75F, z1 + dz * 0.75F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x1 + dx * 0.75F, y1 + dy * 0.75F, z1 + dz * 0.75F).setColor(vertColour).setNormal(nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(x2, y2, z2).setColor(vertColour).setNormal(nx, ny, nz), RenderHelper.baseLineWidth
            );
         }
      }

      MeshData meshData = bufferBuilder.build();
      if (meshData != null) {
         this.gridBuffer.upload(meshData);
      }
   }

   private void apply(ClientLevel level, int maxRadius, int height, MutableBlockPos mutableBlockPos, int x, int y, int z) {
      float maxRadiusSq = (maxRadius + 0.5F) * (maxRadius + 0.5F);
      float expandedMaxRadiusSq = (maxRadius + 1.5F) * (maxRadius + 1.5F);
      if (this.cachedSourceMask == null) {
         this.cachedSourceMask = MaskManager.getSourceMask();
         this.cachedMaskContext = new MaskContext(level);
      }

      for (int xo = -maxRadius - 1; xo <= maxRadius + 1; xo++) {
         label85:
         for (int zo = -maxRadius - 1; zo <= maxRadius + 1; zo++) {
            int radiusSq = xo * xo + zo * zo;
            if (radiusSq <= expandedMaxRadiusSq) {
               double desiredY;
               if (this.shape == 0) {
                  desiredY = -((x + xo + 0.5) * this.planeNormal.x + (z + zo + 0.5) * this.planeNormal.z + this.planarK) / this.planeNormal.y;
               } else {
                  int dx = x + xo - this.currentTargetPos.getX();
                  int dz = z + zo - this.currentTargetPos.getZ();
                  desiredY = 0.5 + this.currentTargetPos.getY() - this.coneSlope * Math.sqrt(dx * dx + dz * dz);
               }

               int currentOriginalHeight = this.heightmapApplier.getOriginalY(x + xo, z + zo);
               if (currentOriginalHeight != Integer.MIN_VALUE) {
                  if (radiusSq <= maxRadiusSq) {
                     float falloff = this.falloffLut[radiusSq * (this.falloffLutSize - 1) / (maxRadius * maxRadius + maxRadius)];
                     int newY = (int)Math.floor((currentOriginalHeight + 0.5) * (1.0F - falloff) + desiredY * falloff);
                     if (this.raiseLowerMode[0] == 0) {
                        newY = Math.max(newY, currentOriginalHeight);
                     } else if (this.raiseLowerMode[0] == 1) {
                        newY = Math.min(newY, currentOriginalHeight);
                     }

                     int oldModifiedHeight = this.heightmapApplier.getModifiedY(x + xo, z + zo);
                     if (oldModifiedHeight == Integer.MIN_VALUE) {
                        this.heightmapApplier.setModifiedY(x + xo, z + zo, newY);
                     } else {
                        double newDesiredDelta = Math.abs(newY - desiredY);
                        double oldDesiredDelta = Math.abs(oldModifiedHeight - desiredY);
                        if (newDesiredDelta < oldDesiredDelta) {
                           this.heightmapApplier.setModifiedY(x + xo, z + zo, newY);
                        }
                     }
                  }
               } else {
                  int motionBlockingHeight = level.getHeight(Types.MOTION_BLOCKING, x + xo, z + zo);
                  int startY = Math.min(motionBlockingHeight, y + height);

                  for (int h = startY; h >= y - height; h--) {
                     mutableBlockPos.set(x + xo, h, z + zo);
                     BlockState block = level.getBlockState(mutableBlockPos);
                     if (block.blocksMotion()) {
                        if (radiusSq <= maxRadiusSq && this.cachedSourceMask.test(this.cachedMaskContext.reset(), x + xo, h, z + zo)) {
                           float falloffx = this.falloffLut[radiusSq * (this.falloffLutSize - 1) / (maxRadius * maxRadius + maxRadius)];
                           int newYx = (int)Math.floor((h + 0.5) * (1.0F - falloffx) + desiredY * falloffx);
                           if (this.raiseLowerMode[0] == 0) {
                              newYx = Math.max(newYx, h);
                           } else if (this.raiseLowerMode[0] == 1) {
                              newYx = Math.min(newYx, h);
                           }

                           this.heightmapApplier.setOriginalY(x + xo, z + zo, h);
                           this.heightmapApplier.setModifiedY(x + xo, z + zo, newYx);
                           continue label85;
                        }

                        int oldModifiedY = this.heightmapApplier.getModifiedY(x + xo, z + zo);
                        if (oldModifiedY == Integer.MIN_VALUE || oldModifiedY > h) {
                           this.heightmapApplier.setModifiedY(x + xo, z + zo, h);
                        }
                        continue label85;
                     }
                  }

                  int temporaryHeight = Math.min(motionBlockingHeight, y - height - 1);
                  int oldModifiedY = this.heightmapApplier.getModifiedY(x + xo, z + zo);
                  if (oldModifiedY == Integer.MIN_VALUE || oldModifiedY > temporaryHeight) {
                     this.heightmapApplier.setModifiedY(x + xo, z + zo, temporaryHeight);
                  }
               }
            }
         }
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = ImGui.sliderInt(AxiomI18n.get("axiom.tool.generic.brush_radius"), this.radius, 1, 32);
      int[] modifiedHeight = new int[]{this.height[0] + 1};
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.heightmap.y_limit"), modifiedHeight, 1, 64);
      this.height[0] = modifiedHeight[0] - 1;
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.slope"));
      changed |= ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.elevation.mode"),
         this.raiseLowerMode,
         new String[]{
            AxiomI18n.get("axiom.tool.elevation.raise"), AxiomI18n.get("axiom.tool.elevation.lower"), AxiomI18n.get("axiom.tool.elevation.raise_and_lower")
         }
      );
      changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.heightmap.smoothing"), this.smoothing, 0.0F, 1.0F);
      int newShape = -1;
      if (this.shape == 0) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.slope.shape_plane"))) {
         newShape = 0;
      }

      if (this.shape == 0) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      if (this.shape == 1) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.slope.shape_cone"))) {
         newShape = 1;
      }

      if (this.shape == 1) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      ImGui.text(AxiomI18n.get("axiom.tool.slope.shape"));
      if (newShape != -1) {
         this.shape = newShape;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.heightmap.falloff"));
      changed |= this.falloffWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
   }

   @Override
   public String listenForEsc() {
      return this.pendingTargetPos == null && !this.usingTool ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public boolean initiateAdjustment() {
      return SimpleRadiusAdjustment.initiateAdjustment(this.radius, this.baseRadiusAdjustment);
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return SimpleRadiusAdjustment.renderAdjustment(mouseX, mouseY, mouseDelta, 32, this.radius, this.baseRadiusAdjustment);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.slope");
   }

   @Override
   public Tutorial getTutorial() {
      return Tutorial.SLOPE_TOOL;
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.falloffWidget.writeSettings(tag);
      tag.putInt("BrushRadius", this.radius[0]);
      tag.putInt("Height", this.height[0]);
      tag.putFloat("Smoothing", this.smoothing[0]);
      tag.putByte("RaiseLowerMode", (byte)this.raiseLowerMode[0]);
      tag.putByte("Shape", (byte)this.shape);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.falloffWidget.loadSettings(tag);
      this.radius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushRadius", 8);
      this.height[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Height", 63);
      this.smoothing[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Smoothing", 1.0F);
      this.raiseLowerMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "RaiseLowerMode", 0);
      this.shape = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Shape", 0);
   }

   @Override
   public char iconChar() {
      return '\ue918';
   }

   @Override
   public String keybindId() {
      return "slope";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_SLOPE, AxiomPermission.BUILD_SECTION);
   }
}
