package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class SelectedMaskElement implements MaskElement, GenericSingleMaskElement {
   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return Selection.contains(x, y, z);
   }

   @Override
   public String cmdStringName() {
      return "selected";
   }
}
