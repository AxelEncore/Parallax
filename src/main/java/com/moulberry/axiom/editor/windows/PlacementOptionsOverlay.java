package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.ModifySelection;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.windows.operations.QuickFillWindow;
import com.moulberry.axiom.editor.windows.operations.QuickReplaceWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.tools.ToolManager;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImBoolean;
import net.minecraft.core.Direction.Axis;

public class PlacementOptionsOverlay {
   public static void render(int x, int y) {
      if (ModifySelection.isModifyingSelection()) {
         renderModifySelection(x, y);
      } else if (Placement.INSTANCE.isPlacing()) {
         renderPlacement(x, y);
      } else if (!Selection.getSelectionBuffer().isEmpty()) {
         renderSelection(x, y);
      }
   }

   private static void renderPlacement(int x, int y) {
      ImGui.setNextWindowPos(x - 10, y + 10, 1, 1.0F, 0.0F);
      ImBoolean open = new ImBoolean(true);
      String title = AxiomI18n.get("axiom.editorui.window.clipboard.placement_options");
      if (ImGui.begin(title, open, 528708)) {
         if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.keep_existing"), Placement.INSTANCE.keepExisting)) {
            Placement.INSTANCE.keepExisting = !Placement.INSTANCE.keepExisting;
         }

         ImGui.sameLine();
         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.merge_blocks"), Placement.INSTANCE.prioritizeFullBlocks)) {
            Placement.INSTANCE.prioritizeFullBlocks = !Placement.INSTANCE.prioritizeFullBlocks;
         }

         if (Placement.INSTANCE.keepExisting) {
            ImGuiHelper.tooltip(
               "Merges new blocks with existing blocks based on block shape:\nIf the new block completely overlaps the old block, the new block is used\nOtherwise, the old block is used\n(Inverts if Keep Existing is disabled)"
            );
         } else {
            ImGuiHelper.tooltip(
               "Merges new blocks with existing blocks based on block shape:\nIf the old block completely overlaps the new block, the old block is used\nOtherwise, the new block is used\n(Inverts if Keep Existing is enabled)"
            );
         }

         if (!Placement.INSTANCE.containsAir) {
            ImGui.beginDisabled();
            ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.paste_air"), false);
            ImGui.endDisabled();
            ImGuiHelper.tooltip("Placement does not contain any Air blocks\nIf you wanted to copy air, tick \"Copy Air\" in the selection window", 1024);
         } else if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.paste_air"), Placement.INSTANCE.pasteAir)) {
            Placement.INSTANCE.pasteAir = !Placement.INSTANCE.pasteAir;
         }

         ImGui.sameLine();
         if (Placement.INSTANCE.entities == null || Placement.INSTANCE.entities.isEmpty()) {
            ImGui.beginDisabled();
            ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.paste_entities"), false);
            ImGui.endDisabled();
            ImGuiHelper.tooltip("Placement does not contain any entities\nIf you wanted to copy entities, tick \"Copy Entities\" in the selection window", 1024);
         } else if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.paste_entities"), Placement.INSTANCE.pasteEntities)) {
            Placement.INSTANCE.pasteEntities = !Placement.INSTANCE.pasteEntities;
         }

         if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.unlock_rotation"), Placement.INSTANCE.unlockRotation)) {
            Placement.INSTANCE.unlockRotation = !Placement.INSTANCE.unlockRotation;
            if (Placement.INSTANCE.unlockRotation && Axiom.configuration.internal.showNon90DegreeRotationWarning) {
               ImGui.openPopup("###RotationWarning");
            }
         }

         ImGuiHelper.tooltip("Allows for rotation by angles less than 90 degrees");
         if (ImGuiHelper.beginPopupModal(AxiomI18n.get("axiom.editorui.window.clipboard.warning_title") + "###RotationWarning", 68)) {
            ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.warning_1"));
            ImGui.text(AxiomI18n.get("axiom.editorui.window.clipboard.warning_2"));
            if (ImGui.button(AxiomI18n.get("axiom.editorui.warning_confirmation"))) {
               Axiom.configuration.internal.showNon90DegreeRotationWarning = false;
               ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
         }

         if (EditorWindowType.TOOL_MASKS.isOpen() && MaskManager.hasConfiguredMask()) {
            if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.apply_tool_mask"), Placement.INSTANCE.applyToolMask)) {
               Placement.INSTANCE.applyToolMask = !Placement.INSTANCE.applyToolMask;
            }

            ImGuiHelper.tooltip("Checks the active tool mask when pasting into the world");
         }

         if (ImGui.button(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.rotate_scale"))) {
            RotatePlacementWindow.reset();
            EditorWindowType.ROTATE_PLACEMENT.setOpen(true);
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.flip_ellipsis"))) {
            ImGui.openPopup("##FlipOptions");
         }

         if (ImGui.beginPopup("##FlipOptions")) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_x"))) {
               Placement.INSTANCE.flip(Axis.X);
               ImGui.closeCurrentPopup();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_y"))) {
               Placement.INSTANCE.flip(Axis.Y);
               ImGui.closeCurrentPopup();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_z"))) {
               Placement.INSTANCE.flip(Axis.Z);
               ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
         }

         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.snap_to_ground"))) {
            Placement.INSTANCE.snapToGround();
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.paste_paren") + Keybinds.CONFIRM.longKeyIdentifier() + ")")) {
            Placement.INSTANCE.pastePlacement();
         }

         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.paste_copy"))) {
            Placement.INSTANCE.pastePlacementWithoutStopping(false);
         }

         ImGui.sameLine();
         String pasteAndSelect = AxiomI18n.get("axiom.keybinds.paste_and_select");
         if (Keybinds.PASTE_AND_SELECT.getKey() != 0) {
            pasteAndSelect = pasteAndSelect + " (" + Keybinds.PASTE_AND_SELECT.longKeyIdentifier() + ")";
         }

         if (ImGui.button(pasteAndSelect)) {
            Placement.INSTANCE.pastePlacement(true);
         }
      }

      ImGui.end();
      if (!open.get()) {
         Placement.INSTANCE.stopPlacement();
      }
   }

   private static void renderSelection(int x, int y) {
      ImGui.setNextWindowPos(x - 10, y + 10, 1, 1.0F, 0.0F);
      ImBoolean open = new ImBoolean(true);
      String title = "Selection Options";
      if (ImGui.begin(title, open, 528708)) {
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.clear_selection", Keybinds.CONFIRM.longKeyIdentifier()))) {
            Selection.clearSelection();
         }

         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.move_selection"))) {
            ModifySelection.start();
         }

         ImGui.separator();
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.fill_selection", Keybinds.QUICK_FILL.shortKeyIdentifier()))) {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            QuickFillWindow.openQuickFillWindow = true;
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.replace_selection", Keybinds.QUICK_REPLACE.shortKeyIdentifier()))) {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            QuickReplaceWindow.openQuickReplaceWindow = true;
         }

         ImGui.separator();
         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.copy_air"), Selection.copyWithAir)) {
            Selection.copyWithAir = !Selection.copyWithAir;
         }

         ImGui.sameLine();
         if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_ENTITY)) {
            ImGui.beginDisabled();
            ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.copy_entities"), false);
            ImGui.endDisabled();
            ImGuiHelper.tooltip("Server doesn't support (or doesn't allow) requesting entity data", 1024);
         } else {
            if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.copy_entities"), Selection.copyWithEntities)) {
               Selection.copyWithEntities = !Selection.copyWithEntities;
            }

            ImGuiHelper.tooltip("*** Only includes entities within render distance ***");
         }

         ImGui.separator();
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.copy_selection", Keybinds.COPY.shortKeyIdentifier()))) {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            Selection.callAction(UserAction.COPY, null);
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.cut_selection", Keybinds.CUT.shortKeyIdentifier()))) {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            Selection.callAction(UserAction.CUT, null);
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.duplicate_selection", Keybinds.DUPLICATE.shortKeyIdentifier()))) {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            Selection.callAction(UserAction.DUPLICATE, null);
         }

         ImGui.separator();
         MainMenuBar.renderOperationsImgui(false);
      }

      ImGui.end();
      if (!open.get()) {
         Selection.clearSelection();
      }
   }

   private static void renderModifySelection(int x, int y) {
      ImGui.setNextWindowPos(x - 10, y + 10, 1, 1.0F, 0.0F);
      ImBoolean open = new ImBoolean(true);
      String title = "Move Selection";
      if (ImGui.begin(title, open, 528708)) {
         if (ImGui.button(AxiomI18n.get("axiom.tool.selection.confirm_selection", Keybinds.CONFIRM.longKeyIdentifier()))) {
            ModifySelection.finish();
         }

         ImGui.sameLine();
         ImGui.dummy(25.0F, 0.0F);
         ImGui.separator();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.rotate_y"))) {
            ModifySelection.rotateY();
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.flip_ellipsis"))) {
            ImGui.openPopup("##FlipOptions");
         }

         if (ImGui.beginPopup("##FlipOptions")) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_x"))) {
               ModifySelection.flip(Axis.X);
               ImGui.closeCurrentPopup();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_y"))) {
               ModifySelection.flip(Axis.Y);
               ImGui.closeCurrentPopup();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_z"))) {
               ModifySelection.flip(Axis.Z);
               ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
         }
      }

      ImGui.end();
      if (!open.get()) {
         ModifySelection.cancelAndClearSelection();
      }
   }
}
