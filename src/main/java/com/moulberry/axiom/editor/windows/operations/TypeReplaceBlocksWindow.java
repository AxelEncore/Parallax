package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.widgets.BlockStateWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.TypeReplaceOperation;
import imgui.moulberry92.ImGui;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TypeReplaceBlocksWindow {
   private static final BlockStateWidget fromWidget = new BlockStateWidget(Blocks.STONE);
   private static final BlockStateWidget toWidget = new BlockStateWidget(Blocks.GRANITE);

   public static void render(BlockList blockList) {
      if (EditorWindowType.TYPE_REPLACE.isOpen()) {
         if (EditorWindowType.TYPE_REPLACE.begin("###TypeReplaceBlocks", true)) {
            boolean docked = ImGui.isWindowDocked();
            float start = ImGui.getCursorPosX();
            float width = 140.0F;
            if (ImGui.beginTable("##Table", 2, 33280)) {
               ImGui.tableNextColumn();
               String from = AxiomI18n.get("axiom.widget.from_block");
               String to = AxiomI18n.get("axiom.widget.to_block");
               if (!docked) {
                  ImGui.textDisabled(from);
                  ImGui.tableNextColumn();
                  ImGui.textDisabled(to);
                  ImGui.tableNextColumn();
               }

               fromWidget.renderBlockSwitcher(blockList, "From");
               ImGui.tableNextColumn();
               toWidget.renderBlockSwitcher(blockList, "To");
               ImGui.sameLine(0.0F, 0.0F);
               width = Math.max(width, ImGui.getCursorPosX() - start);
               ImGui.dummy(0.0F, 0.0F);
               ImGui.endTable();
            }

            boolean disable = Selection.getSelectionBuffer().isEmpty();
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.replace.do_replace"), width, 0.0F)) {
               BlockState first = fromWidget.getBlockState().getVanillaState();
               BlockState second = toWidget.getBlockState().getVanillaState();
               FamilyMap.AxiomBlockFamily firstFamily = FamilyMap.getFamilyForBase(first.getBlock());
               FamilyMap.AxiomBlockFamily secondFamily = FamilyMap.getFamilyForBase(second.getBlock());
               if (firstFamily != null && secondFamily != null) {
                  TypeReplaceOperation.replace(firstFamily, secondFamily);
               }
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.TYPE_REPLACE.end();
      }
   }

   static {
      Predicate<CustomBlockState> blockFamilyFilter = blockState -> FamilyMap.getFamilyForBase(blockState.getVanillaState().getBlock()) != null;
      fromWidget.setFilter(blockFamilyFilter);
      toWidget.setFilter(blockFamilyFilter);
   }
}
