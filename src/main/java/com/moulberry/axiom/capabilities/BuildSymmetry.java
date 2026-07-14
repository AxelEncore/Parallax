package com.moulberry.axiom.capabilities;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.buildertools.MirrorBuilderTool;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.BlockHelper;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class BuildSymmetry {
   private static BlockPos symmetryPoint = null;
   private static boolean flipX = false;
   private static boolean flipY = false;
   private static boolean flipZ = false;
   private static boolean rotY = false;

   public static boolean isActive() {
      return symmetryPoint != null && BuilderToolManager.selectedToolIs(MirrorBuilderTool.class);
   }

   public static boolean isActiveAndHasModifier() {
      return isActive() && (flipX || flipY || flipZ || rotY);
   }

   public static void clear() {
      symmetryPoint = null;
      rotY = false;
      flipZ = false;
      flipY = false;
      flipX = false;
   }

   public static void setSymmetryPoint(Vec3 vec) {
      symmetryPoint = new BlockPos((int)Math.round(vec.x * 2.0), (int)Math.round(vec.y * 2.0), (int)Math.round(vec.z * 2.0));
   }

   @Nullable
   public static Vec3 getSymmetryPoint() {
      return symmetryPoint == null ? null : new Vec3(symmetryPoint.getX() * 0.5F, symmetryPoint.getY() * 0.5F, symmetryPoint.getZ() * 0.5F);
   }

   public static boolean toggleRotY() {
      rotY = !rotY;
      return rotY;
   }

   public static boolean toggleFlipX() {
      flipX = !flipX;
      return flipX;
   }

   public static boolean toggleFlipY() {
      flipY = !flipY;
      return flipY;
   }

   public static boolean toggleFlipZ() {
      flipZ = !flipZ;
      return flipZ;
   }

   public static Map<BlockPos, BlockState> applySymmetry(Map<BlockPos, BlockState> original) {
      if (!isActiveAndHasModifier()) {
         return original;
      } else {
         Map<BlockPos, BlockState> mirrored = new LinkedHashMap<>();
         boolean doFlipX = flipX;
         boolean doFlipY = flipY;
         boolean doFlipZ = flipZ;
         int symmX = symmetryPoint.getX();
         int symmY = symmetryPoint.getY();
         int symmZ = symmetryPoint.getZ();
         if (rotY) {
            if (doFlipX && doFlipZ) {
               doFlipZ = false;
            }

            if ((symmX + symmZ & 1) != 0) {
               if (Math.abs(symmZ) > Math.abs(symmX)) {
                  symmZ = (int)(symmZ - Math.signum((float)symmZ));
               } else if (symmX != 0) {
                  symmX = (int)(symmX - Math.signum((float)symmX));
               }
            }

            for (Entry<BlockPos, BlockState> entry : original.entrySet()) {
               BlockPos pos = entry.getKey();
               int x = pos.getX();
               int z = pos.getZ();
               int newX = -Math.floorDiv(-(z * 2 - symmZ + symmX), 2);
               int newZ = Math.floorDiv(symmX + symmZ - x * 2 - 2, 2);
               if (newX != x || newZ != z) {
                  if (entry.getValue().isAir()) {
                     mirrored.put(new BlockPos(newX, pos.getY(), newZ), Blocks.AIR.defaultBlockState());
                  } else {
                     mirrored.putIfAbsent(new BlockPos(newX, pos.getY(), newZ), BlockHelper.rotateY(entry.getValue(), Rotation.COUNTERCLOCKWISE_90));
                  }
               }

               newX = symmX - x - 1;
               newZ = symmZ - z - 1;
               if (newX != x || newZ != z) {
                  if (entry.getValue().isAir()) {
                     mirrored.put(new BlockPos(newX, pos.getY(), newZ), Blocks.AIR.defaultBlockState());
                  } else {
                     mirrored.putIfAbsent(new BlockPos(newX, pos.getY(), newZ), BlockHelper.rotateY(entry.getValue(), Rotation.CLOCKWISE_180));
                  }
               }

               newX = Math.floorDiv(symmZ + symmX - z * 2 - 2, 2);
               newZ = -Math.floorDiv(-(x * 2 - symmX + symmZ), 2);
               if (newX != x || newZ != z) {
                  if (entry.getValue().isAir()) {
                     mirrored.put(new BlockPos(newX, pos.getY(), newZ), Blocks.AIR.defaultBlockState());
                  } else {
                     mirrored.putIfAbsent(new BlockPos(newX, pos.getY(), newZ), BlockHelper.rotateY(entry.getValue(), Rotation.CLOCKWISE_90));
                  }
               }
            }
         }

         if (doFlipX) {
            if (!mirrored.isEmpty()) {
               mirrored.putAll(original);
               original = mirrored;
               mirrored = new LinkedHashMap<>();
            }

            for (Entry<BlockPos, BlockState> entry : original.entrySet()) {
               BlockPos posx = entry.getKey();
               int xx = posx.getX();
               int newXx = symmX - xx - 1;
               if (newXx != xx) {
                  if (entry.getValue().isAir()) {
                     mirrored.put(new BlockPos(newXx, posx.getY(), posx.getZ()), Blocks.AIR.defaultBlockState());
                  } else {
                     mirrored.putIfAbsent(new BlockPos(newXx, posx.getY(), posx.getZ()), BlockHelper.flipX(entry.getValue()));
                  }
               }
            }
         }

         if (doFlipZ) {
            if (!mirrored.isEmpty()) {
               mirrored.putAll(original);
               original = mirrored;
               mirrored = new LinkedHashMap<>();
            }

            for (Entry<BlockPos, BlockState> entryx : original.entrySet()) {
               BlockPos posx = entryx.getKey();
               int zx = posx.getZ();
               int newZx = symmZ - zx - 1;
               if (newZx != zx) {
                  if (entryx.getValue().isAir()) {
                     mirrored.put(new BlockPos(posx.getX(), posx.getY(), newZx), Blocks.AIR.defaultBlockState());
                  } else {
                     mirrored.putIfAbsent(new BlockPos(posx.getX(), posx.getY(), newZx), BlockHelper.flipZ(entryx.getValue()));
                  }
               }
            }
         }

         if (doFlipY) {
            if (!mirrored.isEmpty()) {
               mirrored.putAll(original);
               original = mirrored;
               mirrored = new LinkedHashMap<>();
            }

            for (Entry<BlockPos, BlockState> entryxx : original.entrySet()) {
               BlockPos posx = entryxx.getKey();
               int y = posx.getY();
               int newY = symmY - y - 1;
               if (newY != y) {
                  if (entryxx.getValue().isAir()) {
                     mirrored.put(new BlockPos(posx.getX(), newY, posx.getZ()), Blocks.AIR.defaultBlockState());
                  } else {
                     mirrored.putIfAbsent(new BlockPos(posx.getX(), newY, posx.getZ()), BlockHelper.flipY(entryxx.getValue()));
                  }
               }
            }
         }

         mirrored.putAll(original);
         return mirrored;
      }
   }

   public static void renderWorld(AxiomWorldRenderContext rc) {
      if (symmetryPoint != null && isActive()) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            clear();
            return;
         }

         int symmetryRange = Axiom.configuration.builderTools.symmetryRange;
         if (player.distanceToSqr(symmetryPoint.getX() * 0.5F, player.getY(), symmetryPoint.getZ() * 0.5F) > symmetryRange * symmetryRange) {
            ChatUtils.warning("Build Symmetry disabled due to travelling more than " + symmetryRange + " blocks away");
            clear();
            return;
         }

         if (Minecraft.getInstance().options.hideGui) {
            return;
         }

         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(symmetryPoint.getX() * 0.5 - 0.1 - rc.x(), symmetryPoint.getY() * 0.5 - 0.1 - rc.y(), symmetryPoint.getZ() * 0.5 - 0.1 - rc.z());
         RenderHelper.tryApplyModelViewMatrix();
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         Pose pose = matrices.last();
         Matrix4f poseMatrix = pose.pose();
         BufferBuilder bufferBuilder = provider.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
         if (!flipX && !flipY && !flipZ && !rotY) {
            Shapes.shadedBoxTriangles(bufferBuilder, poseMatrix, 0.2F, 0.2F, 0.2F, -6250336);
         } else {
            Shapes.shadedBoxTriangles(bufferBuilder, poseMatrix, 0.2F, 0.2F, 0.2F, -256);
         }

         if (flipX) {
            Vec3 base = new Vec3(0.5, 0.1F, 0.1F);
            Vec3 tip = new Vec3(0.8F, 0.1F, 0.1F);
            Shapes.shadedCone(bufferBuilder, poseMatrix, base, tip, 0, 0.2F, -65536);
            base = new Vec3(-0.3F, 0.1F, 0.1F);
            tip = new Vec3(-0.6F, 0.1F, 0.1F);
            Shapes.shadedCone(bufferBuilder, poseMatrix, base, tip, 0, 0.2F, -65536);
         }

         if (flipY) {
            Vec3 base = new Vec3(0.1F, 0.5, 0.1F);
            Vec3 tip = new Vec3(0.1F, 0.8F, 0.1F);
            Shapes.shadedCone(bufferBuilder, poseMatrix, base, tip, 1, 0.2F, -16711936);
            base = new Vec3(0.1F, -0.3F, 0.1F);
            tip = new Vec3(0.1F, -0.6F, 0.1F);
            Shapes.shadedCone(bufferBuilder, poseMatrix, base, tip, 1, 0.2F, -16711936);
         }

         if (flipZ) {
            Vec3 base = new Vec3(0.1F, 0.1F, 0.5);
            Vec3 tip = new Vec3(0.1F, 0.1F, 0.8F);
            Shapes.shadedCone(bufferBuilder, poseMatrix, base, tip, 2, 0.2F, -16776961);
            base = new Vec3(0.1F, 0.1F, -0.3F);
            tip = new Vec3(0.1F, 0.1F, -0.6F);
            Shapes.shadedCone(bufferBuilder, poseMatrix, base, tip, 2, 0.2F, -16776961);
         }

         AxiomRenderer.setShaderColour(0.8F, 0.8F, 0.8F, 0.5F);
         MeshData box = bufferBuilder.build();
         AxiomRenderer.renderPipeline(AxiomRenderPipelines.POSITION_COLOR_IGNORE_DEPTH, null, box, false);
         if (rotY) {
            bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            AxiomRenderer.setLineWidthLegacy(RenderHelper.baseLineWidth);
            float lastX = 0.0F;
            float lastZ = 0.0F;

            for (int i = 0; i <= 36; i++) {
               double angle = Math.toRadians(i * 10);
               double sin = Math.sin(angle);
               double cos = Math.cos(angle);
               float x = (float)sin * 0.35F;
               float z = (float)cos * 0.35F;
               if (i > 0) {
                  float nx = x - lastX;
                  float nz = z - lastZ;
                  float norm = 1.0F / (float)Math.sqrt(nx * nx + nz * nz);
                  nx *= norm;
                  nz *= norm;
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, lastX + 0.1F, 0.1F, lastZ + 0.1F).setColor(-16711936).setNormal(pose, nx, 0.0F, nz),
                     RenderHelper.baseLineWidth
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, x + 0.1F, 0.1F, z + 0.1F).setColor(-16711936).setNormal(pose, nx, 0.0F, nz), RenderHelper.baseLineWidth
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, lastX * 0.8F + 0.1F, 0.1F, lastZ * 0.8F + 0.1F).setColor(-16711936).setNormal(pose, nx, 0.0F, nz),
                     RenderHelper.baseLineWidth
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, x * 0.8F + 0.1F, 0.1F, z * 0.8F + 0.1F).setColor(-16711936).setNormal(pose, nx, 0.0F, nz),
                     RenderHelper.baseLineWidth
                  );
               }

               lastX = x;
               lastZ = z;
            }

            MeshData meshData = bufferBuilder.build();
            AxiomRenderer.setShaderColour(0.8F, 0.8F, 0.8F, 0.5F);
            AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
            AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
            AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, true);
         }

         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         AxiomRenderer.renderPipeline(AxiomRenderPipelines.POSITION_COLOR, null, box, true);
         matrices.popPose();
      }
   }
}
