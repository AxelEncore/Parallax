package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.utils.BlockCondition;
import java.util.List;

public class BlockAdjacentConditionMaskElement implements MaskElement, GenericBlockConditionMaskElement {
   private final BlockCondition blockCondition;
   private final List<BlockConditionWidget.BlockConditionState> blockConditionStates;

   public BlockAdjacentConditionMaskElement(BlockCondition blockCondition, List<BlockConditionWidget.BlockConditionState> blockConditionStates) {
      this.blockCondition = blockCondition;
      this.blockConditionStates = blockConditionStates;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 0, 0))) {
         return true;
      } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, 0, 0))) {
         return true;
      } else {
         return this.blockCondition.matches(context.getBlockState(x, y, z, 0, 0, -1))
            ? true
            : this.blockCondition.matches(context.getBlockState(x, y, z, 0, 0, 1));
      }
   }

   @Override
   public BlockCondition getBlockCondition() {
      return this.blockCondition;
   }

   @Override
   public List<BlockConditionWidget.BlockConditionState> getConditionStates() {
      return this.blockConditionStates;
   }

   @Override
   public String cmdStringName() {
      return "adjacent";
   }
}
