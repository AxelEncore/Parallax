package com.moulberry.axiom.editor.tutorial;

import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;

public enum TutorialStage {
   CLIPBOARD("axiom.tutorial.clipboard", EditorWindowType.CLIPBOARD, null),
   HISTORY("axiom.tutorial.history", EditorWindowType.HISTORY, CLIPBOARD),
   MAIN_WINDOW("axiom.tutorial.main_window", null, HISTORY),
   ACTIVE_BLOCK("axiom.tutorial.active_block", EditorWindowType.ACTIVE_BLOCK, MAIN_WINDOW),
   TOOL_OPTIONS("axiom.tutorial.tool_options", EditorWindowType.TOOL_OPTIONS, ACTIVE_BLOCK),
   TOOLS_WINDOW("axiom.tutorial.tools_window", EditorWindowType.TOOLS, TOOL_OPTIONS),
   SELECTION2("axiom.tutorial.selection2", null, null),
   SELECTION1("axiom.tutorial.selection1", null, SELECTION2),
   MAGIC_SELECT_TOOL("axiom.tutorial.magic_select_tool", EditorWindowType.TOOL_OPTIONS, null),
   BOX_SELECT_TOOL2("axiom.tutorial.box_select_tool2", EditorWindowType.TOOL_OPTIONS, null),
   BOX_SELECT_TOOL1("axiom.tutorial.box_select_tool1", EditorWindowType.TOOL_OPTIONS, BOX_SELECT_TOOL2),
   SLOPE_TOOL2("axiom.tutorial.slope_tool2", EditorWindowType.TOOL_OPTIONS, null),
   SLOPE_TOOL1("axiom.tutorial.slope_tool1", EditorWindowType.TOOL_OPTIONS, SLOPE_TOOL2),
   SCULPT_DRAW_TOOL("axiom.tutorial.sculpt_draw_tool", EditorWindowType.TOOL_OPTIONS, null);

   private final String translationKey;
   private final EditorWindowType linkedWindow;
   private final TutorialStage nextStage;
   Tutorial tutorial = null;
   int stageIndex;

   private TutorialStage(String translationKey, EditorWindowType linkedWindow, TutorialStage nextStage) {
      this.translationKey = translationKey;
      this.linkedWindow = linkedWindow;
      this.nextStage = nextStage;
   }

   public EditorWindowType getLinkedWindow() {
      return this.linkedWindow;
   }

   public TutorialStage getNextStage() {
      return this.nextStage;
   }

   public Tutorial getTutorial() {
      return this.tutorial;
   }

   public void render(ImVec2 windowPos, ImVec2 windowSize) {
      float mainSizeX = ImGui.getMainViewport().getSizeX();
      boolean overlap = false;
      if (windowPos.x + windowSize.x / 2.0F > mainSizeX / 2.0F) {
         float x = windowPos.x;
         if (x < 350.0F) {
            x = 350.0F;
            overlap = true;
         }

         ImGui.setNextWindowPos(x, windowPos.y + windowSize.y / 2.0F, 1, 1.0F, 0.5F);
      } else {
         float x = windowPos.x + windowSize.x;
         if (x > mainSizeX - 350.0F) {
            x = mainSizeX - 350.0F;
            overlap = true;
         }

         ImGui.setNextWindowPos(x, windowPos.y + windowSize.y / 2.0F, 1, 0.0F, 0.5F);
      }

      if (overlap) {
         ImGui.setNextWindowFocus();
      }

      this.render();
   }

   public void render() {
      ImGui.setNextWindowSize(350.0F, 0.0F);
      ImGuiHelper.pushStyleVar(4, 2.0F);
      ImGuiHelper.pushStyleColor(5, -16776961);
      if (ImGui.begin("##TutorialPopup" + this.ordinal(), 528747)) {
         ImGui.pushTextWrapPos();
         if (this == MAIN_WINDOW) {
            ImGui.text(
               AxiomI18n.get(
                  this.translationKey,
                  Keybinds.ROTATE_CAMERA.longKeyIdentifier(),
                  Keybinds.USE_TOOL.longKeyIdentifier(),
                  Keybinds.ARCBALL_CAMERA.longKeyIdentifier()
               )
            );
         } else if (this == SELECTION2) {
            ImGui.text(
               AxiomI18n.get(
                  this.translationKey,
                  Keybinds.QUICK_FILL.longKeyIdentifier(),
                  Keybinds.QUICK_REPLACE.longKeyIdentifier(),
                  Keybinds.COPY.longKeyIdentifier(),
                  Keybinds.CUT.longKeyIdentifier()
               )
            );
         } else {
            ImGui.text(AxiomI18n.get(this.translationKey));
         }

         ImGui.popTextWrapPos();
         if (ImGui.button(AxiomI18n.get("axiom.tutorial.continue") + " (" + (this.stageIndex + 1) + "/" + this.getTutorial().stageCount() + ")")) {
            TutorialManager.nextTutorialStage();
         }
      }

      ImGui.end();
      ImGuiHelper.popStyleColor();
      ImGuiHelper.popStyleVar();
   }
}
