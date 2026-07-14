package com.moulberry.axiom.editor.windows;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.editor.ImGuiHelper;
import imgui.moulberry92.ImGui;

public class BrushStudioWindow {
   public static void render() {
      if (ImGui.begin("Brush Studio")) {
         if (ImGui.beginChild("Category", 175.0F, -1.0F, true)) {
            ImGui.selectable(AxiomI18n.get("axiom.hardcoded.shape"), true);
            ImGui.selectable(AxiomI18n.get("axiom.hardcoded.texture"));
            ImGui.selectable(AxiomI18n.get("axiom.hardcoded.taper"));
            ImGui.selectable(AxiomI18n.get("axiom.hardcoded.dynamics"));
         }

         ImGui.endChild();
         ImGui.sameLine();
         if (ImGui.beginChild("Properties", -175.0F, -1.0F, true)) {
            ImGuiHelper.combo("##Shape", new int[]{0}, new String[]{"Sphere", "Cube", "Octahedron", "Cylinder", "Capsule"});
            ImGui.text("1");
            ImGui.text("2");
            ImGui.text("3");
         }

         ImGui.endChild();
         ImGui.sameLine();
         if (ImGui.beginChild("Preview", -1.0F, -1.0F, true)) {
            ImGui.text("1");
            ImGui.text("2");
            ImGui.text("3");
         }

         ImGui.endChild();
      }

      ImGui.end();
   }
}
