package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.utils.BlockCondition;
import java.util.List;

public class BlockBelowConditionMaskElement implements MaskElement, GenericBlockConditionMaskElement {
   private final BlockCondition blockCondition;
   private final List<BlockConditionWidget.BlockConditionState> blockConditionStates;

   public BlockBelowConditionMaskElement(BlockCondition blockCondition, List<BlockConditionWidget.BlockConditionState> blockConditionStates) {
      this.blockCondition = blockCondition;
      this.blockConditionStates = blockConditionStates;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return this.blockCondition.matches(context.getBlockState(x, y, z, 0, -1, 0));
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
      return "below";
   }
}
