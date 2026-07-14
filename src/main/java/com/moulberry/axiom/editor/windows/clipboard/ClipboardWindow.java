package com.moulberry.axiom.editor.windows.clipboard;

import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.tutorial.TutorialStage;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;
import java.text.NumberFormat;
import net.minecraft.core.Direction.Axis;

public class ClipboardWindow {
   public static void render() {
      boolean openBlueprintBrowser = false;
      boolean openCtxMenu = false;
      boolean showTutorial = TutorialManager.getCurrentStage() == TutorialStage.CLIPBOARD;
      if (EditorWindowType.CLIPBOARD.isOpen() || showTutorial) {
         ImGuiViewport viewport = ImGui.getMainViewport();
         ImGui.setNextWindowSize(200.0F, viewport.getSizeY() / 3.0F, 4);
         ImGui.setNextWindowPos(viewport.getPosX() + 20.0F, viewport.getPosY() + viewport.getSizeY() / 9.0F, 4);
         if (EditorWindowType.CLIPBOARD.begin("###Clipboard", true)) {
            ClipboardObject clipboard = Clipboard.INSTANCE.getClipboard();
            int buttonSize = (int)(64.0F * EditorUI.getUiScale());
            if (clipboard == null) {
               openBlueprintBrowser = ImGui.button(AxiomI18n.get("axiom.editorui.window.clipboard.empty"), buttonSize, buttonSize);
               openCtxMenu = ImGui.isItemClicked(1);
               ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.clipboard.copy_hint", Keybinds.COPY.shortKeyIdentifier()));
            } else {
               int thumbnailTextureId = clipboard.thumbnailTextureId();
               int imageSize = (int)(96.0F * EditorUI.getUiScale());
               if (thumbnailTextureId == -1) {
                  openBlueprintBrowser = ImGui.button("##TooBig", imageSize, imageSize);
               } else {
                  ImGui.pushStyleVar(11, 2.0F, 2.0F);
                  openBlueprintBrowser = ImGui.imageButton("##ClipboardThumbnail", clipboard.thumbnailTextureId(), imageSize, imageSize, 0.0F, 1.0F, 1.0F, 0.0F);
                  ImGui.popStyleVar();
               }

               openCtxMenu = ImGui.isItemClicked(1);
               ImGui.sameLine();
               ImGui.beginGroup();
               ImGui.text(clipboard.name().isBlank() ? AxiomI18n.get("axiom.editorui.window.blueprint_browser.unnamed_blueprint") : clipboard.name());
               int blockCount = clipboard.blockRegion().count();
               int blockEntityCount = clipboard.blockEntities().size();
               int entityCount = clipboard.entities().size();
               if (blockCount == 1) {
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.single_block"));
               } else {
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.n_blocks", NumberFormat.getNumberInstance().format((long)blockCount)));
               }

               if (blockEntityCount == 1) {
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.single_block_entity"));
               } else if (blockEntityCount > 1) {
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.n_block_entities", NumberFormat.getNumberInstance().format((long)blockEntityCount)));
               }

               boolean pastingEntities = Placement.INSTANCE.pasteEntities;
               String extra = "";
               if (!pastingEntities) {
                  ImGui.beginDisabled();
                  extra = " (disabled)";
               }

               if (entityCount == 1) {
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.single_entity") + extra);
               } else if (entityCount > 1) {
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.n_entities", NumberFormat.getNumberInstance().format((long)entityCount)) + extra);
               }

               if (!pastingEntities) {
                  ImGui.endDisabled();
               }

               ImGui.endGroup();
            }

            if (!Placement.INSTANCE.isPlacing() && clipboard != null) {
               ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.clipboard.paste_hint", Keybinds.PASTE.shortKeyIdentifier()));
            }

            if (showTutorial) {
               TutorialStage.CLIPBOARD.render(ImGui.getWindowPos(), ImGui.getWindowSize());
            }
         } else if (showTutorial) {
            TutorialManager.nextTutorialStage();
         }

         EditorWindowType.CLIPBOARD.end();
         if (openBlueprintBrowser) {
            BlueprintBrowserWindow.open(null, false);
         }

         if (openCtxMenu) {
            ImGui.openPopup("###ClipboardCtxMenu");
         }

         if (ImGuiHelper.beginPopup("###ClipboardCtxMenu")) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.clipboard.clear"))) {
               Clipboard.INSTANCE.clearClipboard();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.rotate_cw_x"))) {
               Clipboard.INSTANCE.rotate(Axis.X, -1);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.rotate_cw_y"))) {
               Clipboard.INSTANCE.rotate(Axis.Y, -1);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.rotate_cw_z"))) {
               Clipboard.INSTANCE.rotate(Axis.Z, -1);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_x_paren"))) {
               Clipboard.INSTANCE.flip(Axis.X);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_y_paren"))) {
               Clipboard.INSTANCE.flip(Axis.Y);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_z_paren"))) {
               Clipboard.INSTANCE.flip(Axis.Z);
            }

            ImGui.endPopup();
         }
      }
   }
}
