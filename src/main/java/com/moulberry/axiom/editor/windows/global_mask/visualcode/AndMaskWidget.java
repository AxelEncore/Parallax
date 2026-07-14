package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.i18n.AxiomI18n;

public class AndMaskWidget extends MaskWidgetWithChildren {
   public AndMaskWidget() {
      super(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_and"), 0);
   }

   @Override
   public MaskWidget copy() {
      AndMaskWidget widget = new AndMaskWidget();

      for (MaskWidget child : this.children) {
         widget.addChild(-1, child.copy());
      }

      return widget;
   }
}
