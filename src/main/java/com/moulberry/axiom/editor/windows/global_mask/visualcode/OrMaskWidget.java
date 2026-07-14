package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.i18n.AxiomI18n;

public class OrMaskWidget extends MaskWidgetWithChildren {
   public OrMaskWidget() {
      super(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_or"), 0);
   }

   @Override
   public MaskWidget copy() {
      OrMaskWidget widget = new OrMaskWidget();

      for (MaskWidget child : this.children) {
         widget.addChild(-1, child.copy());
      }

      return widget;
   }
}
