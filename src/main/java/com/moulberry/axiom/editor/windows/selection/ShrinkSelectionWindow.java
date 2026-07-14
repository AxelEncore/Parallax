package com.moulberry.axiom.editor.windows.selection;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.ExpandSelection;
import imgui.moulberry92.ImGui;

public class ShrinkSelectionWindow {
   private static final int[] shrinkAmount = new int[]{2};

   public static void render() {
      if (EditorWindowType.SHRINK_SELECTION.isOpen()) {
         if (EditorWindowType.SHRINK_SELECTION.begin("###ShrinkSelection", true)) {
            boolean docked = ImGui.isWindowDocked();
            if (!docked) {
               ImGui.text(AxiomI18n.get("axiom.editorui.window.shrink_selection.shrink_selection_by"));
            }

            ImGuiHelper.inputInt("##ShrinkAmount", shrinkAmount);
            if (!docked && ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
               EditorWindowType.SHRINK_SELECTION.setOpen(false);
            }

            ImGui.sameLine();
            boolean disable = Selection.getSelectionBuffer().isEmpty() | shrinkAmount[0] == 0;
            if (disable) {
               ImGui.beginDisabled();
            }

            String shrinkText = shrinkAmount[0] < 0
               ? AxiomI18n.get("axiom.editorui.window.expand_selection.do_expand")
               : AxiomI18n.get("axiom.editorui.window.shrink_selection.do_shrink");
            if (ImGui.button(shrinkText + "###ShrinkButton")) {
               ExpandSelection.shrink(shrinkAmount[0]);
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.SHRINK_SELECTION.end();
      }
   }
}
