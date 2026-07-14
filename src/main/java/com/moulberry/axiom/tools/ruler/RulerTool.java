package com.moulberry.axiom.tools.ruler;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.modelling.GizmoList;
import com.moulberry.axiom.utils.ProjectedText;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4fc;

public class RulerTool implements Tool {
   private static final int[] shapeMode = new int[]{0};
   private static final int[] displayMode = new int[]{0};
   private static int lastGizmoList = -1;
   private static final List<GizmoList> lines = new ArrayList<>();
   private static final List<RulerCircle> circles = new ArrayList<>();
   private BlockPos circleOrigin = null;
   private Axis circleAxis = null;
   private static final int[] splitSegments = new int[]{2};
   private BlockPos minimum = null;
   private BlockPos maximum = null;
   private float totalLengthEuclidean = 0.0F;
   private int totalLengthManhattan = 0;
   private static int RULER_SHAPE_LINE = 0;
   private static int RULER_SHAPE_CIRCLE = 1;

   @Override
   public void reset() {
      this.circleOrigin = null;
      this.circleAxis = null;
      this.recalculate();
   }

   private void clear() {
      lines.clear();
      circles.clear();
      this.reset();
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result != null) {
               if (shapeMode[0] == RULER_SHAPE_LINE) {
                  for (GizmoList linexxxx : lines) {
                     if (linexxxx.hasActiveGizmo()) {
                        for (GizmoList otherx : lines) {
                           if (linexxxx != otherx) {
                              otherx.deselectActiveGizmo();
                           }
                        }

                        linexxxx.addGizmo(Vec3.atCenterOf(result.blockPos()));
                        this.recalculate();
                        return UserAction.ActionResult.USED_STOP;
                     }
                  }

                  GizmoList gizmoList = new GizmoList();
                  gizmoList.addGizmo(Vec3.atCenterOf(result.blockPos()));
                  lines.add(gizmoList);
                  this.recalculate();
               } else if (shapeMode[0] == RULER_SHAPE_CIRCLE) {
                  if (this.circleOrigin == null) {
                     this.circleOrigin = result.blockPos();
                     this.circleAxis = result.direction().getAxis();
                  } else {
                     for (RulerCircle circle : circles) {
                        circle.center().enableAxes = false;
                     }
                     BlockPos targetPos = switch (this.circleAxis) {
                        case X -> new BlockPos(this.circleOrigin.getX(), result.blockPos().getY(), result.blockPos().getZ());
                        case Y -> new BlockPos(result.blockPos().getX(), this.circleOrigin.getY(), result.blockPos().getZ());
                        case Z -> new BlockPos(result.blockPos().getX(), result.blockPos().getY(), this.circleOrigin.getZ());
                        default -> throw new IncompatibleClassChangeError();
                     };
                     if (!this.circleOrigin.equals(targetPos)) {
                        circles.add(new RulerCircle(this.circleOrigin, targetPos, this.circleAxis));
                     }

                     this.circleOrigin = null;
                     this.circleAxis = null;
                  }
               }
            }

            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            if (shapeMode[0] == RULER_SHAPE_LINE) {
               for (GizmoList linexx : lines) {
                  if (linexx.hasActiveGizmo()) {
                     if (linexx.leftClick()) {
                        return UserAction.ActionResult.USED_STOP;
                     }
                     break;
                  }
               }

               for (GizmoList linexxx : lines) {
                  if (linexxx.leftClick()) {
                     return UserAction.ActionResult.USED_STOP;
                  }
               }
            } else if (shapeMode[0] == RULER_SHAPE_CIRCLE) {
               for (RulerCircle circle : circles) {
                  Gizmo gizmo = circle.center();
                  if (gizmo.enableAxes && gizmo.leftClick()) {
                     return UserAction.ActionResult.USED_STOP;
                  }

                  gizmo.enableAxes = false;
               }

               for (RulerCircle circle : circles) {
                  Gizmo gizmo = circle.center();
                  if (gizmo.leftClick()) {
                     for (RulerCircle other : circles) {
                        if (circle != other) {
                           other.center().enableAxes = false;
                        }
                     }

                     return UserAction.ActionResult.USED_STOP;
                  }

                  gizmo.enableAxes = false;
               }
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case ESCAPE:
         case ENTER:
            boolean deselected = false;

            for (GizmoList linexxxxx : lines) {
               deselected |= linexxxxx.hasActiveGizmo();
               linexxxxx.deselectActiveGizmo();
            }

            for (RulerCircle circle : circles) {
               deselected |= circle.center().enableAxes;
               circle.center().enableAxes = false;
            }

            if (deselected) {
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case DELETE:
            if (shapeMode[0] == RULER_SHAPE_LINE) {
               for (GizmoList linexxxxx : lines) {
                  if (linexxxxx.hasActiveGizmo()) {
                     linexxxxx.delete();
                     this.recalculate();
                     return UserAction.ActionResult.USED_STOP;
                  }
               }
            } else if (shapeMode[0] == RULER_SHAPE_CIRCLE && circles.removeIf(circlex -> circlex.center().enableAxes)) {
               return UserAction.ActionResult.USED_STOP;
            }

            this.clear();
            break;
         case REDO:
            if (shapeMode[0] == RULER_SHAPE_LINE) {
               if (lastGizmoList >= 0 && lastGizmoList < lines.size()) {
                  GizmoList linex = lines.get(lastGizmoList);
                  if (linex.redo()) {
                     this.recalculate();
                     return UserAction.ActionResult.USED_STOP;
                  }
               }

               for (GizmoList linex : lines) {
                  if (linex.redo()) {
                     this.recalculate();
                     return UserAction.ActionResult.USED_STOP;
                  }
               }
            }

            return UserAction.ActionResult.USED_STOP;
         case UNDO:
            if (shapeMode[0] == RULER_SHAPE_LINE) {
               if (lastGizmoList >= 0 && lastGizmoList < lines.size()) {
                  GizmoList line = lines.get(lastGizmoList);
                  if (line.undo()) {
                     this.recalculate();
                     return UserAction.ActionResult.USED_STOP;
                  }
               }

               for (GizmoList line : lines) {
                  if (line.undo()) {
                     this.recalculate();
                     return UserAction.ActionResult.USED_STOP;
                  }
               }
            } else if (shapeMode[0] == RULER_SHAPE_CIRCLE && circles.removeIf(circlex -> circlex.center().enableAxes)) {
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.USED_STOP;
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      Selection.render(rc, 4);
      boolean showRaycastOverlay = true;
      if (shapeMode[0] == RULER_SHAPE_LINE) {
         for (GizmoList line : lines) {
            if (line.hasGrabbedGizmo()) {
               showRaycastOverlay = false;
               break;
            }
         }
      }

      for (int i = 0; i < lines.size(); i++) {
         GizmoList linex = lines.get(i);
         if (linex.isEmpty() && lastGizmoList != i) {
            lines.remove(i);
            break;
         }
      }

      if (shapeMode[0] == RULER_SHAPE_LINE) {
         for (int ix = 0; ix < lines.size(); ix++) {
            GizmoList linex = lines.get(ix);
            if (linex.updateGizmos(rc)) {
               this.recalculate();
            }

            linex.renderGizmos(rc);
            if (linex.hasActiveGizmo()) {
               lastGizmoList = ix;
               Gizmo last = linex.getGizmos().get(linex.getGizmos().size() - 1);
               if (last.enableAxes && !last.isGrabbed()) {
                  RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
                  if (result != null) {
                     BlockPos target = result.blockPos();
                     PoseStack matrices = rc.poseStack();
                     matrices.pushPose();
                     matrices.translate(target.getX() - rc.x(), target.getY() - rc.y(), target.getZ() - rc.z());
                     AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
                     VertexConsumerProvider provider = VertexConsumerProvider.shared();
                     BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                     Pose pose = matrices.last();
                     BlockPos offsetTarget = last.getTargetPosition().subtract(target);
                     Vec3 fromVec = Vec3.atCenterOf(offsetTarget);
                     Vec3 toVec = new Vec3(0.5, 0.5, 0.5);
                     Shapes.line(bufferBuilder, pose, 1.0F, 1.0F, 1.0F, fromVec, toVec);
                     MeshData meshData = provider.build();
                     AxiomRenderer.setShaderColour(0.961F, 0.796F, 0.259F, 1.0F);
                     AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
                     AxiomRenderer.setShaderColour(0.961F, 0.796F, 0.259F, 0.35F);
                     AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
                     AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
                     boolean displayDistance = displayMode[0] == 1;
                     ProjectedText.setupProjectedText();
                     drawDistanceTextForLine(matrices, rc.projection(), displayDistance, offsetTarget, BlockPos.ZERO, Vec3.ZERO);
                     ProjectedText.finishProjectedText();
                     matrices.popPose();
                  }
               }
            }
         }
      } else if (shapeMode[0] == RULER_SHAPE_CIRCLE) {
         if (!circles.isEmpty()) {
            Vec3 lookDirection = Tool.getLookDirection();
            boolean isLeftDown = Tool.isMouseDown(0);
            boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
            boolean showGizmo = !EditorUI.isActive() || !isCtrlDown;
            boolean renderGizmos = showGizmo;
            boolean positionChanged = false;

            for (RulerCircle circle : circles) {
               Gizmo gizmo = circle.center();
               Vec3 before = gizmo.getTargetVec();
               gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
               gizmo.setAxisDirections(
                  rc.x() > gizmo.getTargetPosition().getX(), rc.y() > gizmo.getTargetPosition().getY(), rc.z() > gizmo.getTargetPosition().getZ()
               );
               if (gizmo.isGrabbed()) {
                  renderGizmos = true;
                  showRaycastOverlay = false;
               }

               Vec3 after = gizmo.getTargetVec();
               if (!before.equals(after)) {
                  positionChanged = true;
               }
            }

            if (renderGizmos) {
               for (RulerCircle circle : circles) {
                  circle.center().render(rc, isCtrlDown);
               }
            }

            if (positionChanged) {
               this.recalculate();
            }
         }

         if (this.circleOrigin != null) {
            showRaycastOverlay = false;
            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result != null) {
               BlockPos targetPos = switch (this.circleAxis) {
                  case X -> new BlockPos(this.circleOrigin.getX(), result.blockPos().getY(), result.blockPos().getZ());
                  case Y -> new BlockPos(result.blockPos().getX(), this.circleOrigin.getY(), result.blockPos().getZ());
                  case Z -> new BlockPos(result.blockPos().getX(), result.blockPos().getY(), this.circleOrigin.getZ());
                  default -> throw new IncompatibleClassChangeError();
               };
               RulerCircle rulerCircle = new RulerCircle(this.circleOrigin, targetPos, this.circleAxis);
               Vec3 origin = result.getLocation();
               PoseStack matrices = rc.poseStack();
               matrices.pushPose();
               matrices.translate(origin.x - rc.x(), origin.y - rc.y(), origin.z - rc.z());
               VertexConsumerProvider provider = VertexConsumerProvider.shared();
               BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               Pose pose = matrices.last();
               rulerCircle.drawLineFromCenterToEdge(bufferBuilder, pose, origin);
               MeshData meshData = provider.build();
               AxiomRenderer.setShaderColour(0.961F, 0.796F, 0.259F, 1.0F);
               AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
               AxiomRenderer.setShaderColour(0.961F, 0.796F, 0.259F, 0.35F);
               AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
               AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
               boolean displayDistance = displayMode[0] == 1;
               ProjectedText.setupProjectedText();
               drawDistanceTextForCircle(matrices, rc.projection(), rulerCircle, displayDistance, origin);
               ProjectedText.finishProjectedText();
               bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               rulerCircle.drawCircle(rc, Minecraft.getInstance().level, bufferBuilder, -668862, origin);
               meshData = provider.build();
               AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, true);
               matrices.popPose();
            }
         }
      }

      if (showRaycastOverlay) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
         Tool.renderRaycastOverlay(rc, result);
      }
   }

   public static void renderPoints(AxiomWorldRenderContext rc) {
      if (!lines.isEmpty() || !circles.isEmpty()) {
         Vec3 origin = rc.position();
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         buildLines(bufferBuilder, rc.poseStack(), origin);
         MeshData meshData = provider.build();
         AxiomRenderer.setShaderColour(1.0F, 0.1F, 0.1F, 1.0F);
         AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
         AxiomRenderer.setShaderColour(1.0F, 0.1F, 0.1F, 0.35F);
         AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         drawDistanceText(rc.poseStack(), rc.projection(), origin);
         drawBlockOutlines(rc, origin);
      }
   }

   private static void drawDistanceText(PoseStack matrix, Matrix4fc projection, Vec3 origin) {
      ProjectedText.setupProjectedText();
      boolean displayDistance = displayMode[0] == 1;

      for (GizmoList line : lines) {
         drawDistanceTextForLines(matrix, projection, line, displayDistance, origin);
      }

      for (RulerCircle circle : circles) {
         drawDistanceTextForCircle(matrix, projection, circle, displayDistance, origin);
      }

      ProjectedText.finishProjectedText();
   }

   private static void drawDistanceTextForLines(PoseStack matrix, Matrix4fc projection, GizmoList line, boolean displayDistance, Vec3 origin) {
      List<Gizmo> gizmos = line.getGizmos();

      for (int i = 0; i < gizmos.size() - 1; i++) {
         BlockPos from = gizmos.get(i).getTargetPosition();
         BlockPos to = gizmos.get(i + 1).getTargetPosition();
         drawDistanceTextForLine(matrix, projection, displayDistance, to, from, origin);
      }
   }

   private static void drawDistanceTextForLine(PoseStack matrix, Matrix4fc projection, boolean displayDistance, BlockPos to, BlockPos from, Vec3 origin) {
      float dx = to.getX() - from.getX();
      float dy = to.getY() - from.getY();
      float dz = to.getZ() - from.getZ();
      if (displayDistance) {
         float distance = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (distance > 1.0F) {
            ProjectedText.renderProjectedText(
               String.format("+%.1f", distance),
               matrix,
               projection,
               (float)(from.getX() - origin.x) + dx * 0.5F + 0.5F,
               (float)(from.getY() - origin.y) + dy * 0.5F + 0.5F,
               (float)(from.getZ() - origin.z) + dz * 0.5F + 0.5F
            );
         }
      } else {
         int bx = Math.abs(to.getX() - from.getX()) + 1;
         int by = Math.abs(to.getY() - from.getY()) + 1;
         int bz = Math.abs(to.getZ() - from.getZ()) + 1;
         int distance = Math.max(bx, Math.max(by, bz));
         if (distance > 1) {
            ProjectedText.renderProjectedText(
               String.format("%d", distance),
               matrix,
               projection,
               (float)(from.getX() - origin.x) + dx * 0.5F + 0.5F,
               (float)(from.getY() - origin.y) + dy * 0.5F + 0.5F,
               (float)(from.getZ() - origin.z) + dz * 0.5F + 0.5F
            );
         }
      }
   }

   private static void drawDistanceTextForCircle(PoseStack matrix, Matrix4fc projection, RulerCircle circle, boolean displayDistance, Vec3 origin) {
      BlockPos from = circle.center().getTargetPosition();
      BlockPos to = from.offset(circle.offset());
      float dx = to.getX() - from.getX();
      float dy = to.getY() - from.getY();
      float dz = to.getZ() - from.getZ();
      if (displayDistance) {
         float distance = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (distance > 1.0F) {
            ProjectedText.renderProjectedText(
               String.format("r=%.1f", distance),
               matrix,
               projection,
               (float)(from.getX() - origin.x) + dx * 0.5F + 0.5F,
               (float)(from.getY() - origin.y) + dy * 0.5F + 0.5F,
               (float)(from.getZ() - origin.z) + dz * 0.5F + 0.5F
            );
         }

         int radius = circle.radius();
         float circumference = (float)(radius * 2 * Math.PI);
         ProjectedText.renderProjectedText(
            String.format("c=%.1f", circumference),
            matrix,
            projection,
            (float)(to.getX() - origin.x),
            (float)(to.getY() - origin.y),
            (float)(to.getZ() - origin.z)
         );
      } else {
         int bx = Math.abs(to.getX() - from.getX()) + 1;
         int by = Math.abs(to.getY() - from.getY()) + 1;
         int bz = Math.abs(to.getZ() - from.getZ()) + 1;
         int distance = Math.max(bx, Math.max(by, bz));
         if (distance > 1) {
            ProjectedText.renderProjectedText(
               String.format("r=%d", distance),
               matrix,
               projection,
               (float)(from.getX() - origin.x) + dx * 0.5F + 0.5F,
               (float)(from.getY() - origin.y) + dy * 0.5F + 0.5F,
               (float)(from.getZ() - origin.z) + dz * 0.5F + 0.5F
            );
         }

         int blockCountCircumference = circle.blockCount();
         ProjectedText.renderProjectedText(
            String.format("c=%d", blockCountCircumference),
            matrix,
            projection,
            (float)(to.getX() - origin.x),
            (float)(to.getY() - origin.y),
            (float)(to.getZ() - origin.z)
         );
      }
   }

   private static void drawBlockOutlines(AxiomWorldRenderContext rc, Vec3 origin) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

         for (GizmoList line : lines) {
            for (Gizmo gizmo : line.getGizmos()) {
               BlockPos point = gizmo.getTargetPosition();
               BlockState blockState = level.getBlockState(point);
               VoxelShape voxelShape = blockState.getShape(level, point, CollisionContext.empty());
               PoseStack matrices = rc.poseStack();
               matrices.pushPose();
               matrices.translate(point.getX() - origin.x, point.getY() - origin.y, point.getZ() - origin.z);
               Shapes.blockOutline(bufferBuilder, matrices.last(), voxelShape, -16399395);
               matrices.popPose();
            }
         }

         for (RulerCircle circle : circles) {
            circle.drawCircle(rc, level, bufferBuilder, -16399395, origin);
         }

         AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(bufferBuilder.build());
      }
   }

   private static void buildLines(BufferBuilder bufferBuilder, PoseStack matrices, Vec3 origin) {
      Pose pose = matrices.last();

      for (GizmoList line : lines) {
         List<Gizmo> gizmos = line.getGizmos();

         for (int i = 0; i < gizmos.size() - 1; i++) {
            Vec3 from = gizmos.get(i).getTargetVec();
            Vec3 to = gizmos.get(i + 1).getTargetVec();
            Shapes.line(bufferBuilder, pose, 1.0F, 1.0F, 1.0F, from.subtract(origin), to.subtract(origin));
         }
      }

      for (RulerCircle circle : circles) {
         circle.drawLineFromCenterToEdge(bufferBuilder, pose, origin);
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.ruler"));
      ImGuiHelper.combo("Mode", shapeMode, new String[]{"Lines", "Circle"});
      ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.ruler.display"),
         displayMode,
         new String[]{AxiomI18n.get("axiom.tool.ruler.display.block_count"), AxiomI18n.get("axiom.tool.ruler.display.distance")}
      );
      ImGuiHelper.separatorWithText("Actions");
      boolean canSelectedCenter = this.minimum != null && this.maximum != null;
      if (!canSelectedCenter) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.ruler.select_center")) && canSelectedCenter) {
         Selection.clearSelection();
         int minX = Math.floorDiv(this.minimum.getX() + this.maximum.getX(), 2);
         int minY = Math.floorDiv(this.minimum.getY() + this.maximum.getY(), 2);
         int minZ = Math.floorDiv(this.minimum.getZ() + this.maximum.getZ(), 2);
         int maxX = (this.maximum.getX() - this.minimum.getX() & 1) == 0 ? minX : minX + 1;
         int maxY = (this.maximum.getY() - this.minimum.getY() & 1) == 0 ? minY : minY + 1;
         int maxZ = (this.maximum.getZ() - this.minimum.getZ() & 1) == 0 ? minZ : minZ + 1;
         Selection.addAABBWithHistory(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), true);
         ToolManager.setToolSelected(false);
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.ruler.split")) && canSelectedCenter) {
         ImGui.openPopup("##SplitPopup");
      }

      if (!canSelectedCenter) {
         ImGui.endDisabled();
      }

      if (ImGuiHelper.beginPopup("##SplitPopup")) {
         if (splitSegments[0] < 2) {
            splitSegments[0] = 2;
         }

         ImGui.setNextItemWidth(100.0F);
         ImGui.sliderInt("Segments", splitSegments, 2, 16);
         boolean canSplit = false;

         for (GizmoList line : lines) {
            int index = line.getActiveGizmoIndex();
            if (index > 0) {
               canSplit = true;
               break;
            }
         }

         if (!canSplit) {
            ImGui.beginDisabled();
         }

         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.split")) && canSplit) {
            for (GizmoList linex : lines) {
               int index = linex.getActiveGizmoIndex();
               if (index > 0) {
                  Gizmo previousGizmo = linex.getGizmos().get(index - 1);
                  Gizmo selectedGizmo = linex.getGizmos().get(index);
                  Vec3 from = previousGizmo.getTargetVec();
                  Vec3 to = selectedGizmo.getTargetVec();
                  Vec3 delta = to.subtract(from);
                  linex.setActiveGizmo(index - 1);
                  int segments = Math.max(2, splitSegments[0]);

                  for (int i = 1; i < segments; i++) {
                     float partial = (float)i / segments;
                     Vec3 mid = from.add(delta.scale(partial));
                     linex.addGizmo(mid);
                  }
               }
            }
         }

         if (!canSplit) {
            ImGui.endDisabled();
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
            ImGui.closeCurrentPopup();
         }

         ImGui.endPopup();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.ruler.information"));
      NumberFormat format = NumberFormat.getInstance();
      if (this.minimum != null && this.maximum != null) {
         ImGui.text(AxiomI18n.get("axiom.tool.ruler.total_length_euclidean", String.format("%.2f", this.totalLengthEuclidean)));
         ImGui.text(AxiomI18n.get("axiom.tool.ruler.total_length_manhattan", format.format((long)this.totalLengthManhattan)));
         ImGui.text(
            AxiomI18n.get(
               "axiom.tool.ruler.minimum",
               format.format((long)this.minimum.getX()),
               format.format((long)this.minimum.getY()),
               format.format((long)this.minimum.getZ())
            )
         );
         ImGui.text(
            AxiomI18n.get(
               "axiom.tool.ruler.maximum",
               format.format((long)this.maximum.getX()),
               format.format((long)this.maximum.getY()),
               format.format((long)this.maximum.getZ())
            )
         );
         int boundingX = this.maximum.getX() - this.minimum.getX() + 1;
         int boundingY = this.maximum.getY() - this.minimum.getY() + 1;
         int boundingZ = this.maximum.getZ() - this.minimum.getZ() + 1;
         ImGui.text(
            AxiomI18n.get("axiom.tool.ruler.bounding_size", format.format((long)boundingX), format.format((long)boundingY), format.format((long)boundingZ))
         );
      } else {
         ImGui.beginDisabled();
         ImGui.text(AxiomI18n.get("axiom.tool.ruler.no_information"));
         ImGui.endDisabled();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.ruler.points"));
      if (ImGui.button(AxiomI18n.get("axiom.hardcoded.clear_delete"))) {
         this.clear();
      }
   }

   private void recalculate() {
      this.totalLengthEuclidean = 0.0F;
      this.totalLengthManhattan = 0;
      this.minimum = null;
      this.maximum = null;

      for (GizmoList line : lines) {
         List<Gizmo> gizmos = line.getGizmos();
         if (!gizmos.isEmpty()) {
            for (int i = 0; i < gizmos.size() - 1; i++) {
               BlockPos one = gizmos.get(i).getTargetPosition();
               BlockPos two = gizmos.get(i + 1).getTargetPosition();
               this.totalLengthEuclidean = (float)(this.totalLengthEuclidean + Math.sqrt(one.distSqr(two)));
               this.totalLengthManhattan = this.totalLengthManhattan + one.distManhattan(two);
            }

            if (this.minimum == null) {
               this.minimum = gizmos.get(0).getTargetPosition();
            }

            if (this.maximum == null) {
               this.maximum = gizmos.get(0).getTargetPosition();
            }

            for (Gizmo gizmo : gizmos) {
               BlockPos point = gizmo.getTargetPosition();
               this.minimum = new BlockPos(
                  Math.min(this.minimum.getX(), point.getX()), Math.min(this.minimum.getY(), point.getY()), Math.min(this.minimum.getZ(), point.getZ())
               );
               this.maximum = new BlockPos(
                  Math.max(this.maximum.getX(), point.getX()), Math.max(this.maximum.getY(), point.getY()), Math.max(this.maximum.getZ(), point.getZ())
               );
            }
         }
      }
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.ruler");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putInt("DisplayMode", displayMode[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      displayMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "DisplayMode", 0);
   }

   @Override
   public char iconChar() {
      return '\ue909';
   }

   @Override
   public String keybindId() {
      return "ruler";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_RULER);
   }
}
