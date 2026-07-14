package com.moulberry.axiom.editor.windows.selection;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.DistortSelection;
import imgui.moulberry92.ImGui;

public class SmoothSelectionWindow {
   private static final float[] stddev = new float[]{2.0F};
   private static final float[] threshold = new float[]{0.5F};

   public static void render() {
      if (EditorWindowType.SMOOTH_SELECTION.isOpen()) {
         if (EditorWindowType.SMOOTH_SELECTION.begin("###SmoothSelection", true)) {
            boolean docked = ImGui.isWindowDocked();
            if (docked) {
               ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.window.smooth_selection.stddev") + "##StdDev", stddev, 1.0F, 10.0F);
               ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.window.smooth_selection.threshold") + "##Threshold", threshold, 0.0F, 1.0F);
            } else {
               ImGui.text(AxiomI18n.get("axiom.editorui.window.smooth_selection.stddev"));
               ImGui.sliderFloat("##StdDev", stddev, 1.0F, 10.0F);
               ImGui.text(AxiomI18n.get("axiom.editorui.window.smooth_selection.threshold"));
               ImGui.sliderFloat("##Threshold", threshold, 0.0F, 1.0F);
            }

            if (!docked) {
               if (!ImGui.isWindowDocked() && ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
                  EditorWindowType.SMOOTH_SELECTION.setOpen(false);
               }

               ImGui.sameLine();
            }

            boolean disable = Selection.getSelectionBuffer().isEmpty();
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.smooth_selection.do_smooth") + "###SmoothButton")) {
               DistortSelection.smooth(stddev[0], threshold[0]);
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.SMOOTH_SELECTION.end();
      }
   }
}
