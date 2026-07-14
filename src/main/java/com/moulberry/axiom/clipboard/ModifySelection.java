package com.moulberry.axiom.clipboard;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.world_modification.Dispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ModifySelection {
   private static Gizmo gizmo = null;
   private static ChunkedBooleanRegion selectionRegion = null;
   private static float lastScaleX = 1.0F;
   private static float lastScaleY = 1.0F;
   private static float lastScaleZ = 1.0F;
   private static float appliedScaleX = 1.0F;
   private static float appliedScaleY = 1.0F;
   private static float appliedScaleZ = 1.0F;
   private static ChunkedBooleanRegion scaledSelectionRegion = null;
   private static Vec3 originalSelectionCenter = Vec3.ZERO;
   private static boolean hasMutatedSelectionRegion = false;

   public static UserAction.ActionResult callAction(UserAction action, Object object) {
      if (isModifyingSelection() && EditorUI.isActive() && !ToolManager.isToolActive()) {
         switch (action) {
            case ROTATE_PLACEMENT:
               rotateY();
               break;
            case FLIP_PLACEMENT:
               Vec3 lookDirection = Tool.getLookDirection();
               if (lookDirection == null) {
                  lookDirection = Minecraft.getInstance().cameraEntity.getLookAngle();
               }

               Direction direction = PositionUtils.orderedByNearest(lookDirection)[0];
               flip(direction.getAxis());
               break;
            case LEFT_MOUSE:
               if (!leftClick()) {
                  return UserAction.ActionResult.NOT_HANDLED;
               }
               break;
            case SCROLL:
               UserAction.ScrollAmount scrollAmount = (UserAction.ScrollAmount)object;
               if (!handleScroll(scrollAmount.scrollX(), scrollAmount.scrollY())) {
                  return UserAction.ActionResult.NOT_HANDLED;
               }
               break;
            case UNDO:
               gizmo = null;
               selectionRegion = null;
               break;
            case REDO:
               gizmo = null;
               selectionRegion = null;
               return Dispatcher.callAction(action, object);
            case SAVE:
            case ESCAPE:
               return UserAction.ActionResult.NOT_HANDLED;
            case DELETE:
               cancelAndClearSelection();
               break;
            case ENTER:
            case PASTE:
               finish();
         }

         return UserAction.ActionResult.USED_STOP;
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   private static boolean leftClick() {
      return gizmo != null && (!EditorUI.isCtrlOrCmdDown() || gizmo.isGrabbed()) ? gizmo.leftClick() : false;
   }

   public static boolean handleScroll(int xScroll, int yScroll) {
      if (gizmo != null && (!EditorUI.isCtrlOrCmdDown() || gizmo.isGrabbed())) {
         Vec3 look = Tool.getLookDirection();
         if (look != null) {
            gizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), look);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void start() {
      if (!isModifyingSelection()) {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         BlockPos max = selectionBuffer.max();
         BlockPos min = selectionBuffer.min();
         if (!selectionBuffer.isEmpty() && min != null && max != null) {
            Vec3 center = new Vec3((max.getX() + min.getX() + 1) * 0.5, (max.getY() + min.getY() + 1) * 0.5, (max.getZ() + min.getZ() + 1) * 0.5);
            BlockPos centerBlockPos = BlockPos.containing(center);
            Vec3 offsetFromCenter = new Vec3(
               center.x - (centerBlockPos.getX() + 0.5), center.y - (centerBlockPos.getY() + 0.5), center.z - (centerBlockPos.getZ() + 0.5)
            );
            ToolManager.setToolSelected(false);
            gizmo = new Gizmo(centerBlockPos, offsetFromCenter);
            gizmo.enableScale = true;
            hasMutatedSelectionRegion = false;
            originalSelectionCenter = center;
            if (selectionBuffer instanceof SelectionBuffer.Set set) {
               selectionRegion = new ChunkedBooleanRegion(set.selectionRegion.unsafeGetPositionSet());
            } else if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
               int minX = aabb.min().getX();
               int minY = aabb.min().getY();
               int minZ = aabb.min().getZ();
               int maxX = aabb.max().getX();
               int maxY = aabb.max().getY();
               int maxZ = aabb.max().getZ();
               ChunkedBooleanRegion region = new ChunkedBooleanRegion();

               for (int x = minX; x <= maxX; x++) {
                  for (int y = minY; y <= maxY; y++) {
                     for (int z = minZ; z <= maxZ; z++) {
                        region.add(x, y, z);
                     }
                  }
               }

               selectionRegion = region;
            } else {
               ChunkedBooleanRegion region = new ChunkedBooleanRegion();
               selectionBuffer.forEach(region::add);
               selectionRegion = region;
            }

            Entity camera = Minecraft.getInstance().cameraEntity;
            if (camera != null) {
               gizmo.setAxisDirections(
                  camera.position().x() > gizmo.getTargetPosition().getX(),
                  camera.position().y() > gizmo.getTargetPosition().getY(),
                  camera.position().z() > gizmo.getTargetPosition().getZ()
               );
            }
         }
      }
   }

   public static void cancelAndClearSelection() {
      if (gizmo != null) {
         gizmo = null;
         selectionRegion = null;
         scaledSelectionRegion = null;
         Selection.clearSelection();
      }
   }

   public static void finish() {
      if (gizmo != null) {
         applyIfScaled();
         BlockPos offset = gizmo.getTargetPosition().subtract(BlockPos.containing(originalSelectionCenter));
         if (offset.equals(BlockPos.ZERO)) {
            if (hasMutatedSelectionRegion) {
               Selection.set(selectionRegion);
            }
         } else {
            ChunkedBooleanRegion region = new ChunkedBooleanRegion();
            int xo = offset.getX();
            int yo = offset.getY();
            int zo = offset.getZ();
            selectionRegion.forEach((x, y, z) -> region.add(x + xo, y + yo, z + zo));
            Selection.set(region);
         }

         selectionRegion = null;
         scaledSelectionRegion = null;
         gizmo = null;
      }
   }

   public static void applyIfScaled() {
      if (scaledSelectionRegion != null) {
         selectionRegion = scaledSelectionRegion;
         scaledSelectionRegion = null;
         appliedScaleX = 1.0F;
         appliedScaleY = 1.0F;
         appliedScaleZ = 1.0F;
         lastScaleX = 1.0F;
         lastScaleY = 1.0F;
         lastScaleZ = 1.0F;
      }
   }

   public static void rotateY() {
      applyIfScaled();
      BlockPos offsetCurrent = gizmo.getTargetPosition();
      double negateOriginalX = originalSelectionCenter.x;
      double negateOriginalY = originalSelectionCenter.y;
      double negateOriginalZ = originalSelectionCenter.z;
      int offsetCurrentX = offsetCurrent.getX();
      int offsetCurrentY = offsetCurrent.getY();
      int offsetCurrentZ = offsetCurrent.getZ();
      ChunkedBooleanRegion region = new ChunkedBooleanRegion();
      selectionRegion.forEach(
         (x, y, z) -> region.add(
            (int)Math.floor(offsetCurrentX - (z + 0.5 - negateOriginalZ)),
            (int)Math.floor(offsetCurrentY + y + 0.5 - negateOriginalY),
            (int)Math.floor(offsetCurrentZ + x + 0.5 - negateOriginalX)
         )
      );
      selectionRegion = region;
      originalSelectionCenter = originalSelectionCenter.add(
         (int)Math.floor(offsetCurrentX + 0.5 - negateOriginalX),
         (int)Math.floor(offsetCurrentY + 0.5 - negateOriginalY),
         (int)Math.floor(offsetCurrentZ + 0.5 - negateOriginalZ)
      );
      hasMutatedSelectionRegion = true;
      Vec3 offsetFromCenter = new Vec3(
         originalSelectionCenter.x - (offsetCurrentX + 0.5),
         originalSelectionCenter.y - (offsetCurrentY + 0.5),
         originalSelectionCenter.z - (offsetCurrentZ + 0.5)
      );
      gizmo.setOffsetInstantly(offsetFromCenter);
   }

   public static void flip(Axis axis) {
      applyIfScaled();
      BlockPos offsetCurrent = gizmo.getTargetPosition();
      double negateOriginalX = originalSelectionCenter.x;
      double negateOriginalY = originalSelectionCenter.y;
      double negateOriginalZ = originalSelectionCenter.z;
      int offsetCurrentX = offsetCurrent.getX();
      int offsetCurrentY = offsetCurrent.getY();
      int offsetCurrentZ = offsetCurrent.getZ();
      ChunkedBooleanRegion region = new ChunkedBooleanRegion();
      switch (axis) {
         case X:
            selectionRegion.forEach(
               (x, y, z) -> region.add(
                  (int)Math.floor(offsetCurrentX - (x + 0.5 - negateOriginalX)),
                  (int)Math.floor(offsetCurrentY + (y + 0.5 - negateOriginalY)),
                  (int)Math.floor(offsetCurrentZ + (z + 0.5 - negateOriginalZ))
               )
            );
            break;
         case Y:
            selectionRegion.forEach(
               (x, y, z) -> region.add(
                  (int)Math.floor(offsetCurrentX + (x + 0.5 - negateOriginalX)),
                  (int)Math.floor(offsetCurrentY - (y + 0.5 - negateOriginalY)),
                  (int)Math.floor(offsetCurrentZ + (z + 0.5 - negateOriginalZ))
               )
            );
            break;
         case Z:
            selectionRegion.forEach(
               (x, y, z) -> region.add(
                  (int)Math.floor(offsetCurrentX + (x + 0.5 - negateOriginalX)),
                  (int)Math.floor(offsetCurrentY + (y + 0.5 - negateOriginalY)),
                  (int)Math.floor(offsetCurrentZ - (z + 0.5 - negateOriginalZ))
               )
            );
      }

      selectionRegion = region;
      originalSelectionCenter = originalSelectionCenter.add(
         (int)Math.floor(offsetCurrentX + 0.5 - negateOriginalX),
         (int)Math.floor(offsetCurrentY + 0.5 - negateOriginalY),
         (int)Math.floor(offsetCurrentZ + 0.5 - negateOriginalZ)
      );
      hasMutatedSelectionRegion = true;
      Vec3 offsetFromCenter = new Vec3(
         originalSelectionCenter.x - (offsetCurrentX + 0.5),
         originalSelectionCenter.y - (offsetCurrentY + 0.5),
         originalSelectionCenter.z - (offsetCurrentZ + 0.5)
      );
      gizmo.setOffsetInstantly(offsetFromCenter);
   }

   public static void render(AxiomWorldRenderContext rc) {
      if (gizmo != null) {
         if (EditorUI.isActive() && !ToolManager.isToolActive()) {
            Vec3 lookDirection = Tool.getLookDirection();
            boolean isLeftDown = Tool.isMouseDown(0);
            boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
            boolean showGizmo = !isCtrlDown;
            if (lookDirection != null) {
               gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
               gizmo.setAxisDirections(
                  rc.x() > gizmo.getTargetPosition().getX(), rc.y() > gizmo.getTargetPosition().getY(), rc.z() > gizmo.getTargetPosition().getZ()
               );
            }

            Gizmo.GizmoScale poppedScale = gizmo.popScale();
            if (poppedScale != null) {
               float scaleX = poppedScale.getScaleX();
               float scaleY = poppedScale.getScaleY();
               float scaleZ = poppedScale.getScaleZ();
               appliedScaleX *= scaleX;
               appliedScaleY *= scaleY;
               appliedScaleZ *= scaleZ;
               lastScaleX = appliedScaleX;
               lastScaleY = appliedScaleY;
               lastScaleZ = appliedScaleZ;
               scaledSelectionRegion = createScaled(appliedScaleX, appliedScaleY, appliedScaleZ);
            }

            Gizmo.GizmoScale peekedScale = gizmo.peekScale();
            if (peekedScale != null) {
               float scaleX = peekedScale.getScaleX() * appliedScaleX;
               float scaleY = peekedScale.getScaleY() * appliedScaleY;
               float scaleZ = peekedScale.getScaleZ() * appliedScaleZ;
               if (scaleX != lastScaleX || scaleY != lastScaleY || scaleZ != lastScaleZ) {
                  lastScaleX = scaleX;
                  lastScaleY = scaleY;
                  lastScaleZ = scaleZ;
                  scaledSelectionRegion = createScaled(scaleX, scaleY, scaleZ);
               }
            }

            if (scaledSelectionRegion != null) {
               Vec3 translation = gizmo.getInterpPosition();
               translation = translation.subtract(originalSelectionCenter);
               scaledSelectionRegion.render(rc, translation, 7);
            } else {
               Vec3 translation = gizmo.getInterpPosition();
               translation = translation.subtract(originalSelectionCenter);
               selectionRegion.render(rc, translation, 7);
            }

            if (gizmo.isGrabbed() || showGizmo) {
               gizmo.render(rc, isCtrlDown);
            }
         } else {
            finish();
         }
      }
   }

   public static ChunkedBooleanRegion createScaled(float scaleX, float scaleY, float scaleZ) {
      if (!(Math.abs(scaleX) < 1.0E-7) && !(Math.abs(scaleY) < 1.0E-7) && !(Math.abs(scaleZ) < 1.0E-7)) {
         BlockPos min = selectionRegion.min();
         BlockPos max = selectionRegion.max();
         double centerX = originalSelectionCenter.x;
         double centerY = originalSelectionCenter.y;
         double centerZ = originalSelectionCenter.z;
         int minX = (int)Math.floor((min.getX() - centerX) * scaleX + centerX) - 1;
         int minY = (int)Math.floor((min.getY() - centerY) * scaleY + centerY) - 1;
         int minZ = (int)Math.floor((min.getZ() - centerZ) * scaleZ + centerZ) - 1;
         int maxX = (int)Math.ceil((max.getX() + 1 - centerX) * scaleX + centerX) + 1;
         int maxY = (int)Math.ceil((max.getY() + 1 - centerY) * scaleY + centerY) + 1;
         int maxZ = (int)Math.ceil((max.getZ() + 1 - centerZ) * scaleZ + centerZ) + 1;
         ChunkedBooleanRegion region = new ChunkedBooleanRegion();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  int newX = (int)Math.floor((x + 0.5F - centerX) / scaleX + centerX);
                  int newY = (int)Math.floor((y + 0.5F - centerY) / scaleY + centerY);
                  int newZ = (int)Math.floor((z + 0.5F - centerZ) / scaleZ + centerZ);
                  if (selectionRegion.contains(newX, newY, newZ)) {
                     region.add(x, y, z);
                  }
               }
            }
         }

         return region;
      } else {
         return new ChunkedBooleanRegion();
      }
   }

   public static boolean isModifyingSelection() {
      return gizmo != null;
   }
}
