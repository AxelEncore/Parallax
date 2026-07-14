package com.moulberry.axiom.tools.box_select;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.phys.Vec3;

public class BoxSelectTool implements Tool {
   private BlockPos dragOrigin = null;
   private boolean dragging = false;
   private Gizmo firstGizmo = null;
   private Gizmo secondGizmo = null;
   private Gizmo centerGizmo = null;
   private final int[] mode = new int[]{1};
   private static final int MODE_REPLACE = 0;
   private static final int MODE_ADD = 1;
   private static final int MODE_SUBTRACT = 2;
   private static final int MODE_INTERSECT = 3;
   private final int[] inputMode = new int[]{0};
   private final int[] position = new int[3];
   private final int[] size = new int[3];

   public boolean handleScroll(int xScroll, int yScroll) {
      if (this.firstGizmo == null || this.secondGizmo == null || this.centerGizmo == null) {
         return false;
      } else if (EditorUI.isCtrlOrCmdDown() && !this.firstGizmo.isGrabbed() && !this.secondGizmo.isGrabbed() && !this.centerGizmo.isGrabbed()) {
         return false;
      } else {
         BlockPos oldCenterPos = this.centerGizmo.getTargetPosition();
         Vec3 lookDirection = Tool.getLookDirection();
         if (this.centerGizmo.enableAxes && lookDirection != null) {
            this.centerGizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), lookDirection);
         }

         BlockPos oldFirstPos = this.firstGizmo.getTargetPosition();
         BlockPos oldSecondPos = this.secondGizmo.getTargetPosition();
         if (!this.centerGizmo.getTargetPosition().equals(oldCenterPos)) {
            BlockPos delta = this.centerGizmo.getTargetPosition().subtract(oldCenterPos);
            this.firstGizmo.moveTo(this.firstGizmo.getTargetPosition().offset(delta));
            this.secondGizmo.moveTo(this.secondGizmo.getTargetPosition().offset(delta));
         }

         if (this.firstGizmo.enableAxes && lookDirection != null) {
            this.firstGizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), lookDirection);
         }

         if (this.secondGizmo.enableAxes && lookDirection != null) {
            this.secondGizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), lookDirection);
         }

         if (!oldFirstPos.equals(this.firstGizmo.getTargetPosition()) || !oldSecondPos.equals(this.secondGizmo.getTargetPosition())) {
            this.updateGizmoState(oldFirstPos, oldSecondPos);
         }

         return true;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case CUT:
         case COPY:
         case DELETE:
            if (this.dragOrigin != null) {
               RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
               if (result == null) {
                  return UserAction.ActionResult.NOT_HANDLED;
               }

               this.setupGizmos(this.dragOrigin, result.getBlockPos());
            }

            if (this.firstGizmo == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.reset();
            return UserAction.ActionResult.USED_CONT;
         case ENTER:
            if (this.dragOrigin != null) {
               RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
               if (result == null) {
                  return UserAction.ActionResult.NOT_HANDLED;
               }

               this.setupGizmos(this.dragOrigin, result.getBlockPos());
            }

            if (this.firstGizmo == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.reset();
            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            if (this.leftClick()) {
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case RIGHT_MOUSE:
            this.rightClick();
            return UserAction.ActionResult.USED_STOP;
         case SCROLL:
            UserAction.ScrollAmount scrollObject = (UserAction.ScrollAmount)object;
            if (this.handleScroll(scrollObject.scrollX(), scrollObject.scrollY())) {
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case UNDO:
            if (this.firstGizmo == null && this.dragOrigin == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.resetWithoutApplyingSelection();
            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   private void applyActiveSelection() {
      BlockPos one = this.firstGizmo.getTargetPosition();
      BlockPos two = this.secondGizmo.getTargetPosition();
      int minX = Math.min(one.getX(), two.getX());
      int minY = Math.min(one.getY(), two.getY());
      int minZ = Math.min(one.getZ(), two.getZ());
      int maxX = Math.max(one.getX(), two.getX());
      int maxY = Math.max(one.getY(), two.getY());
      int maxZ = Math.max(one.getZ(), two.getZ());
      MaskElement maskElement = MaskManager.getSelectionMask();
      if (maskElement instanceof ConstantMaskElement constantMaskElement) {
         if (constantMaskElement.getConstant()) {
            int mode = this.mode[0];
            if (mode == 0) {
               Selection.clearSelection();
               Selection.addAABB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            } else if (mode == 1) {
               Selection.addAABB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            } else if (mode == 2) {
               Selection.subtractAABB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            } else {
               if (mode != 3) {
                  throw new FaultyImplementationError();
               }

               Selection.intersectAABB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            }
         }
      } else {
         MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
         PositionSet positionSet = new PositionSet();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  if (maskElement.test(maskContext.reset(), x, y, z)) {
                     positionSet.add(x, y, z);
                  }
               }
            }
         }

         int mode = this.mode[0];
         if (mode == 0) {
            Selection.clearSelection();
            Selection.addSet(positionSet);
         } else if (mode == 1) {
            Selection.addSet(positionSet);
         } else if (mode == 2) {
            Selection.subtractSet(positionSet);
         } else {
            if (mode != 3) {
               throw new FaultyImplementationError();
            }

            Selection.intersectSet(positionSet);
         }
      }
   }

   @Override
   public void reset() {
      if (this.firstGizmo != null) {
         this.applyActiveSelection();
      }

      this.resetWithoutApplyingSelection();
   }

   private void resetWithoutApplyingSelection() {
      this.dragOrigin = null;
      this.firstGizmo = null;
      this.secondGizmo = null;
      this.centerGizmo = null;
      Arrays.fill(this.position, 0);
      Arrays.fill(this.size, 0);
   }

   private boolean leftClick() {
      if (this.firstGizmo != null && this.secondGizmo != null && this.centerGizmo != null) {
         if (EditorUI.isCtrlOrCmdDown() && !this.firstGizmo.isGrabbed() && !this.secondGizmo.isGrabbed() && !this.centerGizmo.isGrabbed()) {
            return false;
         }

         if (this.centerGizmo.leftClick()) {
            this.firstGizmo.enableAxes = false;
            this.secondGizmo.enableAxes = false;
            return true;
         }

         if (this.firstGizmo.leftClick()) {
            this.centerGizmo.enableAxes = false;
            this.secondGizmo.enableAxes = false;
            return true;
         }

         if (this.secondGizmo.leftClick()) {
            this.centerGizmo.enableAxes = false;
            this.firstGizmo.enableAxes = false;
            return true;
         }
      }

      return false;
   }

   private void rightClick() {
      if (this.dragOrigin == null) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
         if (result != null) {
            this.reset();
            this.dragOrigin = result.getBlockPos();
            this.dragging = true;
            boolean isEmpty = Selection.getSelectionBuffer().isEmpty();
            if (this.mode[0] == 0 && !isEmpty) {
               Selection.clearSelection();
            } else if (isEmpty && (this.mode[0] == 3 || this.mode[0] == 2)) {
               this.mode[0] = 0;
            }
         }
      } else if (!this.dragging) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
         if (result != null) {
            for (int i = 0; i < 3; i++) {
               Axis axis = Axis.VALUES[i];
               this.position[i] = Math.min(this.dragOrigin.get(axis), result.getBlockPos().get(axis));
               this.size[i] = Math.abs(this.dragOrigin.get(axis) - result.getBlockPos().get(axis)) + 1;
            }

            this.setupGizmos(this.dragOrigin, result.getBlockPos());
         }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.firstGizmo != null) {
         Objects.requireNonNull(this.secondGizmo);
         Objects.requireNonNull(this.centerGizmo);
         Selection.render(rc, 4);
         BlockPos oldCenterPos = this.centerGizmo.getTargetPosition();
         Vec3 lookDirection = Tool.getLookDirection();
         boolean isLeftDown = Tool.isMouseDown(0);
         boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
         boolean showGizmo = !EditorUI.isActive() || !isCtrlDown;
         this.centerGizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
         this.centerGizmo
            .setAxisDirections(
               rc.x() > this.centerGizmo.getTargetPosition().getX(),
               rc.y() > this.centerGizmo.getTargetPosition().getY(),
               rc.z() > this.centerGizmo.getTargetPosition().getZ()
            );
         BlockPos oldFirstPos = this.firstGizmo.getTargetPosition();
         BlockPos oldSecondPos = this.secondGizmo.getTargetPosition();
         if (!this.centerGizmo.getTargetPosition().equals(oldCenterPos)) {
            BlockPos delta = this.centerGizmo.getTargetPosition().subtract(oldCenterPos);
            this.firstGizmo.moveTo(this.firstGizmo.getTargetPosition().offset(delta));
            this.secondGizmo.moveTo(this.secondGizmo.getTargetPosition().offset(delta));
         }

         this.firstGizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
         this.secondGizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
         if (!oldFirstPos.equals(this.firstGizmo.getTargetPosition()) || !oldSecondPos.equals(this.secondGizmo.getTargetPosition())) {
            this.updateGizmoState(oldFirstPos, oldSecondPos);
         }

         if (showGizmo || this.firstGizmo.isGrabbed() || this.secondGizmo.isGrabbed() || this.centerGizmo.isGrabbed()) {
            this.firstGizmo.render(rc, isCtrlDown);
            this.secondGizmo.render(rc, isCtrlDown);
            this.centerGizmo.render(rc, isCtrlDown);
         }

         EffectRenderer.renderBoundingBox(rc, this.firstGizmo.getInterpPosition(), this.secondGizmo.getInterpPosition(), 3);
      } else {
         if (this.dragOrigin == null) {
            Selection.render(rc, 7);
         } else {
            Selection.render(rc, 4);
            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result == null) {
               if (this.dragging) {
                  if (!Tool.isMouseDown(1)) {
                     this.dragOrigin = null;
                  }

                  Arrays.fill(this.position, 0);
                  Arrays.fill(this.size, 0);
                  return;
               }

               return;
            }

            for (int i = 0; i < 3; i++) {
               Axis axis = Axis.VALUES[i];
               this.position[i] = Math.min(this.dragOrigin.get(axis), result.getBlockPos().get(axis));
               this.size[i] = Math.abs(this.dragOrigin.get(axis) - result.getBlockPos().get(axis)) + 1;
            }

            if (this.dragging && !Tool.isMouseDown(1)) {
               if (this.dragOrigin.equals(result.getBlockPos())) {
                  this.dragging = false;
               } else {
                  for (int i = 0; i < 3; i++) {
                     Axis axis = Axis.VALUES[i];
                     this.position[i] = Math.min(this.dragOrigin.get(axis), result.getBlockPos().get(axis));
                     this.size[i] = Math.abs(this.dragOrigin.get(axis) - result.getBlockPos().get(axis)) + 1;
                  }

                  this.setupGizmos(this.dragOrigin, result.getBlockPos());
               }
            } else {
               double minX = Math.min(this.dragOrigin.getX(), result.getBlockPos().getX());
               double minY = Math.min(this.dragOrigin.getY(), result.getBlockPos().getY());
               double minZ = Math.min(this.dragOrigin.getZ(), result.getBlockPos().getZ());
               double maxX = Math.max(this.dragOrigin.getX(), result.getBlockPos().getX());
               double maxY = Math.max(this.dragOrigin.getY(), result.getBlockPos().getY());
               double maxZ = Math.max(this.dragOrigin.getZ(), result.getBlockPos().getZ());
               EffectRenderer.renderBoundingBox(rc, new Vec3(minX, minY, minZ), new Vec3(maxX + 1.0, maxY + 1.0, maxZ + 1.0), 3);
            }
         }
      }
   }

   private void setupGizmos(BlockPos first, BlockPos second) {
      float offsetX = first.getX() <= second.getX() ? -0.5F : 0.5F;
      float offsetY = first.getY() <= second.getY() ? -0.5F : 0.5F;
      float offsetZ = first.getZ() <= second.getZ() ? -0.5F : 0.5F;
      float centerX = (first.getX() + second.getX()) / 2.0F;
      float centerY = (first.getY() + second.getY()) / 2.0F;
      float centerZ = (first.getZ() + second.getZ()) / 2.0F;
      this.firstGizmo = new Gizmo(first, new Vec3(offsetX, offsetY, offsetZ));
      this.secondGizmo = new Gizmo(second, new Vec3(-offsetX, -offsetY, -offsetZ));
      this.centerGizmo = new Gizmo(
         BlockPos.containing(centerX, centerY, centerZ), new Vec3(centerX - Math.floor(centerX), centerY - Math.floor(centerY), centerZ - Math.floor(centerZ))
      );
      boolean firstXGreater = this.firstGizmo.getInterpPosition().x() > this.secondGizmo.getInterpPosition().x();
      boolean firstYGreater = this.firstGizmo.getInterpPosition().y() > this.secondGizmo.getInterpPosition().y();
      boolean firstZGreater = this.firstGizmo.getInterpPosition().z() > this.secondGizmo.getInterpPosition().z();
      this.firstGizmo.setAxisDirections(firstXGreater, firstYGreater, firstZGreater);
      this.secondGizmo.setAxisDirections(!firstXGreater, !firstYGreater, !firstZGreater);
      this.firstGizmo.enableAxes = false;
      this.secondGizmo.enableAxes = true;
      this.centerGizmo.enableAxes = false;
      this.dragOrigin = null;
   }

   private void updateGizmoState(BlockPos oldFirstPos, BlockPos oldSecondPos) {
      this.maybeSwapOffsetsForAxis(oldFirstPos, oldSecondPos, Axis.X);
      this.maybeSwapOffsetsForAxis(oldFirstPos, oldSecondPos, Axis.Y);
      this.maybeSwapOffsetsForAxis(oldFirstPos, oldSecondPos, Axis.Z);
      BlockPos firstPos = this.firstGizmo.getTargetPosition();
      BlockPos secondPos = this.secondGizmo.getTargetPosition();
      boolean firstXGreater = this.firstGizmo.getInterpPosition().x() > this.secondGizmo.getInterpPosition().x();
      boolean firstYGreater = this.firstGizmo.getInterpPosition().y() > this.secondGizmo.getInterpPosition().y();
      boolean firstZGreater = this.firstGizmo.getInterpPosition().z() > this.secondGizmo.getInterpPosition().z();
      this.firstGizmo.setAxisDirections(firstXGreater, firstYGreater, firstZGreater);
      this.secondGizmo.setAxisDirections(!firstXGreater, !firstYGreater, !firstZGreater);
      float centerX = (firstPos.getX() + secondPos.getX()) / 2.0F;
      float centerY = (firstPos.getY() + secondPos.getY()) / 2.0F;
      float centerZ = (firstPos.getZ() + secondPos.getZ()) / 2.0F;
      this.centerGizmo.moveTo(BlockPos.containing(centerX, centerY, centerZ));
      this.centerGizmo.setOffset(new Vec3(centerX - Math.floor(centerX), centerY - Math.floor(centerY), centerZ - Math.floor(centerZ)));

      for (int i = 0; i < 3; i++) {
         Axis axis = Axis.VALUES[i];
         this.position[i] = Math.min(firstPos.get(axis), secondPos.get(axis));
         this.size[i] = Math.abs(firstPos.get(axis) - secondPos.get(axis)) + 1;
      }
   }

   private void maybeSwapOffsetsForAxis(BlockPos oldFirstPos, BlockPos oldSecondPos, Axis axis) {
      int size = this.firstGizmo.getTargetPosition().get(axis) - this.secondGizmo.getTargetPosition().get(axis);
      if (size > 0 && this.firstGizmo.getOffset().get(axis) < 0.0 || size < 0 && this.firstGizmo.getOffset().get(axis) > 0.0) {
         double temp = this.firstGizmo.getOffset().get(axis);
         this.firstGizmo.setOffset(this.firstGizmo.getOffset().with(axis, this.secondGizmo.getOffset().get(axis)));
         this.secondGizmo.setOffset(this.secondGizmo.getOffset().with(axis, temp));
         int firstDelta = oldFirstPos.get(axis) - this.firstGizmo.getTargetPosition().get(axis);
         if (firstDelta > 0) {
            this.firstGizmo.moveTo(this.firstGizmo.getTargetPosition().relative(axis, 1));
         } else if (firstDelta < 0) {
            this.firstGizmo.moveTo(this.firstGizmo.getTargetPosition().relative(axis, -1));
         }

         int secondDelta = oldSecondPos.get(axis) - this.secondGizmo.getTargetPosition().get(axis);
         if (secondDelta > 0) {
            this.secondGizmo.moveTo(this.secondGizmo.getTargetPosition().relative(axis, 1));
         } else if (secondDelta < 0) {
            this.secondGizmo.moveTo(this.secondGizmo.getTargetPosition().relative(axis, -1));
         }
      }
   }

   private void updateGizmosFromPositionSize() {
      for (int i = 0; i < 4; i++) {
         if (i == 3) {
            return;
         }

         if (this.position[i] != 0 || this.size[i] != 0) {
            break;
         }
      }

      if (this.firstGizmo == null) {
         BlockPos first = new BlockPos(this.position[0], this.position[1], this.position[2]);
         BlockPos second = first.offset(this.size[0] - 1, this.size[1] - 1, this.size[2] - 1);
         this.setupGizmos(first, second);
      } else {
         BlockPos oldFirstPos = this.firstGizmo.getTargetPosition();
         BlockPos oldSecondPos = this.secondGizmo.getTargetPosition();
         MutableBlockPos newFirstPos = new MutableBlockPos();
         MutableBlockPos newSecondPos = new MutableBlockPos();

         for (int i = 0; i < 3; i++) {
            Axis axis = Axis.VALUES[i];
            int oldFirstPosAxis = oldFirstPos.get(axis);
            int oldSecondPosAxis = oldSecondPos.get(axis);
            MutableBlockPos lesser;
            MutableBlockPos greater;
            if (oldFirstPosAxis < oldSecondPosAxis) {
               lesser = newFirstPos;
               greater = newSecondPos;
            } else if (oldFirstPosAxis > oldSecondPosAxis) {
               lesser = newSecondPos;
               greater = newFirstPos;
            } else {
               double firstOffsetAxis = this.firstGizmo.getOffset().get(axis);
               double secondOffsetAxis = this.secondGizmo.getOffset().get(axis);
               if (firstOffsetAxis < secondOffsetAxis) {
                  lesser = newFirstPos;
                  greater = newSecondPos;
               } else {
                  if (!(firstOffsetAxis > secondOffsetAxis)) {
                     throw new FaultyImplementationError("Offsets are equal");
                  }

                  lesser = newSecondPos;
                  greater = newFirstPos;
               }
            }

            if (this.size[i] == 0) {
               this.size[i] = 1;
            } else if (this.size[i] < 0) {
               this.position[i] = this.position[i] + this.size[i];
               this.size[i] = Math.abs(this.size[i]);
            }

            switch (axis) {
               case X:
                  lesser.setX(this.position[i]);
                  greater.setX(this.position[i] + this.size[i] - 1);
                  break;
               case Y:
                  lesser.setY(this.position[i]);
                  greater.setY(this.position[i] + this.size[i] - 1);
                  break;
               case Z:
                  lesser.setZ(this.position[i]);
                  greater.setZ(this.position[i] + this.size[i] - 1);
            }
         }

         this.firstGizmo.moveTo(newFirstPos);
         this.secondGizmo.moveTo(newSecondPos);
         if (!oldFirstPos.equals(this.firstGizmo.getTargetPosition()) || !oldSecondPos.equals(this.secondGizmo.getTargetPosition())) {
            this.updateGizmoState(oldFirstPos, oldSecondPos);
         }
      }
   }

   private void shrinkToFit() {
      if (this.firstGizmo != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            int minWorldSectionY = level.getMinSection();
            BlockPos firstPosition = this.firstGizmo.getTargetPosition();
            BlockPos secondPosition = this.secondGizmo.getTargetPosition();
            MutableBlockPos minPosition = new MutableBlockPos(
               Math.min(firstPosition.getX(), secondPosition.getX()),
               Math.max(Math.min(firstPosition.getY(), secondPosition.getY()), level.getMinBuildHeight()),
               Math.min(firstPosition.getZ(), secondPosition.getZ())
            );
            MutableBlockPos maxPosition = new MutableBlockPos(
               Math.max(firstPosition.getX(), secondPosition.getX()),
               Math.min(Math.max(firstPosition.getY(), secondPosition.getY()), level.getMaxBuildHeight() - 1),
               Math.max(firstPosition.getZ(), secondPosition.getZ())
            );
            int minSectionX = minPosition.getX() >> 4;
            int minSectionY = minPosition.getY() >> 4;
            int minSectionZ = minPosition.getZ() >> 4;
            int maxSectionX = maxPosition.getX() >> 4;
            int maxSectionY = maxPosition.getY() >> 4;
            int maxSectionZ = maxPosition.getZ() >> 4;
            boolean foundAnyBlock = false;
            int locMinX = minPosition.getX() & 15;
            int locMinY = minPosition.getY() & 15;
            int locMinZ = minPosition.getZ() & 15;
            int locMaxX = maxPosition.getX() & 15;
            int locMaxY = maxPosition.getY() & 15;
            int locMaxZ = maxPosition.getZ() & 15;

            for (int x = minSectionX; x <= maxSectionX; x++) {
               int fromX = x == minSectionX ? locMinX : 0;
               int toX = x == maxSectionX ? locMaxX : 15;
               int foundMinX = toX + 1;

               label599:
               for (int z = minSectionZ; z <= maxSectionZ; z++) {
                  int fromZ = z == minSectionZ ? locMinZ : 0;
                  int toZ = z == maxSectionZ ? locMaxZ : 15;
                  LevelChunk chunk = level.getChunk(x, z);

                  for (int y = minSectionY; y <= maxSectionY; y++) {
                     int fromY = y == minSectionY ? locMinY : 0;
                     int toY = y == maxSectionY ? locMaxY : 15;
                     LevelChunkSection section = chunk.getSection(y - minWorldSectionY);
                     if (!section.hasOnlyAir()) {
                        PalettedContainer<BlockState> states = section.getStates();

                        label589:
                        for (int lx = fromX; lx < foundMinX; lx++) {
                           for (int ly = fromY; ly <= toY; ly++) {
                              for (int lz = fromZ; lz <= toZ; lz++) {
                                 if (!((BlockState)states.get(lx, ly, lz)).isAir()) {
                                    foundMinX = lx;
                                    break label589;
                                 }
                              }
                           }
                        }

                        if (foundMinX == fromX) {
                           break label599;
                        }
                     }
                  }
               }

               if (foundMinX < toX + 1) {
                  foundAnyBlock = true;
                  minPosition.setX(x * 16 + foundMinX);
                  minSectionX = minPosition.getX() >> 4;
                  locMinX = minPosition.getX() & 15;
                  break;
               }
            }

            if (!foundAnyBlock) {
               this.resetWithoutApplyingSelection();
            } else {
               for (int x = maxSectionX; x >= minSectionX; x--) {
                  int fromX = x == minSectionX ? locMinX : 0;
                  int toX = x == maxSectionX ? locMaxX : 15;
                  int foundMaxX = fromX - 1;

                  label556:
                  for (int z = minSectionZ; z <= maxSectionZ; z++) {
                     int fromZ = z == minSectionZ ? locMinZ : 0;
                     int toZ = z == maxSectionZ ? locMaxZ : 15;
                     LevelChunk chunk = level.getChunk(x, z);

                     for (int yx = minSectionY; yx <= maxSectionY; yx++) {
                        int fromY = yx == minSectionY ? locMinY : 0;
                        int toY = yx == maxSectionY ? locMaxY : 15;
                        LevelChunkSection section = chunk.getSection(yx - minWorldSectionY);
                        if (!section.hasOnlyAir()) {
                           PalettedContainer<BlockState> states = section.getStates();

                           label546:
                           for (int lx = toX; lx > foundMaxX; lx--) {
                              for (int ly = fromY; ly <= toY; ly++) {
                                 for (int lzx = fromZ; lzx <= toZ; lzx++) {
                                    if (!((BlockState)states.get(lx, ly, lzx)).isAir()) {
                                       foundMaxX = lx;
                                       break label546;
                                    }
                                 }
                              }
                           }

                           if (foundMaxX == toX) {
                              break label556;
                           }
                        }
                     }
                  }

                  if (foundMaxX > fromX - 1) {
                     maxPosition.setX(x * 16 + foundMaxX);
                     maxSectionX = maxPosition.getX() >> 4;
                     locMaxX = maxPosition.getX() & 15;
                     break;
                  }
               }

               for (int z = minSectionZ; z <= maxSectionZ; z++) {
                  int fromZ = z == minSectionZ ? locMinZ : 0;
                  int toZ = z == maxSectionZ ? locMaxZ : 15;
                  int foundMinZ = toZ + 1;

                  label513:
                  for (int x = minSectionX; x <= maxSectionX; x++) {
                     int fromX = x == minSectionX ? locMinX : 0;
                     int toX = x == maxSectionX ? locMaxX : 15;
                     LevelChunk chunk = level.getChunk(x, z);

                     for (int yxx = minSectionY; yxx <= maxSectionY; yxx++) {
                        int fromY = yxx == minSectionY ? locMinY : 0;
                        int toY = yxx == maxSectionY ? locMaxY : 15;
                        LevelChunkSection section = chunk.getSection(yxx - minWorldSectionY);
                        if (!section.hasOnlyAir()) {
                           PalettedContainer<BlockState> states = section.getStates();

                           label503:
                           for (int lzxx = fromZ; lzxx < foundMinZ; lzxx++) {
                              for (int lx = fromX; lx <= toX; lx++) {
                                 for (int ly = fromY; ly <= toY; ly++) {
                                    if (!((BlockState)states.get(lx, ly, lzxx)).isAir()) {
                                       foundMinZ = lzxx;
                                       break label503;
                                    }
                                 }
                              }
                           }

                           if (foundMinZ == fromZ) {
                              break label513;
                           }
                        }
                     }
                  }

                  if (foundMinZ < toZ + 1) {
                     minPosition.setZ(z * 16 + foundMinZ);
                     minSectionZ = minPosition.getZ() >> 4;
                     locMinZ = minPosition.getZ() & 15;
                     break;
                  }
               }

               for (int z = maxSectionZ; z >= minSectionZ; z--) {
                  int fromZ = z == minSectionZ ? locMinZ : 0;
                  int toZ = z == maxSectionZ ? locMaxZ : 15;
                  int foundMaxZ = fromZ - 1;

                  label470:
                  for (int x = minSectionX; x <= maxSectionX; x++) {
                     int fromX = x == minSectionX ? locMinX : 0;
                     int toX = x == maxSectionX ? locMaxX : 15;
                     LevelChunk chunk = level.getChunk(x, z);

                     for (int yxxx = minSectionY; yxxx <= maxSectionY; yxxx++) {
                        int fromY = yxxx == minSectionY ? locMinY : 0;
                        int toY = yxxx == maxSectionY ? locMaxY : 15;
                        LevelChunkSection section = chunk.getSection(yxxx - minWorldSectionY);
                        if (!section.hasOnlyAir()) {
                           PalettedContainer<BlockState> states = section.getStates();

                           label460:
                           for (int lzxx = toZ; lzxx > foundMaxZ; lzxx--) {
                              for (int lx = fromX; lx <= toX; lx++) {
                                 for (int lyx = fromY; lyx <= toY; lyx++) {
                                    if (!((BlockState)states.get(lx, lyx, lzxx)).isAir()) {
                                       foundMaxZ = lzxx;
                                       break label460;
                                    }
                                 }
                              }
                           }

                           if (foundMaxZ == toZ) {
                              break label470;
                           }
                        }
                     }
                  }

                  if (foundMaxZ > fromZ - 1) {
                     maxPosition.setZ(z * 16 + foundMaxZ);
                     maxSectionZ = maxPosition.getZ() >> 4;
                     locMaxZ = maxPosition.getZ() & 15;
                     break;
                  }
               }

               for (int yxxxx = minSectionY; yxxxx <= maxSectionY; yxxxx++) {
                  int fromY = yxxxx == minSectionY ? locMinY : 0;
                  int toY = yxxxx == maxSectionY ? locMaxY : 15;
                  int foundMinY = toY + 1;

                  label427:
                  for (int z = minSectionZ; z <= maxSectionZ; z++) {
                     int fromZ = z == minSectionZ ? locMinZ : 0;
                     int toZ = z == maxSectionZ ? locMaxZ : 15;

                     for (int x = minSectionX; x <= maxSectionX; x++) {
                        int fromX = x == minSectionX ? locMinX : 0;
                        int toX = x == maxSectionX ? locMaxX : 15;
                        LevelChunk chunk = level.getChunk(x, z);
                        LevelChunkSection section = chunk.getSection(yxxxx - minWorldSectionY);
                        if (!section.hasOnlyAir()) {
                           PalettedContainer<BlockState> states = section.getStates();

                           label417:
                           for (int lyxx = fromY; lyxx < foundMinY; lyxx++) {
                              for (int lx = fromX; lx <= toX; lx++) {
                                 for (int lzxx = fromZ; lzxx <= toZ; lzxx++) {
                                    if (!((BlockState)states.get(lx, lyxx, lzxx)).isAir()) {
                                       foundMinY = lyxx;
                                       break label417;
                                    }
                                 }
                              }
                           }

                           if (foundMinY == fromY) {
                              break label427;
                           }
                        }
                     }
                  }

                  if (foundMinY < toY + 1) {
                     minPosition.setY(yxxxx * 16 + foundMinY);
                     minSectionY = minPosition.getY() >> 4;
                     locMinY = minPosition.getY() & 15;
                     break;
                  }
               }

               for (int yxxxx = maxSectionY; yxxxx >= minSectionY; yxxxx--) {
                  int fromY = yxxxx == minSectionY ? locMinY : 0;
                  int toY = yxxxx == maxSectionY ? locMaxY : 15;
                  int foundMaxY = fromY - 1;

                  label384:
                  for (int z = minSectionZ; z <= maxSectionZ; z++) {
                     int fromZ = z == minSectionZ ? locMinZ : 0;
                     int toZ = z == maxSectionZ ? locMaxZ : 15;

                     for (int xx = minSectionX; xx <= maxSectionX; xx++) {
                        int fromX = xx == minSectionX ? locMinX : 0;
                        int toX = xx == maxSectionX ? locMaxX : 15;
                        LevelChunk chunk = level.getChunk(xx, z);
                        LevelChunkSection section = chunk.getSection(yxxxx - minWorldSectionY);
                        if (!section.hasOnlyAir()) {
                           PalettedContainer<BlockState> states = section.getStates();

                           label374:
                           for (int lyxx = toY; lyxx > foundMaxY; lyxx--) {
                              for (int lx = fromX; lx <= toX; lx++) {
                                 for (int lzxxx = fromZ; lzxxx <= toZ; lzxxx++) {
                                    if (!((BlockState)states.get(lx, lyxx, lzxxx)).isAir()) {
                                       foundMaxY = lyxx;
                                       break label374;
                                    }
                                 }
                              }
                           }

                           if (foundMaxY == toY) {
                              break label384;
                           }
                        }
                     }
                  }

                  if (foundMaxY > fromY - 1) {
                     maxPosition.setY(yxxxx * 16 + foundMaxY);
                     break;
                  }
               }

               this.position[0] = minPosition.getX();
               this.position[1] = minPosition.getY();
               this.position[2] = minPosition.getZ();
               this.size[0] = maxPosition.getX() - minPosition.getX() + 1;
               this.size[1] = maxPosition.getY() - minPosition.getY() + 1;
               this.size[2] = maxPosition.getZ() - minPosition.getZ() + 1;
               this.updateGizmosFromPositionSize();
            }
         }
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.box_select"));
      ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.box_select.input_mode"),
         this.inputMode,
         new String[]{AxiomI18n.get("axiom.tool.box_select.input_mode.position_and_size"), AxiomI18n.get("axiom.tool.box_select.input_mode.pos1_and_pos2")}
      );
      boolean changed = false;
      if (this.inputMode[0] == 0) {
         changed |= ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.box_select.position"), this.position);
         changed |= ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.box_select.size"), this.size);
      } else {
         int[] pos1 = new int[]{0, 0, 0};
         int[] pos2 = new int[]{0, 0, 0};
         if (this.firstGizmo != null) {
            BlockPos first = this.firstGizmo.getTargetPosition();
            pos1[0] = first.getX();
            pos1[1] = first.getY();
            pos1[2] = first.getZ();
         }

         if (this.secondGizmo != null) {
            BlockPos second = this.secondGizmo.getTargetPosition();
            pos2[0] = second.getX();
            pos2[1] = second.getY();
            pos2[2] = second.getZ();
         }

         changed |= ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.box_select.pos1"), pos1);
         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.set") + "##SetPos1")) {
            BlockPos blockPos = Minecraft.getInstance().player.blockPosition();
            pos1[0] = blockPos.getX();
            pos1[1] = blockPos.getY();
            pos1[2] = blockPos.getZ();
            changed = true;
         }

         ImGuiHelper.tooltip("Set Pos1 to the player's current position");
         changed |= ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.box_select.pos2"), pos2);
         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.set") + "##SetPos2")) {
            BlockPos blockPos = Minecraft.getInstance().player.blockPosition();
            pos2[0] = blockPos.getX();
            pos2[1] = blockPos.getY();
            pos2[2] = blockPos.getZ();
            changed = true;
         }

         ImGuiHelper.tooltip("Set Pos2 to the player's current position");
         if (changed) {
            if (this.firstGizmo != null && this.secondGizmo != null) {
               BlockPos oldFirstPos = this.firstGizmo.getTargetPosition();
               BlockPos oldSecondPos = this.secondGizmo.getTargetPosition();
               this.firstGizmo.moveTo(new BlockPos(pos1[0], pos1[1], pos1[2]));
               this.secondGizmo.moveTo(new BlockPos(pos2[0], pos2[1], pos2[2]));
               this.maybeSwapOffsetsForAxis(oldFirstPos, oldSecondPos, Axis.X);
               this.maybeSwapOffsetsForAxis(oldFirstPos, oldSecondPos, Axis.Y);
               this.maybeSwapOffsetsForAxis(oldFirstPos, oldSecondPos, Axis.Z);
            } else {
               this.setupGizmos(new BlockPos(pos1[0], pos1[1], pos1[2]), new BlockPos(pos2[0], pos2[1], pos2[2]));
            }

            this.position[0] = Math.min(pos1[0], pos2[0]);
            this.position[1] = Math.min(pos1[1], pos2[1]);
            this.position[2] = Math.min(pos1[2], pos2[2]);
            this.size[0] = Math.abs(pos1[0] - pos2[0]) + 1;
            this.size[1] = Math.abs(pos1[1] - pos2[1]) + 1;
            this.size[2] = Math.abs(pos1[2] - pos2[2]) + 1;
         }
      }

      boolean sizePositive = this.size[0] >= 0 && this.size[1] >= 0 && this.size[2] >= 0;
      if ((changed || !sizePositive) && (!ImGui.isAnyItemActive() || sizePositive)) {
         this.updateGizmosFromPositionSize();
      }

      boolean shrinkDisabled = this.firstGizmo == null;
      if (shrinkDisabled) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.box_select.shrink"))) {
         this.shrinkToFit();
      }

      if (shrinkDisabled) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      ImGuiHelper.helpMarker(AxiomI18n.get("axiom.tool.box_select.shrink_description"));
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.selection"));
      ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.selection.mode"),
         this.mode,
         new String[]{
            AxiomI18n.get("axiom.tool.selection.replace"),
            AxiomI18n.get("axiom.tool.selection.add"),
            AxiomI18n.get("axiom.tool.selection.subtract"),
            AxiomI18n.get("axiom.tool.selection.intersect")
         }
      );
      if (this.firstGizmo != null) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.selection.actions"));
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.confirm_selection", Keybinds.CONFIRM.longKeyIdentifier()))) {
            this.reset();
         }
      } else if (Selection.selectedBlockCount() > 0) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.selection.actions"));
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.clear_selection", Keybinds.CONFIRM.longKeyIdentifier()))) {
            Selection.clearSelection();
         }

         if (Selection.getSelectionBuffer() instanceof SelectionBuffer.AABB aabb && ImGui.button(AxiomI18n.get("axiom.hardcoded.unconfirm"))) {
            Selection.clearSelection();
            BlockPos min = aabb.min();
            BlockPos max = aabb.max();
            this.position[0] = min.getX();
            this.position[1] = min.getY();
            this.position[2] = min.getZ();
            this.size[0] = max.getX() - min.getX() + 1;
            this.size[1] = max.getY() - min.getY() + 1;
            this.size[2] = max.getZ() - min.getZ() + 1;
            this.updateGizmosFromPositionSize();
         }
      }

      if (Axiom.configuration.entityManipulation.showMarkerEntities) {
         if (this.firstGizmo != null) {
            if (ImGui.button(AxiomI18n.get("axiom.tool.box_select.box_select.create_marker_region"))) {
               BlockPos first = this.firstGizmo.getTargetPosition();
               BlockPos second = this.secondGizmo.getTargetPosition();
               MarkerEntityManipulator.spawnNewMarkerRegion(first, second);
            }
         } else if (Selection.getSelectionBuffer() instanceof SelectionBuffer.AABB aabb
            && ImGui.button(AxiomI18n.get("axiom.tool.box_select.box_select.create_marker_region"))) {
            BlockPos first = aabb.min();
            BlockPos second = aabb.max();
            MarkerEntityManipulator.spawnNewMarkerRegion(first, second);
         }
      }
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.box_select");
   }

   @Override
   public Tutorial getTutorial() {
      return Tutorial.BOX_SELECT_TOOL;
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putByte("SelectionMode", (byte)this.mode[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.mode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "SelectionMode", 1);
   }

   @Override
   public char iconChar() {
      return '\ue903';
   }

   @Override
   public String keybindId() {
      return "box_select";
   }

   @Override
   public int defaultKeybind() {
      return 66;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_BOXSELECT);
   }
}
