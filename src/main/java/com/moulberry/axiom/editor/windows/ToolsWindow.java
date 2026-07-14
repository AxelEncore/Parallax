package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.editor.ButtonSpacingSpec;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.tutorial.TutorialStage;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import imgui.moulberry92.ImFont;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;

public class ToolsWindow {
   public static void render(ImFont icons) {
      boolean showTutorial = TutorialManager.getCurrentStage() == TutorialStage.TOOLS_WINDOW;
      if (EditorWindowType.TOOLS.isOpen() || showTutorial) {
         ImGuiViewport viewport = ImGui.getMainViewport();
         ImGui.setNextWindowSize(200.0F, viewport.getSizeY() / 3.0F, 4);
         ImGui.setNextWindowPos(viewport.getPosX() + 20.0F, viewport.getPosY() + viewport.getSizeY() / 9.0F, 4);
         if (EditorWindowType.TOOLS.begin("###Tools", true)) {
            float available = ImGui.getWindowSizeX();
            float toolButtonSizeX = (32.0F + ImGui.getStyle().getFramePaddingX() * 2.0F) * EditorUI.getUiScale();
            float toolButtonSizeY = (32.0F + ImGui.getStyle().getFramePaddingY() * 2.0F) * EditorUI.getUiScale();
            ButtonSpacingSpec spec = ButtonSpacingSpec.calculate(available, toolButtonSizeX, ToolManager.getTools().size(), 8.0F, true);
            boolean removeBackground = spec.buttonsPerRow() == 1 && spec.spacingX() <= 1.0F;
            int buttonsInRow = 0;
            float currentX = spec.spacingX();
            float currentY = ImGui.getCursorPosY() - ImGui.getStyle().getWindowPaddingY() + spec.spacingY();
            int buttonHoveredCol = ImGui.getColorU32(22);
            int buttonActiveCol = ImGui.getColorU32(23);
            ImGui.pushFont(icons, icons.getLegacySize());
            if (removeBackground) {
               ImGuiHelper.pushStyleColor(21, 0);
               ImGuiHelper.pushStyleColor(22, 0);
               ImGuiHelper.pushStyleColor(23, 0);
            }

            int activeTool = ToolManager.isToolActive() ? ToolManager.getCurrentToolIndex() : -1;

            for (int i = 0; i < ToolManager.getTools().size(); i++) {
               ImGui.setCursorPos(currentX, currentY);
               if (!removeBackground && i == activeTool) {
                  ImGuiHelper.pushStyleColor(21, buttonActiveCol);
               }

               Tool tool = ToolManager.getTools().get(i);
               char icon = tool.iconChar();
               boolean disabled = !AxiomClient.hasPermissions(tool.requiredPermissions());
               ImGui.pushID(i);
               if (disabled) {
                  ImGui.beginDisabled();
               }

               if (ImGui.button(String.valueOf(icon), toolButtonSizeX, toolButtonSizeY)) {
                  if (i == activeTool) {
                     ToolManager.setToolSelected(false);
                  } else {
                     ToolManager.setCurrentToolIndex(i);
                     ToolManager.setToolSelected(true);
                  }
               }

               if (disabled) {
                  ImGui.endDisabled();
                  ImGui.popFont();
                  ImGuiHelper.tooltip("Server hasn't given you permission to use this tool", 1024);
                  ImGui.pushFont(icons, icons.getLegacySize());
               }

               ImGui.popID();
               if (!removeBackground && i == activeTool) {
                  ImGuiHelper.popStyleColor();
               }

               if (removeBackground && (i == activeTool || ImGui.isItemHovered())) {
                  ImGui.setCursorPos(currentX + ImGui.getStyle().getFramePaddingX(), currentY + ImGui.getStyle().getFramePaddingY());
                  ImGui.textColored(i != activeTool && !ImGui.isItemActive() ? buttonHoveredCol : buttonActiveCol, String.valueOf(icon));
               }

               if (ImGui.isItemHovered()) {
                  ImGui.popFont();
                  String text = tool.name();
                  Keybind keybind = ToolManager.keybindMap.get(tool);
                  if (keybind != null && keybind.getKey() != 0) {
                     text = text + " (" + keybind.longKeyIdentifier() + ")";
                  }

                  ImGui.beginTooltip();
                  ImGui.text(text);
                  ImGui.endTooltip();
                  ImGui.pushFont(icons, icons.getLegacySize());
               }

               if (buttonsInRow + 1 >= spec.buttonsPerRow()) {
                  currentX = spec.spacingX();
                  currentY += toolButtonSizeY + spec.spacingY();
                  buttonsInRow = 0;
               } else {
                  currentX += toolButtonSizeX + spec.spacingX();
                  buttonsInRow++;
               }
            }

            if (removeBackground) {
               ImGuiHelper.popStyleColor(3);
            }

            ImGui.popFont();
            if (showTutorial) {
               TutorialStage.TOOLS_WINDOW.render(ImGui.getWindowPos(), ImGui.getWindowSize());
            }
         } else if (showTutorial) {
            TutorialManager.nextTutorialStage();
         }

         EditorWindowType.TOOLS.end();
      }
   }
}
