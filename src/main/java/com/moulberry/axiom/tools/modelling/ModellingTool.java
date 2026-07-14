package com.moulberry.axiom.tools.modelling;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.gizmo.ExtrudedGizmo;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.rasterization.HullRasterization;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.rasterization.SmartSurfaceRasterization;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ExpandOffsets;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryBuffer;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.undo.ModellingAdditionalUndoOperation;
import com.moulberry.axiomclientapi.pathers.BallShape;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ModellingTool implements Tool {
   private ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private GizmoList gizmoList = new GizmoList();
   private final List<GizmoList> gridGizmos = new ArrayList<>();
   private boolean recalculate = false;
   private BlockState lastActiveBlock = null;
   private boolean maskWindowOpen = false;
   private Future<ChunkedBlockRegion> pendingCalculation = null;
   private ExtrudedGizmo extrudedGizmo = null;
   private static final int MODE_FLAT_SURFACE = 0;
   private static final int MODE_CATMULL_ROM_SURFACE = 1;
   private static final int MODE_BEZIER_SURFACE = 2;
   private static final int MODE_CONVEX_HULL = 3;
   private static final int MODE_SMART_SURFACE = 4;
   private static final int MODE_TRIANGLE_STRIP = 5;
   private static final int MODE_TRIANGLE_FAN = 6;
   private final int[] mode = new int[]{0};
   private final int[] thickness = new int[]{1};
   private boolean visualizeMesh = false;
   private boolean offsetWhenPlacing = true;
   private boolean keepExisting = false;
   private boolean extendToGround = false;

   public ModellingTool() {
      this.gridGizmos.add(this.gizmoList);
   }

   @Override
   public void reset() {
      this.gizmoList = new GizmoList();
      this.gridGizmos.clear();
      this.gridGizmos.add(this.gizmoList);
      this.chunkedBlockRegion.clear();
      this.recalculate = true;
   }

   public void restore(List<List<BlockPos>> positions, int mode, int thickness, boolean keepExisting, boolean extendToGround) {
      this.reset();
      if (!positions.isEmpty()) {
         this.gridGizmos.clear();

         for (List<BlockPos> line : positions) {
            this.gizmoList.deselectActiveGizmo();
            this.gizmoList = new GizmoList();

            for (BlockPos pos : line) {
               this.gizmoList.addGizmo(Vec3.atCenterOf(pos));
            }

            this.gridGizmos.add(this.gizmoList);
         }

         this.mode[0] = mode;
         this.thickness[0] = thickness;
         this.keepExisting = keepExisting;
         this.extendToGround = extendToGround;
      }
   }

   public void markDirty() {
      this.recalculate = true;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case EXTRUDE:
            if (this.extrudedGizmo == null) {
               this.extrudedGizmo = this.gizmoList.extrude();
               if (this.extrudedGizmo == null) {
                  for (GizmoList linexx : this.gridGizmos) {
                     this.extrudedGizmo = linexx.extrude();
                     if (this.extrudedGizmo != null) {
                        break;
                     }
                  }
               }
            }

            return UserAction.ActionResult.USED_STOP;
         case ENTER:
            if (this.extrudedGizmo != null) {
               this.finishExtrudeGizmo();
               return UserAction.ActionResult.USED_STOP;
            }

            this.pasteShape();
            this.reset();
            return UserAction.ActionResult.USED_STOP;
         case RIGHT_MOUSE:
            if (this.extrudedGizmo != null) {
               this.finishExtrudeGizmo();
               return UserAction.ActionResult.USED_STOP;
            }

            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result != null) {
               BlockPos blockPos = result.blockPos();
               if (this.offsetWhenPlacing) {
                  blockPos = blockPos.relative(result.direction());
               }

               for (GizmoList linex : this.gridGizmos) {
                  if (linex != this.gizmoList) {
                     linex.deselectActiveGizmo();
                  }
               }

               this.gizmoList.addGizmo(Vec3.atCenterOf(blockPos));
               this.recalculate = true;
            }

            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            if (this.extrudedGizmo != null) {
               this.finishExtrudeGizmo();
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.leftClick()) {
               return UserAction.ActionResult.USED_STOP;
            }

            for (GizmoList line : this.gridGizmos) {
               if (line.leftClick()) {
                  this.gizmoList = line;
                  return UserAction.ActionResult.USED_STOP;
               }
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case ESCAPE:
            if (this.extrudedGizmo != null) {
               this.extrudedGizmo = null;
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.hasActiveGizmo()) {
               this.gizmoList.deselectActiveGizmo();
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case DELETE:
            if (this.extrudedGizmo != null) {
               this.extrudedGizmo = null;
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.isEmpty()) {
               this.reset();
            } else {
               this.gizmoList.delete();
            }

            this.recalculate = true;
            return UserAction.ActionResult.USED_STOP;
         case SCROLL:
            UserAction.ScrollAmount scrollAmount = (UserAction.ScrollAmount)object;
            if (this.gizmoList.handleScroll(scrollAmount.scrollX(), scrollAmount.scrollY())) {
               this.recalculate = true;
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case REDO:
            if (this.gizmoList.redo()) {
               this.recalculate = true;
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case UNDO:
            if (this.gizmoList.undo()) {
               this.recalculate = true;
               return UserAction.ActionResult.USED_STOP;
            }
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   private void finishExtrudeGizmo() {
      for (GizmoList line : this.gridGizmos) {
         if (line != this.gizmoList) {
            line.deselectActiveGizmo();
         }
      }

      this.gizmoList.finishExtrude(this.extrudedGizmo, Tool.getLookDirection());
      this.recalculate = true;
      this.extrudedGizmo = null;
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      Vec3 lookDirection = Tool.getLookDirection();
      if (lookDirection != null && this.extrudedGizmo != null) {
         this.extrudedGizmo.render(rc, lookDirection);
      }

      if (this.modeSupportsGridGizmos()) {
         for (GizmoList line : this.gridGizmos) {
            this.recalculate = this.recalculate | line.updateGizmos(rc);
         }
      } else {
         this.recalculate = this.recalculate | this.gizmoList.updateGizmos(rc);
      }

      if (!this.gizmoList.hasGrabbedGizmo() && this.extrudedGizmo == null) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
         if (result != null) {
            BlockPos blockPos = result.blockPos();
            if (this.offsetWhenPlacing) {
               blockPos = blockPos.relative(result.direction());
            }

            Tool.renderRaycastOverlay(rc, blockPos);
         }
      }

      if (Tool.getActiveBlock() != this.lastActiveBlock) {
         this.lastActiveBlock = Tool.getActiveBlock();
         this.recalculate = true;
      }

      boolean maskWindowOpen = EditorWindowType.TOOL_MASKS.isOpen();
      if (this.maskWindowOpen != maskWindowOpen) {
         this.maskWindowOpen = maskWindowOpen;
         this.recalculate = true;
      }

      if (this.recalculate) {
         this.recalculate();
      }

      if (this.pendingCalculation != null && this.pendingCalculation.isDone()) {
         this.chunkedBlockRegion.clear();

         try {
            ChunkedBlockRegion output = this.pendingCalculation.get();
            if (output != null) {
               this.chunkedBlockRegion = output;
            }
         } catch (Exception var6) {
         }

         this.pendingCalculation = null;
      }

      if (!this.chunkedBlockRegion.isEmpty()) {
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         this.renderShapeBounds(rc);
      }

      if (this.modeSupportsGridGizmos()) {
         for (GizmoList gridGizmo : this.gridGizmos) {
            gridGizmo.renderGizmos(rc);
         }
      } else {
         this.gizmoList.renderGizmos(rc);
      }
   }

   private boolean modeSupportsGridGizmos() {
      return this.mode[0] == 2 || this.mode[0] == 1 || this.mode[0] == 0;
   }

   private void renderShapeBounds(AxiomWorldRenderContext rc) {
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(-rc.x(), -rc.y(), -rc.z());
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Pose pose = matrices.last();
      Vec3 previewNewPos = null;
      if (this.gizmoList.hasActiveGizmo() && !Tool.isMouseDown(1) && !Tool.isMouseDown(0) && !EditorUI.isCtrlOrCmdDown()) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
         if (result != null) {
            BlockPos blockPos = result.blockPos();
            if (this.offsetWhenPlacing) {
               blockPos = blockPos.relative(result.direction());
            }

            previewNewPos = Vec3.atCenterOf(blockPos);
         }
      }

      switch (this.mode[0]) {
         case 0:
         case 1:
         case 2:
            List<Gizmo> lastLine = null;

            for (GizmoList gizmoList : this.gridGizmos) {
               if (!gizmoList.isEmpty()) {
                  List<Gizmo> line = gizmoList.getGizmos();

                  for (int ixx = 0; ixx < line.size() - 1; ixx++) {
                     boolean isActive = gizmoList == this.gizmoList;
                     Vec3 from = line.get(ixx).getInterpPosition();
                     Vec3 to = line.get(ixx + 1).getInterpPosition();
                     Shapes.line(bufferBuilder, pose, isActive ? 0.0F : 1.0F, isActive ? 1.0F : 0.0F, 0.0F, from, to);
                     if (isActive && previewNewPos != null && line.get(ixx).enableAxes) {
                        Shapes.line(bufferBuilder, pose, 1.0F, 1.0F, 1.0F, from, previewNewPos);
                        Shapes.line(bufferBuilder, pose, 1.0F, 1.0F, 1.0F, previewNewPos, to);
                     }
                  }

                  if (lastLine != null) {
                     drawConnectingLines(line, lastLine, bufferBuilder, pose);
                  }

                  lastLine = line;
               }
            }
         case 3:
         case 4:
         default:
            break;
         case 5:
            List<Gizmo> gizmosx = this.gizmoList.getGizmos();

            for (int ix = 0; ix < gizmosx.size() - 1; ix++) {
               Vec3 from = gizmosx.get(ix).getInterpPosition();
               Vec3 to = gizmosx.get(ix + 1).getInterpPosition();
               Shapes.line(bufferBuilder, pose, 1.0F, 0.0F, 0.0F, from, to);
               if (ix < gizmosx.size() - 2) {
                  to = gizmosx.get(ix + 2).getInterpPosition();
                  Shapes.line(bufferBuilder, pose, 1.0F, 0.0F, 0.0F, from, to);
               }
            }
            break;
         case 6:
            List<Gizmo> gizmos = this.gizmoList.getGizmos();
            if (!gizmos.isEmpty()) {
               Vec3 start = gizmos.get(0).getInterpPosition();

               for (int i = 1; i < gizmos.size(); i++) {
                  Vec3 curr = gizmos.get(i).getInterpPosition();
                  Shapes.line(bufferBuilder, pose, 1.0F, 0.0F, 0.0F, start, curr);
                  if (i < gizmos.size() - 1) {
                     Vec3 next = gizmos.get(i + 1).getInterpPosition();
                     Shapes.line(bufferBuilder, pose, 1.0F, 0.0F, 0.0F, curr, next);
                  }
               }
            }
      }

      MeshData meshData = bufferBuilder.build();
      if (meshData != null) {
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.5F);
         AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(meshData);
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      }

      matrices.popPose();
   }

   private static void drawConnectingLines(List<Gizmo> one, List<Gizmo> two, BufferBuilder bufferBuilder, Pose pose) {
      if (!one.isEmpty() && !two.isEmpty()) {
         if (one.size() < two.size()) {
            List<Gizmo> temp = one;
            one = two;
            two = temp;
         }

         int skip;
         for (skip = 0; one.size() - skip * 2 >= two.size() + 2; skip++) {
            Vec3 from = one.get(skip).getInterpPosition();
            Vec3 to = two.get(0).getInterpPosition();
            Shapes.line(bufferBuilder, pose, 0.33F, 1.0F, 1.0F, from, to);
            from = one.get(one.size() - 1 - skip).getInterpPosition();
            to = two.get(two.size() - 1).getInterpPosition();
            Shapes.line(bufferBuilder, pose, 0.33F, 1.0F, 1.0F, from, to);
         }

         if (one.size() - skip * 2 == two.size()) {
            for (int i = 0; i < two.size(); i++) {
               Vec3 from = two.get(i).getInterpPosition();
               Vec3 to = one.get(skip + i).getInterpPosition();
               Shapes.line(bufferBuilder, pose, 0.33F, 1.0F, 1.0F, from, to);
            }
         } else {
            for (int i = 0; i < two.size(); i++) {
               Vec3 from = two.get(i).getInterpPosition();
               Vec3 to = one.get(skip + i).getInterpPosition();
               Shapes.line(bufferBuilder, pose, 0.33F, 1.0F, 1.0F, from, to);
               to = one.get(skip + i + 1).getInterpPosition();
               Shapes.line(bufferBuilder, pose, 0.33F, 1.0F, 1.0F, from, to);
            }
         }
      }
   }

   private void pasteShape() {
      List<List<BlockPos>> positions = this.convertGizmosToPositions();
      ChunkedBlockRegion result = calculate(this.mode[0], positions, Tool.getActiveBlock(), this.visualizeMesh, this.thickness[0]);
      if (result != null && !result.isEmpty()) {
         Level level = Minecraft.getInstance().level;
         if (this.extendToGround && level != null) {
            Position2ObjectMap<BlockState> newBlocks = new Position2ObjectMap<>(k -> new BlockState[4096]);
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            result.forEachEntry((x, y, z, block) -> {
               for (int yo = 1; yo < 256 && result.getBlockStateOrAir(x, y - yo, z).isAir(); yo++) {
                  BlockState below = level.getBlockState(mutableBlockPos.set(x, y - yo, z));
                  if (below.getBlock() == Blocks.VOID_AIR || !below.canBeReplaced()) {
                     break;
                  }

                  newBlocks.put(x, y - yo, z, block);
               }
            });
            newBlocks.forEachEntry(result::addBlock);
         }

         String countString = NumberFormat.getInstance().format((long)result.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.placed", countString);
         int modifiers = this.keepExisting ? HistoryEntry.MODIFIER_KEEP_EXISTING : 0;
         ModellingAdditionalUndoOperation additionalUndoOperation = new ModellingAdditionalUndoOperation(
            positions, this.mode[0], this.thickness[0], this.keepExisting, this.extendToGround
         );
         MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
         MaskElement maskElement = MaskManager.getDestMask();
         if (maskElement instanceof ConstantMaskElement constantMaskElement) {
            if (constantMaskElement.getConstant()) {
               RegionHelper.pushBlockRegionChange(result, null, historyDescription, modifiers, additionalUndoOperation);
            }
         } else {
            ChunkedBlockRegion newChunkedBlockRegion = new ChunkedBlockRegion();
            result.forEachEntry((x, y, z, block) -> {
               if (maskElement.test(maskContext.reset(), x, y, z)) {
                  newChunkedBlockRegion.addBlockWithoutDirty(x, y, z, block);
               }
            });
            RegionHelper.pushBlockRegionChange(newChunkedBlockRegion, null, historyDescription, modifiers, additionalUndoOperation);
         }
      }
   }

   private void recalculate() {
      if (this.pendingCalculation == null) {
         this.recalculate = false;
         if (!this.gridGizmos.isEmpty() && (this.gridGizmos.size() != 1 || !this.gizmoList.isEmpty())) {
            List<List<BlockPos>> positions = this.convertGizmosToPositions();
            this.pendingCalculation = Tool.sharedSingleThreadExecutor
               .submit(() -> calculate(this.mode[0], positions, Tool.getActiveBlock(), this.visualizeMesh, this.thickness[0]));
         } else {
            this.chunkedBlockRegion.clear();
         }
      }
   }

   private List<List<BlockPos>> convertGizmosToPositions() {
      List<List<BlockPos>> positions = new ArrayList<>();
      if (this.modeSupportsGridGizmos()) {
         for (GizmoList line : this.gridGizmos) {
            if (!line.isEmpty()) {
               List<BlockPos> linePositions = new ArrayList<>();

               for (Gizmo gizmo : line.getGizmos()) {
                  linePositions.add(gizmo.getTargetPosition());
               }

               positions.add(linePositions);
            }
         }
      } else {
         List<BlockPos> linePositions = new ArrayList<>();

         for (Gizmo gizmo : this.gizmoList.getGizmos()) {
            linePositions.add(gizmo.getTargetPosition());
         }

         positions.add(linePositions);
      }

      return positions;
   }

   private static ChunkedBlockRegion calculate(int mode, List<List<BlockPos>> positions, BlockState block, boolean visualizeMesh, int thickness) {
      if (positions.isEmpty()) {
         return null;
      } else {
         List<BlockPos> singlePositions = positions.get(0);
         ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
         switch (mode) {
            case 0:
               if (thickness <= 1) {
                  GridSurface.calculateFlat(positions, (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block));
               } else {
                  PositionSet positionSet = new PositionSet();
                  GridSurface.calculateFlat(positions, positionSet::add);
                  int[][] allOffsets = ExpandOffsets.create(BallShape.SPHERE, thickness - 1);
                  positionSet.forEach((x, y, z) -> {
                     int offsetIndex = 63;
                     if (positionSet.contains(x - 1, y, z)) {
                        offsetIndex--;
                     }

                     if (positionSet.contains(x, y - 1, z)) {
                        offsetIndex -= 2;
                     }

                     if (positionSet.contains(x, y, z - 1)) {
                        offsetIndex -= 4;
                     }

                     if (positionSet.contains(x + 1, y, z)) {
                        offsetIndex -= 8;
                     }

                     if (positionSet.contains(x, y + 1, z)) {
                        offsetIndex -= 16;
                     }

                     if (positionSet.contains(x, y, z + 1)) {
                        offsetIndex -= 32;
                     }

                     chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block);
                     int[] offsets = allOffsets[offsetIndex];

                     for (int ix = 0; ix < offsets.length; ix += 3) {
                        chunkedBlockRegion.addBlockWithoutDirty(x + offsets[ix], y + offsets[ix + 1], z + offsets[ix + 2], block);
                     }
                  });
               }
               break;
            case 1:
               if (thickness <= 1) {
                  GridSurface.calculateCatmullRom(positions, (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block));
               } else {
                  PositionSet positionSet = new PositionSet();
                  GridSurface.calculateCatmullRom(positions, positionSet::add);
                  int[][] allOffsets = ExpandOffsets.create(BallShape.SPHERE, thickness - 1);
                  positionSet.forEach((x, y, z) -> {
                     int offsetIndex = 63;
                     if (positionSet.contains(x - 1, y, z)) {
                        offsetIndex--;
                     }

                     if (positionSet.contains(x, y - 1, z)) {
                        offsetIndex -= 2;
                     }

                     if (positionSet.contains(x, y, z - 1)) {
                        offsetIndex -= 4;
                     }

                     if (positionSet.contains(x + 1, y, z)) {
                        offsetIndex -= 8;
                     }

                     if (positionSet.contains(x, y + 1, z)) {
                        offsetIndex -= 16;
                     }

                     if (positionSet.contains(x, y, z + 1)) {
                        offsetIndex -= 32;
                     }

                     chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block);
                     int[] offsets = allOffsets[offsetIndex];

                     for (int ix = 0; ix < offsets.length; ix += 3) {
                        chunkedBlockRegion.addBlockWithoutDirty(x + offsets[ix], y + offsets[ix + 1], z + offsets[ix + 2], block);
                     }
                  });
               }
               break;
            case 2:
               if (thickness <= 1) {
                  GridSurface.calculateBezier(positions, (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block));
               } else {
                  PositionSet positionSet = new PositionSet();
                  GridSurface.calculateBezier(positions, positionSet::add);
                  int[][] allOffsets = ExpandOffsets.create(BallShape.SPHERE, thickness - 1);
                  positionSet.forEach((x, y, z) -> {
                     int offsetIndex = 63;
                     if (positionSet.contains(x - 1, y, z)) {
                        offsetIndex--;
                     }

                     if (positionSet.contains(x, y - 1, z)) {
                        offsetIndex -= 2;
                     }

                     if (positionSet.contains(x, y, z - 1)) {
                        offsetIndex -= 4;
                     }

                     if (positionSet.contains(x + 1, y, z)) {
                        offsetIndex -= 8;
                     }

                     if (positionSet.contains(x, y + 1, z)) {
                        offsetIndex -= 16;
                     }

                     if (positionSet.contains(x, y, z + 1)) {
                        offsetIndex -= 32;
                     }

                     chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block);
                     int[] offsets = allOffsets[offsetIndex];

                     for (int ix = 0; ix < offsets.length; ix += 3) {
                        chunkedBlockRegion.addBlockWithoutDirty(x + offsets[ix], y + offsets[ix + 1], z + offsets[ix + 2], block);
                     }
                  });
               }
               break;
            case 3:
               if (singlePositions.isEmpty()) {
                  return null;
               }

               try {
                  double[] points = new double[singlePositions.size() * 3];

                  for (int i = 0; i < singlePositions.size(); i++) {
                     BlockPos pos = singlePositions.get(i);
                     points[i * 3] = pos.getX();
                     points[i * 3 + 1] = pos.getY();
                     points[i * 3 + 2] = pos.getZ();
                  }

                  HullRasterization.quickHullBlockRegion(chunkedBlockRegion, block, points);
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
               break;
            case 4:
               if (singlePositions.isEmpty()) {
                  return null;
               }

               BlockPos[] points = new BlockPos[singlePositions.size()];

               for (int i = 0; i < singlePositions.size(); i++) {
                  points[i] = singlePositions.get(i);
               }

               SmartSurfaceRasterization.smartSurface(chunkedBlockRegion, block, points, visualizeMesh);
               break;
            case 5:
               if (singlePositions.isEmpty()) {
                  return null;
               }

               for (int i = 0; i < singlePositions.size() - 2; i++) {
                  Rasterization3D.triangle(
                     singlePositions.get(i),
                     singlePositions.get(i + 1),
                     singlePositions.get(i + 2),
                     (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block)
                  );
               }
               break;
            case 6:
               if (singlePositions.isEmpty()) {
                  return null;
               }

               BlockPos start = singlePositions.get(0);

               for (int i = 1; i < singlePositions.size() - 1; i++) {
                  Rasterization3D.triangle(
                     start, singlePositions.get(i), singlePositions.get(i + 1), (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(x, y, z, block)
                  );
               }
         }

         chunkedBlockRegion.dirtyAll();
         chunkedBlockRegion.uniqueBlockState = block;
         return chunkedBlockRegion;
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.modelling"));
      boolean changed = ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.shape"),
         this.mode,
         new String[]{
            AxiomI18n.get("axiom.tool.shape.flat_surface"),
            AxiomI18n.get("axiom.tool.shape.catmull_rom_surface"),
            AxiomI18n.get("axiom.tool.shape.bezier_surface"),
            AxiomI18n.get("axiom.tool.shape.convex_hull"),
            AxiomI18n.get("axiom.tool.shape.smart_surface"),
            AxiomI18n.get("axiom.tool.shape.triangle_strip"),
            AxiomI18n.get("axiom.tool.shape.triangle_fan")
         }
      );
      switch (this.mode[0]) {
         case 0:
         case 1:
         case 2:
            changed |= ImGui.sliderInt("Thickness", this.thickness, 1, 16);
            ImGuiHelper.separatorWithText("Rows");
            int removeRow = -1;
            boolean canRemove = this.gridGizmos.size() > 1;

            for (int i = 0; i < this.gridGizmos.size(); i++) {
               boolean selected = this.gridGizmos.get(i) == this.gizmoList;
               ImGui.pushID(i);
               if (selected) {
                  ImGui.beginDisabled();
               }

               if (ImGui.smallButton(AxiomI18n.get("axiom.hardcoded.row_hash") + (i + 1))) {
                  this.gizmoList = this.gridGizmos.get(i);
               }

               if (selected) {
                  ImGui.endDisabled();
               }

               if (canRemove) {
                  ImGui.sameLine();
                  if (ImGui.smallButton("X")) {
                     removeRow = i;
                  }
               }

               ImGui.popID();
            }

            if (canRemove && removeRow >= 0) {
               GizmoList removed = this.gridGizmos.remove(removeRow);
               if (removed == this.gizmoList) {
                  this.gizmoList = this.gridGizmos.get(this.gridGizmos.size() - 1);
               }

               this.recalculate = true;
            }

            if (ImGui.smallButton(AxiomI18n.get("axiom.hardcoded.add_row"))) {
               this.gizmoList = new GizmoList();
               this.gridGizmos.add(this.gizmoList);
            }
         case 3:
         default:
            break;
         case 4:
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.smart_surface"));
            if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.smart_surface.visualize_mesh"), this.visualizeMesh)) {
               this.visualizeMesh = !this.visualizeMesh;
               changed = true;
            }
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.placement_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.offset_when_placing"), this.offsetWhenPlacing)) {
         this.offsetWhenPlacing = !this.offsetWhenPlacing;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.paste_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.keep_existing"), this.keepExisting)) {
         this.keepExisting = !this.keepExisting;
         changed = true;
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.extend_to_ground"), this.extendToGround)) {
         this.extendToGround = !this.extendToGround;
         changed = true;
      }

      if (this.gizmoList.hasActiveGizmo()) {
         ImGui.separator();
         ImGui.text(AxiomI18n.get("axiom.tool.shape.extrude_tip", Keybinds.EXTRUDE_POINT.longKeyIdentifier()));
      }

      if (ImGui.button(AxiomI18n.get("axiom.hardcoded.paste_copy"))) {
         this.pasteShape();
      }

      if (changed) {
         this.recalculate = true;
      }
   }

   @Override
   public String listenForEsc() {
      if (this.gizmoList.hasActiveGizmo()) {
         return "Deselect Point";
      } else {
         return !this.gizmoList.isEmpty() ? AxiomI18n.get("axiom.widget.cancel") : null;
      }
   }

   @Override
   public String listenForEnter() {
      return !this.chunkedBlockRegion.isEmpty() ? AxiomI18n.get("axiom.widget.confirm") : null;
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.modelling");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.recalculate = true;
   }

   @Override
   public char iconChar() {
      return '\ue928';
   }

   @Override
   public String keybindId() {
      return "modelling";
   }

   @Override
   public HistoryBuffer<?> historyBuffer() {
      return this.gizmoList.getHistoryBuffer();
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_MODELLING, AxiomPermission.BUILD_SECTION);
   }
}
