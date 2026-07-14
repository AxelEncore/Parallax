package com.moulberry.axiom.editor.windows.selection;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.ExpandSelection;
import imgui.moulberry92.ImGui;

public class ExpandSelectionWindow {
   private static final int[] expandAmount = new int[]{2};

   public static void render() {
      if (EditorWindowType.EXPAND_SELECTION.isOpen()) {
         if (EditorWindowType.EXPAND_SELECTION.begin("###ExpandSelection", true)) {
            boolean docked = ImGui.isWindowDocked();
            if (!docked) {
               ImGui.text(AxiomI18n.get("axiom.editorui.window.expand_selection.expand_selection_by"));
            }

            ImGuiHelper.inputInt("##ExpandAmount", expandAmount);
            if (!docked && ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
               EditorWindowType.EXPAND_SELECTION.setOpen(false);
            }

            ImGui.sameLine();
            boolean disable = Selection.getSelectionBuffer().isEmpty() | expandAmount[0] == 0;
            if (disable) {
               ImGui.beginDisabled();
            }

            String expandText = expandAmount[0] < 0
               ? AxiomI18n.get("axiom.editorui.window.shrink_selection.do_shrink")
               : AxiomI18n.get("axiom.editorui.window.expand_selection.do_expand");
            if (ImGui.button(expandText + "###ExpandButton")) {
               ExpandSelection.expand(expandAmount[0]);
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.EXPAND_SELECTION.end();
      }
   }
}
