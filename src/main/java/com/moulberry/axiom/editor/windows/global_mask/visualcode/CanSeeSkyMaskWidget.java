package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;

public class CanSeeSkyMaskWidget extends MaskWidget {
   @Override
   public void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_can_see_sky"));
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
      }
   }

   @Override
   public MaskWidget copy() {
      return new CanSeeSkyMaskWidget();
   }
}
