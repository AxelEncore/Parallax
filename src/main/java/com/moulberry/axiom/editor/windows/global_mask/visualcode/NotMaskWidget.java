package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.i18n.AxiomI18n;

public class NotMaskWidget extends MaskWidgetWithChildren {
   public NotMaskWidget() {
      super(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_not"), 1);
   }

   @Override
   public MaskWidget copy() {
      NotMaskWidget widget = new NotMaskWidget();

      for (MaskWidget child : this.children) {
         widget.addChild(-1, child.copy());
      }

      return widget;
   }
}
