package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.utils.BlockCondition;
import java.util.List;

public interface GenericBlockConditionMaskElement {
   String cmdStringName();

   BlockCondition getBlockCondition();

   List<BlockConditionWidget.BlockConditionState> getConditionStates();

   default boolean canCombine(GenericBlockConditionMaskElement other) {
      return this.getClass() == other.getClass();
   }
}
