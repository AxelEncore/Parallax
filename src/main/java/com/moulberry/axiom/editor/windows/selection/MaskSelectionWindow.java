package com.moulberry.axiom.editor.windows.selection;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.MaskSelection;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;

public class MaskSelectionWindow {
   private static final BlockConditionWidget blockConditionWidget = new BlockConditionWidget(Blocks.STONE);
   private static float propertiesWidth = -1.0F;

   public static void render(BlockList blockList) {
      if (EditorWindowType.FILTER_SELECTION.isOpen()) {
         if (EditorWindowType.FILTER_SELECTION.begin("###MaskSelection", true)) {
            boolean docked = ImGui.isWindowDocked();
            String name = EditorWindowType.FILTER_SELECTION.getName();
            float currWidth = docked ? ImGui.getContentRegionAvailX() : Math.max(propertiesWidth, ImGuiHelper.calcTextWidth(name) + 40.0F);
            ImGui.setCursorPosX(ImGui.getCursorPosX() + (currWidth - 52.0F) / 2.0F);
            blockConditionWidget.renderBlockSwitcher(blockList, "MaskBlock");
            propertiesWidth = blockConditionWidget.renderPropertySettings("MaskBlock", docked).width();
            boolean disable = Selection.getSelectionBuffer().isEmpty();
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.filter_selection.filter") + "###MaskButton", currWidth, 0.0F)) {
               MaskSelection.mask(blockConditionWidget.createCondition());
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.FILTER_SELECTION.end();
      }
   }
}
