package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BlockStateWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.FillOperation;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;

public class FillBlocksWindow {
   private static final BlockStateWidget blockStateWidget = new BlockStateWidget(Blocks.STONE);
   private static int[] typeSelected = new int[]{0};
   private static int width = -1;
   private static float propertiesWidth = -1.0F;

   public static void setFillType(int fillType) {
      typeSelected[0] = fillType;
   }

   public static void render(BlockList blockList) {
      if (!EditorWindowType.FILL.isOpen()) {
         width = -1;
      } else {
         if (EditorWindowType.FILL.begin("###Fill", true)) {
            boolean docked = ImGui.isWindowDocked();
            String[] typeStrings = new String[]{
               AxiomI18n.get("axiom.editorui.window.fill.all"),
               AxiomI18n.get("axiom.editorui.window.fill.inside"),
               AxiomI18n.get("axiom.editorui.window.fill.outline"),
               AxiomI18n.get("axiom.editorui.window.fill.walls"),
               AxiomI18n.get("axiom.editorui.window.fill.top"),
               AxiomI18n.get("axiom.editorui.window.fill.bottom")
            };
            int buttonWidth = (int)(52.0F * EditorUI.getUiScale());
            if (width == -1) {
               for (String type : typeStrings) {
                  width = (int)Math.max((float)width, ImGuiHelper.calcTextWidth(type));
               }

               width = width + (int)Math.ceil(ImGui.getFrameHeight() + ImGui.getStyle().getFramePaddingX() * 2.0F);
               if (width < buttonWidth) {
                  width = buttonWidth;
               }
            }

            float currWidth = docked ? ImGui.getContentRegionAvailX() : Math.max(propertiesWidth, (float)width);
            ImGui.setNextItemWidth(currWidth);
            ImGuiHelper.combo("##SectionType", typeSelected, typeStrings);
            ImGui.setCursorPosX(ImGui.getCursorPosX() + (currWidth - buttonWidth) / 2.0F);
            blockStateWidget.renderBlockSwitcher(blockList, "Block");
            if (blockStateWidget.hasEditableProperties()) {
               ImGui.textDisabled(AxiomI18n.get("axiom.widget.block_properties"));
               propertiesWidth = blockStateWidget.renderPropertySettings("Block", docked);
            }

            boolean disable = Selection.getSelectionBuffer().isEmpty();
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.fill.do_fill"), currWidth, 0.0F)) {
               int flags = switch (typeSelected[0]) {
                  case 0 -> FillOperation.FILL_ALL;
                  case 1 -> FillOperation.FILL_INSIDE;
                  case 2 -> FillOperation.FILL_EXTERIOR;
                  case 3 -> FillOperation.FILL_WALLS;
                  case 4 -> FillOperation.FILL_CEILING;
                  case 5 -> FillOperation.FILL_FLOOR;
                  default -> throw new FaultyImplementationError();
               };
               FillOperation.fill(blockStateWidget.getBlockState().getVanillaState(), flags);
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.FILL.end();
      }
   }
}
