package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;

public class ToolOptionsWindow {
   public static void render() {
      boolean showTutorial = TutorialManager.getCurrentStageLinkedWindow() == EditorWindowType.TOOL_OPTIONS;
      if (EditorWindowType.TOOL_OPTIONS.isOpen() || showTutorial) {
         ImGuiViewport viewport = ImGui.getMainViewport();
         ImGui.setNextWindowSize(200.0F, viewport.getSizeY() / 3.0F, 4);
         ImGui.setNextWindowPos(viewport.getPosX() + 20.0F, viewport.getPosY() + viewport.getSizeY() * 5.0F / 9.0F, 4);
         if (EditorWindowType.TOOL_OPTIONS.begin("###Tool Options", true)) {
            ImGui.setScrollX(0.0F);
            if (ToolManager.isToolActive()) {
               Tool tool = ToolManager.getCurrentTool();
               Tutorial tutorial = tool.getTutorial();
               if (tutorial != null) {
                  tutorial.initiateIfNotCompleted();
               }

               ImGui.pushID(ToolManager.getCurrentToolIndex());
               tool.displayImguiOptions();
               ImGui.popID();
            } else {
               ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.tool_options.no_active_tool"));
            }

            if (showTutorial) {
               TutorialManager.getCurrentStage().render(ImGui.getWindowPos(), ImGui.getWindowSize());
            }
         } else if (showTutorial) {
            TutorialManager.nextTutorialStage();
         }

         EditorWindowType.TOOL_OPTIONS.end();
      }
   }
}
