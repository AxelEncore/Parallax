package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.AxiomServerboundTickBlocks;
import com.moulberry.axiom.packets.SupportedProtocol;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class TickBlocksModal {
   private static boolean open = false;

   public static void render() {
      if (open) {
         open = false;
         ImGui.openPopup("###TickBlocksModal");
      }

      String title = AxiomI18n.get("axiom.editorui.mainmenu.operations.trigger_updates_and_tick");
      if (ImGuiHelper.beginPopupModalCloseable(title + "###TickBlocksModal", 64)) {
         ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0F);
         ImGui.textColored(-12566273, AxiomI18n.get("axiom.hardcoded.op_cannot_be_undone"));
         ImGui.textWrapped(AxiomI18n.get("axiom.hardcoded.op_side_effects_warn"));
         ImGui.textWrapped(AxiomI18n.get("axiom.hardcoded.op_large_area_warn"));
         ImGui.textWrapped(AxiomI18n.get("axiom.hardcoded.backup_before_op"));
         ImGui.textColored(-12566273, AxiomI18n.get("axiom.hardcoded.you_have_been_warned"));
         ImGui.popTextWrapPos();
         ClientLevel level = Minecraft.getInstance().level;
         boolean supported = ClientEvents.serverSupportsProtocol(SupportedProtocol.TICK_BLOCKS);
         boolean disabled = Selection.getSelectionBuffer().isEmpty() || level == null || !supported;
         if (disabled) {
            ImGui.beginDisabled();
         }

         if (ImGui.button(title)) {
            SelectionBuffer buffer = Selection.getSelectionBuffer();
            if (buffer instanceof SelectionBuffer.AABB aabb) {
               new AxiomServerboundTickBlocks(level.dimension(), aabb.min(), aabb.max()).send();
            } else if (buffer instanceof SelectionBuffer.Set set) {
               new AxiomServerboundTickBlocks(level.dimension(), set.selectionRegion.unsafeGetPositionSet()).send();
            }

            ImGui.closeCurrentPopup();
            Selection.clearSelection();
         }

         if (disabled) {
            ImGui.endDisabled();
            if (!supported) {
               ImGuiHelper.tooltip("Server doesn't support axiom:tick_blocks", 1024);
            } else {
               ImGuiHelper.tooltip("No active selection", 1024);
            }
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   public static void open() {
      open = true;
   }
}
