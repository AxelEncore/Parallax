package com.moulberry.axiom.tools.modify;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.clipboard.SelectionHistoryElement;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.funcinterfaces.TriIntPredicate;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.scaling.RotSprite;
import com.moulberry.axiom.scaling.Scale3x;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.utils.ProjectedText;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModifyTool implements Tool {
   private final int[] mode = new int[]{0};
   private final int[] revolveAngle = new int[]{360};
   private final int[] axis = new int[]{1};
   private final int[] count = new int[]{1};
   private final int[] translationType = new int[]{0};
   private final int[] translationX = new int[]{1};
   private final int[] translationY = new int[]{0};
   private final int[] translationZ = new int[]{0};
   private final int[] rotationCount = new int[]{7};
   private final int[] rotationType = new int[]{0};
   private final int[] rotationX = new int[]{0};
   private final int[] rotationY = new int[]{45};
   private final int[] rotationZ = new int[]{0};
   private boolean arrayRotateAddTranslation = false;
   private final int[] arrayRotateTranslationX = new int[]{0};
   private final int[] arrayRotateTranslationY = new int[]{7};
   private final int[] arrayRotateTranslationZ = new int[]{0};
   private final int[] twistDegrees = new int[]{45};
   private final float[] twistAxis = new float[]{0.0F, 1.0F, 0.0F};

   @Override
   public void reset() {
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            int mode = this.mode[0];
            if (mode == 0) {
               this.rightClickRevolve();
            } else if (mode == 1) {
               this.rightClickArrayTranslate();
            } else if (mode == 2) {
               this.rightClickArrayRotate();
            } else if (mode == 3) {
               this.rightClickTwist();
            }

            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Nullable
   private static TriIntPredicate createMaskPredicate() {
      MaskElement maskElement = MaskManager.getDestMaskWithoutDefaultSelection();
      if (maskElement instanceof ConstantMaskElement constantMaskElement) {
         return constantMaskElement.getConstant() ? (x, y, z) -> true : null;
      } else {
         MaskContext maskContext = new MaskContext(Objects.requireNonNull(Minecraft.getInstance().level));
         return (x, y, z) -> {
            maskContext.reset();
            return maskElement.test(maskContext, x, y, z);
         };
      }
   }

   public void rightClickRevolve() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result != null) {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         if (!selectionBuffer.isEmpty()) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               BlockPos translation = BlockPos.ZERO;
               if (this.arrayRotateAddTranslation) {
                  translation = new BlockPos(this.arrayRotateTranslationX[0], this.arrayRotateTranslationY[0], this.arrayRotateTranslationZ[0]);
               }

               TriIntPredicate mask = createMaskPredicate();
               if (mask != null) {
                  double angle = Math.toRadians(this.revolveAngle[0]);
                  int axis = this.axis[0];
                  ChunkedBlockRegion chunkedBlockRegion = ModifyRevolve.revolve(level, selectionBuffer, result.blockPos(), angle, axis, translation, mask);
                  if (!chunkedBlockRegion.isEmpty()) {
                     SelectionHistoryElement historyElement = Selection.getSelectionBuffer().createHistoryElement();
                     Selection.clearSelection();
                     String countString = NumberFormat.getInstance().format((long)chunkedBlockRegion.count());
                     String historyDescription = AxiomI18n.get("axiom.history_description.modified", countString);
                     RegionHelper.pushBlockRegionChange(chunkedBlockRegion, null, historyDescription, 0, historyElement);
                  }
               }
            }
         }
      }
   }

   public void rightClickArrayTranslate() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
            BlockPos max = selectionBuffer.max();
            BlockPos min = selectionBuffer.min();
            if (!selectionBuffer.isEmpty() && max != null && min != null) {
               TriIntPredicate mask = createMaskPredicate();
               if (mask != null) {
                  ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
                  int count = this.count[0];
                  int deltaX;
                  int deltaY;
                  int deltaZ;
                  if (this.translationType[0] == 0) {
                     deltaX = this.translationX[0] * (max.getX() - min.getX() + 1);
                     deltaY = this.translationY[0] * (max.getY() - min.getY() + 1);
                     deltaZ = this.translationZ[0] * (max.getZ() - min.getZ() + 1);
                  } else {
                     deltaX = this.translationX[0];
                     deltaY = this.translationY[0];
                     deltaZ = this.translationZ[0];
                  }

                  MutableBlockPos mutableBlockPos = new MutableBlockPos();
                  if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
                     int minX = aabb.min().getX();
                     int minY = aabb.min().getY();
                     int minZ = aabb.min().getZ();
                     int maxX = aabb.max().getX();
                     int maxY = aabb.max().getY();
                     int maxZ = aabb.max().getZ();

                     for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                           for (int z = minZ; z <= maxZ; z++) {
                              BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));

                              for (int i = 1; i < count + 1; i++) {
                                 if (mask.test(x + deltaX * i, y + deltaY * i, z + deltaZ * i)) {
                                    chunkedBlockRegion.addBlockWithoutDirty(x + deltaX * i, y + deltaY * i, z + deltaZ * i, blockState);
                                 }
                              }
                           }
                        }
                     }
                  } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
                     set.forEach((xx, yx, zx) -> {
                        BlockState blockStatex = level.getBlockState(mutableBlockPos.set(xx, yx, zx));

                        for (int ix = 1; ix < count + 1; ix++) {
                           if (mask.test(xx + deltaX * ix, yx + deltaY * ix, zx + deltaZ * ix)) {
                              chunkedBlockRegion.addBlockWithoutDirty(xx + deltaX * ix, yx + deltaY * ix, zx + deltaZ * ix, blockStatex);
                           }
                        }
                     });
                  }

                  SelectionHistoryElement historyElement = Selection.getSelectionBuffer().createHistoryElement();
                  Selection.move(deltaX * count, deltaY * count, deltaZ * count, false);
                  String countString = NumberFormat.getInstance().format((long)chunkedBlockRegion.count());
                  String historyDescription = AxiomI18n.get("axiom.history_description.modified", countString);
                  RegionHelper.pushBlockRegionChange(chunkedBlockRegion, null, historyDescription, 0, historyElement);
               }
            }
         }
      }
   }

   public void rightClickArrayRotate() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
            BlockPos max = selectionBuffer.max();
            BlockPos min = selectionBuffer.min();
            if (!selectionBuffer.isEmpty() && max != null && min != null) {
               int offsetX = Math.floorDiv(max.getX() + min.getX(), 2);
               int offsetY = Math.floorDiv(max.getY() + min.getY(), 2);
               int offsetZ = Math.floorDiv(max.getZ() + min.getZ(), 2);
               ChunkedBlockRegion in = new ChunkedBlockRegion();
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
                  int minX = aabb.min().getX();
                  int minY = aabb.min().getY();
                  int minZ = aabb.min().getZ();
                  int maxX = aabb.max().getX();
                  int maxY = aabb.max().getY();
                  int maxZ = aabb.max().getZ();

                  for (int x = minX; x <= maxX; x++) {
                     for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                           BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                           if (!blockState.isAir()) {
                              in.addBlockWithoutDirty(x - offsetX, y - offsetY, z - offsetZ, blockState);
                           }
                        }
                     }
                  }
               } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
                  set.forEach((x, y, zx) -> {
                     BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, zx));
                     if (!blockState.isAir()) {
                        in.addBlockWithoutDirty(x - offsetX, y - offsetY, zx - offsetZ, blockState);
                     }
                  });
               }

               ChunkedBlockRegion scaled = Scale3x.scale3x(in, false);
               int deltaX = result.blockPos().getX() - offsetX;
               int deltaY = result.blockPos().getY() - offsetY;
               int deltaZ = result.blockPos().getZ() - offsetZ;
               ChunkedBlockRegion out = new ChunkedBlockRegion();
               int rotationCount = this.rotationCount[0];
               float rotationX;
               float rotationY;
               float rotationZ;
               if (this.rotationType[0] == 0) {
                  rotationX = 0.0F;
                  rotationY = (float) (Math.PI * 2) / (rotationCount + 1);
                  rotationZ = 0.0F;
               } else {
                  rotationX = (float)Math.toRadians(this.rotationX[0]);
                  rotationY = (float)Math.toRadians(this.rotationY[0]);
                  rotationZ = (float)Math.toRadians(this.rotationZ[0]);
               }

               for (int i = 1; i <= rotationCount; i++) {
                  Matrix4f matrix4f = new Matrix4f();
                  matrix4f.rotateYXZ(rotationY * i, rotationX * i, rotationZ * i);
                  Vector4f offset = new Vector4f(deltaX, deltaY, deltaZ, 1.0F);
                  matrix4f.transform(offset);
                  BlockPos position = new BlockPos(
                     result.blockPos().getX() - Math.round(offset.x),
                     result.blockPos().getY() - Math.round(offset.y),
                     result.blockPos().getZ() - Math.round(offset.z)
                  );
                  if (this.arrayRotateAddTranslation) {
                     position = position.offset(
                        Math.round((float)(this.arrayRotateTranslationX[0] * i) / rotationCount),
                        Math.round((float)(this.arrayRotateTranslationY[0] * i) / rotationCount),
                        Math.round((float)(this.arrayRotateTranslationZ[0] * i) / rotationCount)
                     );
                  }

                  RotSprite.rotateCachedWithOutput(scaled, matrix4f, out, position.getX(), position.getY(), position.getZ());
               }

               MaskElement maskElement = MaskManager.getDestMaskWithoutDefaultSelection();
               if (maskElement instanceof ConstantMaskElement constantMaskElement) {
                  if (!constantMaskElement.getConstant()) {
                     return;
                  }
               } else {
                  MaskContext maskContext = new MaskContext(Objects.requireNonNull(Minecraft.getInstance().level));
                  ChunkedBlockRegion checkMaskOut = new ChunkedBlockRegion();
                  out.forEachEntry((x, y, zx, blockState) -> {
                     if (maskElement.test(maskContext.reset(), x, y, zx)) {
                        checkMaskOut.addBlockWithoutDirty(x, y, zx, blockState);
                     }
                  });
                  out = checkMaskOut;
               }

               SelectionHistoryElement historyElement = Selection.getSelectionBuffer().createHistoryElement();
               Selection.clearSelection();
               String countString = NumberFormat.getInstance().format((long)out.count());
               String historyDescription = AxiomI18n.get("axiom.history_description.modified", countString);
               RegionHelper.pushBlockRegionChange(out, null, historyDescription, 0, historyElement);
            }
         }
      }
   }

   public void rightClickTwist() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         BlockPos max = selectionBuffer.max();
         BlockPos min = selectionBuffer.min();
         if (!selectionBuffer.isEmpty() && max != null && min != null) {
            float angleDegrees = this.twistDegrees[0];
            float axisX = this.twistAxis[0];
            float axisY = this.twistAxis[1];
            float axisZ = this.twistAxis[2];
            float invLength = 1.0F / (float)Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
            if (Float.isFinite(invLength)) {
               axisX *= invLength;
               axisY *= invLength;
               axisZ *= invLength;
               TriIntPredicate mask = createMaskPredicate();
               if (mask != null) {
                  ChunkedBlockRegion out = ModifyTwist.twist(angleDegrees, axisX, axisY, axisZ, mask);
                  if (out != null) {
                     SelectionHistoryElement historyElement = Selection.getSelectionBuffer().createHistoryElement();
                     Selection.clearSelection();
                     String countString = NumberFormat.getInstance().format((long)out.count());
                     String historyDescription = AxiomI18n.get("axiom.history_description.modified", countString);
                     RegionHelper.pushBlockRegionChange(out, null, historyDescription, 0, historyElement);
                  }
               }
            }
         }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      Selection.render(rc, 7);
      int mode = this.mode[0];
      if (mode == 0) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            return;
         }

         renderRevolveOverlay(rc, result, this.axis[0]);
      } else if (mode == 1) {
         this.renderArrayTranslateOverlay(rc);
      } else if (mode == 2) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            return;
         }

         int axis;
         if (this.rotationType[0] == 0) {
            axis = 1;
         } else if (this.rotationX[0] != 0 && this.rotationY[0] == 0 && this.rotationZ[0] == 0) {
            axis = 0;
         } else if (this.rotationX[0] == 0 && this.rotationY[0] != 0 && this.rotationZ[0] == 0) {
            axis = 1;
         } else if (this.rotationX[0] == 0 && this.rotationY[0] == 0 && this.rotationZ[0] != 0) {
            axis = 2;
         } else {
            axis = -1;
         }

         if (axis >= 0) {
            renderRevolveOverlay(rc, result, axis);
         }

         this.renderArrayRotateOverlay(rc, result);
      } else if (mode == 3) {
         this.renderTwistOverlay(rc);
      }
   }

   private static void renderRevolveOverlay(AxiomWorldRenderContext rc, RayCaster.RaycastResult result, int axis) {
      BlockPos offsetTarget = result.blockPos().relative(result.direction());
      BlockPos center = Selection.getSelectionBuffer().center();
      BlockPos min = Selection.getSelectionBuffer().min();
      BlockPos max = Selection.getSelectionBuffer().max();
      if (center != null && min != null && max != null) {
         BlockPos target;
         if (axis == 0) {
            int x = Math.max(min.getX(), Math.min(max.getX(), offsetTarget.getX()));
            target = new BlockPos(x, result.blockPos().getY(), result.blockPos().getZ());
         } else if (axis == 1) {
            int y = Math.max(min.getY(), Math.min(max.getY(), offsetTarget.getY()));
            target = new BlockPos(result.blockPos().getX(), y, result.blockPos().getZ());
         } else {
            int z = Math.max(min.getZ(), Math.min(max.getZ(), offsetTarget.getZ()));
            target = new BlockPos(result.blockPos().getX(), result.blockPos().getY(), z);
         }

         BlockPos furthest = PositionUtils.findFurthestPoint(min, max, target);
         BlockPos furthestDelta = furthest.subtract(target);
         BlockPos opposite = target.subtract(furthestDelta);
         if (axis == 0) {
            opposite = new BlockPos(target.getX(), opposite.getY(), opposite.getZ());
         } else if (axis == 1) {
            opposite = new BlockPos(opposite.getX(), target.getY(), opposite.getZ());
         } else if (axis == 2) {
            opposite = new BlockPos(opposite.getX(), opposite.getY(), target.getZ());
         }

         BlockPos closest = PositionUtils.findClosestPoint(min, max, target);
         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(target.getX() - rc.x(), target.getY() - rc.y(), target.getZ() - rc.z());
         Vec3 inner = Vec3.atLowerCornerOf(closest.subtract(target));
         float innerLength = (float)inner.length();
         Vec3 delta = inner.normalize();
         inner = inner.add(0.5 - delta.x * 0.5, 0.5 - delta.y * 0.5, 0.5 - delta.z * 0.5);
         Vec3 outer = Vec3.atLowerCornerOf(opposite.subtract(target));
         float outerLength = (float)outer.length();
         delta = outer.normalize();
         outer = outer.add(0.5 + delta.x * 0.5, 0.5 + delta.y * 0.5, 0.5 + delta.z * 0.5);
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         Pose pose = matrices.last();
         drawLine(bufferBuilder, pose, 0.5F, 0.5F, 0.5F, (float)outer.x, (float)outer.y, (float)outer.z, true);
         drawLine(bufferBuilder, pose, 0.5F, 0.5F, 0.5F, (float)inner.x, (float)inner.y, (float)inner.z, false);
         float centerX = 0.5F;
         float centerY = 0.5F;
         float centerZ = 0.5F;
         float lastX = 0.0F;
         float lastZ = 1.0F;

         for (int i = 1; i <= 360; i++) {
            float x = (float)Math.sin(Math.toRadians(i));
            float z = (float)Math.cos(Math.toRadians(i));
            if (axis == 0) {
               drawLine(
                  bufferBuilder,
                  pose,
                  centerX,
                  centerY + lastX * outerLength,
                  centerZ + lastZ * outerLength,
                  centerX,
                  centerY + x * outerLength,
                  centerZ + z * outerLength,
                  true
               );
               drawLine(
                  bufferBuilder,
                  pose,
                  centerX,
                  centerY + lastX * innerLength,
                  centerZ + lastZ * innerLength,
                  centerX,
                  centerY + x * innerLength,
                  centerZ + z * innerLength,
                  false
               );
            } else if (axis == 1) {
               drawLine(
                  bufferBuilder,
                  pose,
                  centerX + lastX * outerLength,
                  centerY,
                  centerZ + lastZ * outerLength,
                  centerX + x * outerLength,
                  centerY,
                  centerZ + z * outerLength,
                  true
               );
               drawLine(
                  bufferBuilder,
                  pose,
                  centerX + lastX * innerLength,
                  centerY,
                  centerZ + lastZ * innerLength,
                  centerX + x * innerLength,
                  centerY,
                  centerZ + z * innerLength,
                  false
               );
            } else if (axis == 2) {
               drawLine(
                  bufferBuilder,
                  pose,
                  centerX + lastX * outerLength,
                  centerY + lastZ * outerLength,
                  centerZ,
                  centerX + x * outerLength,
                  centerY + z * outerLength,
                  centerZ,
                  true
               );
               drawLine(
                  bufferBuilder,
                  pose,
                  centerX + lastX * innerLength,
                  centerY + lastZ * innerLength,
                  centerZ,
                  centerX + x * innerLength,
                  centerY + z * innerLength,
                  centerZ,
                  false
               );
            }

            lastX = x;
            lastZ = z;
         }

         AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
         ProjectedText.setupProjectedText();
         ProjectedText.renderProjectedText(
            String.format("%.1f", outerLength),
            matrices,
            rc.projection(),
            (opposite.getX() - target.getX()) / 2.0F + 0.5F,
            (opposite.getY() - target.getY()) / 2.0F + 0.5F,
            (opposite.getZ() - target.getZ()) / 2.0F + 0.5F
         );
         if (innerLength >= 0.25) {
            ProjectedText.renderProjectedText(
               String.format("%.1f", innerLength),
               matrices,
               rc.projection(),
               (closest.getX() - target.getX()) / 2.0F + 0.5F,
               (closest.getY() - target.getY()) / 2.0F + 0.5F,
               (closest.getZ() - target.getZ()) / 2.0F + 0.5F
            );
         }

         ProjectedText.finishProjectedText();
         matrices.popPose();
         if (axis == 0) {
            ScreenRenderHook.setOverlayText(
               Component.literal(AxiomI18n.get("axiom.hardcoded.offset_prefix"))
                  .withStyle(ChatFormatting.YELLOW)
                  .append(Component.literal(result.blockPos().getY() - center.getY() + " ").withStyle(ChatFormatting.GREEN))
                  .append(Component.literal(result.blockPos().getZ() - center.getZ() + "").withStyle(ChatFormatting.AQUA))
            );
         } else if (axis == 1) {
            ScreenRenderHook.setOverlayText(
               Component.literal(AxiomI18n.get("axiom.hardcoded.offset_prefix"))
                  .withStyle(ChatFormatting.YELLOW)
                  .append(Component.literal(result.blockPos().getX() - center.getX() + " ").withStyle(ChatFormatting.RED))
                  .append(Component.literal(result.blockPos().getZ() - center.getZ() + "").withStyle(ChatFormatting.AQUA))
            );
         } else if (axis == 2) {
            ScreenRenderHook.setOverlayText(
               Component.literal(AxiomI18n.get("axiom.hardcoded.offset_prefix"))
                  .withStyle(ChatFormatting.YELLOW)
                  .append(Component.literal(result.blockPos().getX() - center.getX() + " ").withStyle(ChatFormatting.RED))
                  .append(Component.literal(result.blockPos().getY() - center.getY() + "").withStyle(ChatFormatting.GREEN))
            );
         }
      }
   }

   private void renderArrayTranslateOverlay(AxiomWorldRenderContext rc) {
      int count = this.count[0];
      if (count <= 100) {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         BlockPos max = selectionBuffer.max();
         BlockPos min = selectionBuffer.min();
         if (!selectionBuffer.isEmpty() && max != null && min != null) {
            int deltaX;
            int deltaY;
            int deltaZ;
            if (this.translationType[0] == 0) {
               deltaX = this.translationX[0] * (max.getX() - min.getX() + 1);
               deltaY = this.translationY[0] * (max.getY() - min.getY() + 1);
               deltaZ = this.translationZ[0] * (max.getZ() - min.getZ() + 1);
            } else {
               deltaX = this.translationX[0];
               deltaY = this.translationY[0];
               deltaZ = this.translationZ[0];
            }

            PoseStack matrices = rc.poseStack();
            matrices.pushPose();
            matrices.translate(min.getX() - rc.x(), min.getY() - rc.y(), min.getZ() - rc.z());
            AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            float sizeX = max.getX() + 1 - min.getX();
            float sizeY = max.getY() + 1 - min.getY();
            float sizeZ = max.getZ() + 1 - min.getZ();

            for (int i = 1; i < count + 1; i++) {
               float red = 0.33333334F;
               float green = 1.0F;
               float blue = 1.0F;
               Shapes.lineBox(
                  matrices,
                  bufferBuilder,
                  deltaX * i,
                  deltaY * i,
                  deltaZ * i,
                  sizeX + deltaX * i,
                  sizeY + deltaY * i,
                  sizeZ + deltaZ * i,
                  red,
                  green,
                  blue,
                  1.0F,
                  red,
                  green,
                  blue,
                  RenderHelper.baseLineWidth
               );
            }

            AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
            matrices.popPose();
         }
      }
   }

   private void renderArrayRotateOverlay(AxiomWorldRenderContext rc, RayCaster.RaycastResult result) {
      int count = this.rotationCount[0];
      if (count <= 100) {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         BlockPos max = selectionBuffer.max();
         BlockPos min = selectionBuffer.min();
         if (!selectionBuffer.isEmpty() && max != null && min != null) {
            int offsetX = Math.floorDiv(max.getX() + min.getX(), 2);
            int offsetY = Math.floorDiv(max.getY() + min.getY(), 2);
            int offsetZ = Math.floorDiv(max.getZ() + min.getZ(), 2);
            int deltaX = result.blockPos().getX() - offsetX;
            int deltaY = result.blockPos().getY() - offsetY;
            int deltaZ = result.blockPos().getZ() - offsetZ;
            AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            float rotationX;
            float rotationY;
            float rotationZ;
            if (this.rotationType[0] == 0) {
               rotationX = 0.0F;
               rotationY = (float) (Math.PI * 2) / (count + 1);
               rotationZ = 0.0F;
            } else {
               rotationX = (float)Math.toRadians(this.rotationX[0]);
               rotationY = (float)Math.toRadians(this.rotationY[0]);
               rotationZ = (float)Math.toRadians(this.rotationZ[0]);
            }

            for (int i = 1; i <= count; i++) {
               float red = 0.33333334F;
               float green = 1.0F;
               float blue = 1.0F;
               Matrix4f matrix4f = new Matrix4f();
               matrix4f.rotateYXZ(rotationY * i, rotationX * i, rotationZ * i);
               Vector4f offset = new Vector4f(deltaX, deltaY, deltaZ, 1.0F);
               matrix4f.transform(offset);
               PoseStack matrices = rc.poseStack();
               matrices.pushPose();
               matrices.translate(
                  result.blockPos().getX() - Math.round(offset.x) + 0.5 - rc.x(),
                  result.blockPos().getY() - Math.round(offset.y) + 0.5 - rc.y(),
                  result.blockPos().getZ() - Math.round(offset.z) + 0.5 - rc.z()
               );
               if (this.arrayRotateAddTranslation) {
                  matrices.translate(
                     Math.round((float)(this.arrayRotateTranslationX[0] * i) / count),
                     Math.round((float)(this.arrayRotateTranslationY[0] * i) / count),
                     Math.round((float)(this.arrayRotateTranslationZ[0] * i) / count)
                  );
               }

               Pose pose = matrices.last();
               pose.pose().rotateYXZ(rotationY * i, rotationX * i, rotationZ * i);
               pose.normal().rotateYXZ(rotationY * i, rotationX * i, rotationZ * i);
               Shapes.lineBox(
                  matrices,
                  bufferBuilder,
                  min.getX() - 0.5F - offsetX,
                  min.getY() - 0.5F - offsetY,
                  min.getZ() - 0.5F - offsetZ,
                  max.getX() + 0.5F - offsetX,
                  max.getY() + 0.5F - offsetY,
                  max.getZ() + 0.5F - offsetZ,
                  red,
                  green,
                  blue,
                  1.0F,
                  red,
                  green,
                  blue,
                  RenderHelper.baseLineWidth
               );
               matrices.popPose();
            }

            AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
         }
      }
   }

   private void renderTwistOverlay(AxiomWorldRenderContext rc) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      BlockPos max = selectionBuffer.max();
      BlockPos min = selectionBuffer.min();
      if (!selectionBuffer.isEmpty() && max != null && min != null) {
         float angleDegrees = this.twistDegrees[0];
         float axisX = this.twistAxis[0];
         float axisY = this.twistAxis[1];
         float axisZ = this.twistAxis[2];
         float invLength = 1.0F / (float)Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
         if (Float.isFinite(invLength)) {
            axisX *= invLength;
            axisY *= invLength;
            axisZ *= invLength;
            int offsetX = Math.floorDiv(max.getX() + min.getX(), 2);
            int offsetY = Math.floorDiv(max.getY() + min.getY(), 2);
            int offsetZ = Math.floorDiv(max.getZ() + min.getZ(), 2);
            float minDot = Float.MAX_VALUE;
            float maxDot = Float.MIN_VALUE;
            int minX = min.getX();
            int minY = min.getY();
            int minZ = min.getZ();
            int maxX = max.getX();
            int maxY = max.getY();
            int maxZ = max.getZ();

            for (int x = minX; x <= maxX + 1; x = maxX + 1) {
               for (int y = minY; y <= maxY + 1; y = maxY + 1) {
                  for (int z = minZ; z <= maxZ + 1; z = maxZ + 1) {
                     float dot = (x - offsetX) * axisX + (y - offsetY) * axisY + (z - offsetZ) * axisZ;
                     minDot = Math.min(minDot, dot);
                     maxDot = Math.max(maxDot, dot);
                     if (z == maxZ + 1) {
                        break;
                     }
                  }

                  if (y == maxY + 1) {
                     break;
                  }
               }

               if (x == maxX + 1) {
                  break;
               }
            }

            if (!(maxDot - minDot < 1.0E-4)) {
               PoseStack matrices = rc.poseStack();
               matrices.pushPose();
               matrices.translate(offsetX - rc.x(), offsetY - rc.y(), offsetZ - rc.z());
               AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
               VertexConsumerProvider provider = VertexConsumerProvider.shared();
               BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               float red = 0.33333334F;
               float green = 1.0F;
               float blue = 1.0F;
               Matrix4f matrix = new Matrix4f();
               Vector4f vector4f = new Vector4f();
               Pose pose = matrices.last();
               Vector3f[] last = new Vector3f[4];

               for (Axis axis : Axis.values()) {
                  Arrays.fill(last, null);

                  for (int v = min.get(axis); v <= max.get(axis) + 1; v++) {
                     for (int i = 0; i < 4; i++) {
                        int x;
                        int y;
                        int zx;
                        switch (axis) {
                           case X:
                              x = v;
                              y = (i & 1) != 0 ? max.getY() + 1 : min.getY();
                              zx = (i & 2) != 0 ? max.getZ() + 1 : min.getZ();
                              break;
                           case Y:
                              x = (i & 1) != 0 ? max.getX() + 1 : min.getX();
                              y = v;
                              zx = (i & 2) != 0 ? max.getZ() + 1 : min.getZ();
                              break;
                           case Z:
                              x = (i & 1) != 0 ? max.getX() + 1 : min.getX();
                              y = (i & 2) != 0 ? max.getY() + 1 : min.getY();
                              zx = v;
                              break;
                           default:
                              throw new FaultyImplementationError();
                        }

                        float dot = (x - offsetX) * axisX + (y - offsetY) * axisY + (zx - offsetZ) * axisZ;
                        float angle = (float)Math.toRadians(angleDegrees) * (dot - minDot) / (maxDot - minDot);
                        matrix.rotation(angle, axisX, axisY, axisZ);
                        vector4f.set(x - offsetX - 0.5, y - offsetY - 0.5, zx - offsetZ - 0.5, 1.0);
                        matrix.transform(vector4f);
                        float currX = vector4f.x + 0.5F;
                        float currY = vector4f.y + 0.5F;
                        float currZ = vector4f.z + 0.5F;
                        if (last[i] == null) {
                           last[i] = new Vector3f(currX, currY, currZ);
                        } else {
                           Shapes.line(bufferBuilder, pose, red, green, blue, currX, currY, currZ, last[i].x, last[i].y, last[i].z);
                           last[i].set(currX, currY, currZ);
                        }
                     }
                  }
               }

               AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
               matrices.popPose();
            }
         }
      }
   }

   private static void drawLine(BufferBuilder bufferBuilder, Pose pose, float fromX, float fromY, float fromZ, float toX, float toY, float toZ, boolean green) {
      Shapes.line(bufferBuilder, pose, green ? 0.1F : 1.0F, green ? 1.0F : 0.1F, 0.1F, fromX, fromY, fromZ, toX, toY, toZ);
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.combo("Mode", this.mode, new String[]{"Revolve", "Translate Copies", "Rotate Copies", "Twist"});
      String action = Keybinds.USE_TOOL.longKeyIdentifier();
      int mode = this.mode[0];
      if (mode == 0) {
         ImGui.sliderInt("Angle", this.revolveAngle, 0, 360);
         ImGuiHelper.combo("Axis", this.axis, new String[]{"X", "Y", "Z"});
         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.add_translation"), this.arrayRotateAddTranslation)) {
            this.arrayRotateAddTranslation = !this.arrayRotateAddTranslation;
         }

         if (this.arrayRotateAddTranslation) {
            ImGuiHelper.inputInt("Total X", this.arrayRotateTranslationX);
            ImGuiHelper.inputInt("Total Y", this.arrayRotateTranslationY);
            ImGuiHelper.inputInt("Total Z", this.arrayRotateTranslationZ);
         }

         if (Selection.getSelectionBuffer().isEmpty()) {
            ImGui.text("⚠ Missing selection");
         } else {
            ImGui.text(action + AxiomI18n.get("axiom.hardcoded.to_revolve_around_point"));
         }
      } else if (mode == 1) {
         ImGui.sliderInt("Count", this.count, 1, 100);
         ImGuiHelper.combo("Type", this.translationType, new String[]{"Relative", "Absolute"});
         ImGuiHelper.tooltip("Relative - Offsets blocks by multiples of the selection size\nAbsolute - Offsets blocks by exact coordinates");
         String text = this.translationType[0] == 0 ? "Factor" : "Offset";
         ImGuiHelper.inputInt(text + " X", this.translationX);
         ImGuiHelper.inputInt(text + " Y", this.translationY);
         ImGuiHelper.inputInt(text + " Z", this.translationZ);
         if (Selection.getSelectionBuffer().isEmpty()) {
            ImGui.text("⚠ Missing selection");
         } else {
            ImGui.text(action + AxiomI18n.get("axiom.hardcoded.to_translate"));
         }
      } else if (mode == 2) {
         ImGui.sliderInt("Count", this.rotationCount, 1, 100);
         ImGuiHelper.combo("Type", this.rotationType, new String[]{"Equidistant", "Custom Angle"});
         if (this.rotationType[0] == 1) {
            ImGuiHelper.inputInt("Degrees X", this.rotationX);
            ImGuiHelper.inputInt("Degrees Y", this.rotationY);
            ImGuiHelper.inputInt("Degrees Z", this.rotationZ);
         }

         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.add_translation"), this.arrayRotateAddTranslation)) {
            this.arrayRotateAddTranslation = !this.arrayRotateAddTranslation;
         }

         if (this.arrayRotateAddTranslation) {
            ImGuiHelper.inputInt("Total X", this.arrayRotateTranslationX);
            ImGuiHelper.inputInt("Total Y", this.arrayRotateTranslationY);
            ImGuiHelper.inputInt("Total Z", this.arrayRotateTranslationZ);
         }

         if (Selection.getSelectionBuffer().isEmpty()) {
            ImGui.text("⚠ Missing selection");
         } else {
            ImGui.text(action + AxiomI18n.get("axiom.hardcoded.to_array_rotate_around_point"));
         }
      } else if (mode == 3) {
         ImGuiHelper.inputInt("Degrees", this.twistDegrees);
         ImGuiHelper.inputFloat("Axis", this.twistAxis);
         if (Selection.getSelectionBuffer().isEmpty()) {
            ImGui.text("⚠ Missing selection");
         } else {
            ImGui.text(action + AxiomI18n.get("axiom.hardcoded.to_twist_selection"));
         }
      }
   }

   @Override
   public String name() {
      return "Modify";
   }

   @Override
   public void writeSettings(CompoundTag tag) {
   }

   @Override
   public void loadSettings(CompoundTag tag) {
   }

   @Override
   public char iconChar() {
      return '\ue91e';
   }

   @Override
   public String keybindId() {
      return "modify";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_MODIFY, AxiomPermission.BUILD_SECTION);
   }
}
