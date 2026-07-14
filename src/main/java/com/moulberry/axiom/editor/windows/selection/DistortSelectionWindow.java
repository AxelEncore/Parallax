package com.moulberry.axiom.editor.windows.selection;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.DistortSelection;
import imgui.moulberry92.ImGui;

public class DistortSelectionWindow {
   private static final float[] noiseRadius = new float[]{5.0F};
   private static final float[] stddev = new float[]{2.0F};

   public static void render() {
      if (EditorWindowType.DISTORT_SELECTION.isOpen()) {
         if (EditorWindowType.DISTORT_SELECTION.begin("###DistortSelection", true)) {
            boolean docked = ImGui.isWindowDocked();
            if (docked) {
               ImGuiHelper.inputFloat(AxiomI18n.get("axiom.editorui.window.distort_selection.noise_radius") + "##NoiseRadius", noiseRadius);
               ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.window.distort_selection.distance") + "##Distance", stddev, 1.0F, 10.0F);
            } else {
               ImGui.text(AxiomI18n.get("axiom.editorui.window.distort_selection.noise_radius"));
               ImGuiHelper.inputFloat("##NoiseRadius", noiseRadius);
               ImGui.text(AxiomI18n.get("axiom.editorui.window.distort_selection.distance"));
               ImGui.sliderFloat("##Distance", stddev, 1.0F, 10.0F);
            }

            if (!docked) {
               if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
                  EditorWindowType.DISTORT_SELECTION.setOpen(false);
               }

               ImGui.sameLine();
            }

            boolean disable = Selection.getSelectionBuffer().isEmpty() | noiseRadius[0] <= 0.0F;
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.distort_selection.do_distort") + "###DistortButton")) {
               DistortSelection.distort(noiseRadius[0], stddev[0]);
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.DISTORT_SELECTION.end();
      }
   }
}
