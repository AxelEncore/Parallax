package com.moulberry.axiom.tools.modelling;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.gizmo.ExtrudedGizmo;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.world_modification.HistoryBuffer;
import com.moulberry.axiom.world_modification.HistoryEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class GizmoList {
   private final List<Gizmo> gizmos = new ArrayList<>();
   private final HistoryBuffer<GizmoList.GizmoListHistoryAction> history = new HistoryBuffer<>();
   private boolean renderGizmos = false;
   private boolean hasGrabbedGizmo = false;
   private boolean isCtrlDown = false;
   private int activeGizmo = -1;
   private boolean pendingDeselect = false;

   public HistoryBuffer<GizmoList.GizmoListHistoryAction> getHistoryBuffer() {
      return this.history;
   }

   private void pushHistory(
      GizmoList.GizmoListHistoryAction backwards, GizmoList.GizmoListHistoryAction forwards, BlockPos center, String description, boolean apply
   ) {
      HistoryEntry<GizmoList.GizmoListHistoryAction> entry = new HistoryEntry<>(forwards, backwards, center, description, 0);
      this.history
         .pushOrMergeIf(
            entry,
            (last, next) -> last.forwards() instanceof GizmoList.MoveGizmo forwardsMoveGizmo
                  && next.backwards() instanceof GizmoList.MoveGizmo backwardsMoveGizmo
               ? backwardsMoveGizmo.index == forwardsMoveGizmo.index
               : false
         );
      if (apply) {
         forwards.apply(this);
      }
   }

   public List<Gizmo> getGizmos() {
      return this.gizmos;
   }

   public int size() {
      return this.gizmos.size();
   }

   @Nullable
   public ExtrudedGizmo extrude() {
      if (this.activeGizmo >= 0) {
         Gizmo gizmo = this.gizmos.get(this.activeGizmo);
         return new ExtrudedGizmo(Minecraft.getInstance().player, gizmo);
      } else {
         return null;
      }
   }

   public void finishExtrude(ExtrudedGizmo extrudedGizmo, Vec3 lookDirection) {
      if (lookDirection != null) {
         int addIndex = this.gizmos.size();

         for (int i = 0; i < this.gizmos.size(); i++) {
            Gizmo gizmo = this.gizmos.get(i);
            if (gizmo == extrudedGizmo.extrudeGizmoFrom) {
               addIndex = i + 1;
            }
         }

         BlockPos pos = extrudedGizmo.getBlockPos(lookDirection);
         if (pos != null) {
            this.pushHistory(
               new GizmoList.RemoveGizmo(addIndex), new GizmoList.AddGizmo(addIndex, Vec3.atCenterOf(pos)), pos, "Add Gizmo " + (addIndex + 1), true
            );
         }
      }
   }

   public boolean handleScroll(int xScroll, int yScroll) {
      for (int i = 0; i < this.gizmos.size(); i++) {
         Gizmo gizmo = this.gizmos.get(i);
         if (gizmo.isCenterGrabbed()) {
            Vec3 look = Tool.getLookDirection();
            Vec3 oldTarget = gizmo.getTargetVec();
            gizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), look);
            Vec3 newTarget = gizmo.getTargetVec();
            if (!oldTarget.equals(newTarget)) {
               this.pushHistory(
                  new GizmoList.MoveGizmo(i, oldTarget), new GizmoList.MoveGizmo(i, newTarget), BlockPos.containing(newTarget), "Move Gizmo " + (i + 1), false
               );
            }

            return true;
         }
      }

      return false;
   }

   public boolean hasActiveGizmo() {
      return this.activeGizmo >= 0;
   }

   public int getActiveGizmoIndex() {
      return this.activeGizmo;
   }

   public boolean hasGrabbedGizmo() {
      return this.hasGrabbedGizmo;
   }

   public boolean isEmpty() {
      return this.gizmos.isEmpty();
   }

   public void deselectActiveGizmo() {
      this.setActiveGizmo(-1);
   }

   public boolean undo() {
      HistoryEntry<GizmoList.GizmoListHistoryAction> entry = this.history.undo(0);
      if (entry != null) {
         entry.backwards().apply(this);
         return true;
      } else {
         return false;
      }
   }

   public boolean redo() {
      HistoryEntry<GizmoList.GizmoListHistoryAction> entry = this.history.redo(0);
      if (entry != null) {
         entry.forwards().apply(this);
         return true;
      } else {
         return false;
      }
   }

   public void delete() {
      if (this.activeGizmo >= 0) {
         Gizmo gizmo = this.gizmos.get(this.activeGizmo);
         this.pushHistory(
            new GizmoList.AddGizmo(this.activeGizmo, gizmo.getTargetVec()),
            new GizmoList.RemoveGizmo(this.activeGizmo),
            gizmo.getTargetPosition(),
            "Delete Gizmo " + (this.activeGizmo + 1),
            true
         );
      } else {
         this.clear();
      }
   }

   public void clear() {
      if (!this.gizmos.isEmpty()) {
         List<Vec3> positions = new ArrayList<>();

         for (Gizmo gizmo : this.gizmos) {
            positions.add(gizmo.getTargetVec());
         }

         this.pushHistory(
            new GizmoList.SetGizmos(positions), new GizmoList.ClearGizmos(), BlockPos.containing((Position)positions.get(0)), "Clear Gizmos", true
         );
      }
   }

   public void setActiveGizmo(int index) {
      this.pendingDeselect = false;
      if (this.activeGizmo >= 0 && this.activeGizmo < this.gizmos.size()) {
         this.gizmos.get(this.activeGizmo).enableAxes = false;
      }

      if (index >= 0 && index < this.gizmos.size()) {
         this.gizmos.get(index).enableAxes = true;
         this.activeGizmo = index;
      } else {
         this.activeGizmo = -1;
      }
   }

   public void addGizmo(Vec3 position) {
      int addIndex;
      if (this.activeGizmo >= 0) {
         addIndex = this.activeGizmo + 1;
      } else {
         addIndex = this.gizmos.size();
      }

      this.pushHistory(
         new GizmoList.RemoveGizmo(addIndex), new GizmoList.AddGizmo(addIndex, position), BlockPos.containing(position), "Add Gizmo " + (addIndex + 1), true
      );
   }

   public boolean leftClick() {
      if (this.activeGizmo >= 0) {
         Gizmo gizmo = this.gizmos.get(this.activeGizmo);
         if (gizmo.leftClick()) {
            return true;
         }
      }

      for (int i = 0; i < this.gizmos.size(); i++) {
         if (i != this.activeGizmo) {
            Gizmo gizmo = this.gizmos.get(i);
            if (gizmo.leftClick()) {
               this.setActiveGizmo(i);
               return true;
            }
         }
      }

      this.pendingDeselect = true;
      return false;
   }

   public boolean updateGizmos(AxiomWorldRenderContext rc) {
      if (this.pendingDeselect) {
         this.pendingDeselect = false;
         this.setActiveGizmo(-1);
      }

      Vec3 lookDirection = Tool.getLookDirection();
      boolean isLeftDown = Tool.isMouseDown(0);
      this.isCtrlDown = EditorUI.isCtrlOrCmdDown();
      boolean showGizmo = !EditorUI.isActive() || !this.isCtrlDown;
      this.renderGizmos = showGizmo;
      boolean positionChanged = false;
      this.hasGrabbedGizmo = false;

      for (int i = 0; i < this.gizmos.size(); i++) {
         Gizmo gizmo = this.gizmos.get(i);
         Vec3 before = gizmo.getTargetVec();
         gizmo.update(rc.nanos(), lookDirection, isLeftDown, this.isCtrlDown, showGizmo);
         gizmo.setAxisDirections(
            rc.x() > gizmo.getTargetPosition().getX(), rc.y() > gizmo.getTargetPosition().getY(), rc.z() > gizmo.getTargetPosition().getZ()
         );
         if (gizmo.isGrabbed()) {
            this.renderGizmos = true;
            this.hasGrabbedGizmo = true;
         }

         Vec3 after = gizmo.getTargetVec();
         if (!before.equals(after)) {
            positionChanged = true;
            this.pushHistory(new GizmoList.MoveGizmo(i, before), new GizmoList.MoveGizmo(i, after), BlockPos.containing(after), "Move Gizmo " + (i + 1), false);
         }
      }

      return positionChanged;
   }

   public void renderGizmos(AxiomWorldRenderContext rc) {
      if (this.renderGizmos) {
         for (Gizmo gizmo : this.gizmos) {
            gizmo.render(rc, this.isCtrlDown);
         }
      }
   }

   public record AddGizmo(int index, Vec3 position) implements GizmoList.GizmoListHistoryAction {
      @Override
      public void apply(GizmoList gizmoList) {
         Gizmo gizmo = new Gizmo(this.position);
         gizmo.enableAxes = false;
         gizmoList.gizmos.add(this.index, gizmo);
         gizmoList.setActiveGizmo(this.index);
      }
   }

   public record ClearGizmos() implements GizmoList.GizmoListHistoryAction {
      @Override
      public void apply(GizmoList gizmoList) {
         gizmoList.setActiveGizmo(-1);
         gizmoList.gizmos.clear();
      }
   }

   public interface GizmoListHistoryAction {
      void apply(GizmoList var1);
   }

   public record MoveGizmo(int index, Vec3 newPosition) implements GizmoList.GizmoListHistoryAction {
      @Override
      public void apply(GizmoList gizmoList) {
         gizmoList.gizmos.get(this.index).moveToVecInstantly(this.newPosition);
      }
   }

   public record RemoveGizmo(int index) implements GizmoList.GizmoListHistoryAction {
      @Override
      public void apply(GizmoList gizmoList) {
         gizmoList.gizmos.remove(this.index);
         if (gizmoList.activeGizmo >= 0) {
            if (gizmoList.activeGizmo == this.index) {
               gizmoList.setActiveGizmo(-1);
            } else if (gizmoList.activeGizmo > this.index) {
               gizmoList.setActiveGizmo(gizmoList.activeGizmo - 1);
            }
         }
      }
   }

   public record SetGizmos(List<Vec3> positions) implements GizmoList.GizmoListHistoryAction {
      @Override
      public void apply(GizmoList gizmoList) {
         gizmoList.setActiveGizmo(-1);
         gizmoList.gizmos.clear();

         for (Vec3 position : this.positions) {
            Gizmo gizmo = new Gizmo(position);
            gizmo.enableAxes = false;
            gizmoList.gizmos.add(gizmo);
         }
      }
   }
}
