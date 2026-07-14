package com.moulberry.axiom.clipboard;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import net.minecraft.core.BlockPos;

public interface SelectionHistoryElement extends AdditionalUndoOperation {
   SelectionHistoryElement.Empty EMPTY = new SelectionHistoryElement.Empty();

   void applyToSelection();

   @Override
   default void perform() {
      this.applyToSelection();
   }

   public record AABB(BlockPos min, BlockPos max) implements SelectionHistoryElement {
      @Override
      public void applyToSelection() {
         Selection.clearSelectionNoHistory();
         Selection.addAABBWithHistory(this.min, this.max, false);
      }
   }

   public static class Empty implements SelectionHistoryElement {
      @Override
      public void applyToSelection() {
         Selection.clearSelectionNoHistory();
      }
   }

   public record Set(PositionSet blocks) implements SelectionHistoryElement {
      @Override
      public void applyToSelection() {
         Selection.clearSelectionNoHistory();
         Selection.modifyWithHistory(region -> {
            region.addAll(this.blocks);
            return region;
         }, false);
      }
   }
}
