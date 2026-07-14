package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.editor.widgets.BlockStateWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.ReplaceCopyPropertiesOperation;
import com.moulberry.axiom.operations.ReplaceOperation;
import com.moulberry.axiom.utils.BlockCondition;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;

public class ReplaceBlocksWindow {
   private static final BlockConditionWidget blockConditionWidget = new BlockConditionWidget(Blocks.STONE);
   private static final BlockStateWidget blockStateWidget = new BlockStateWidget(Blocks.GRANITE);
   private static boolean copyProperties = false;

   public static void render(BlockList blockList) {
      if (EditorWindowType.REPLACE.isOpen()) {
         if (EditorWindowType.REPLACE.begin("###ReplaceBlocks", true)) {
            boolean docked = ImGui.isWindowDocked();
            float start = ImGui.getCursorPosX();
            float width = 140.0F;
            if (ImGui.beginTable("##Table", 2, 33280)) {
               ImGui.tableNextColumn();
               String from = AxiomI18n.get("axiom.widget.from_block");
               String to = AxiomI18n.get("axiom.widget.to_block");
               String properties = AxiomI18n.get("axiom.widget.block_properties");
               if (!docked) {
                  ImGui.textDisabled(from);
                  ImGui.tableNextColumn();
                  ImGui.textDisabled(to);
                  ImGui.tableNextColumn();
               }

               blockConditionWidget.renderBlockSwitcher(blockList, "From");
               ImGui.tableNextColumn();
               blockStateWidget.renderBlockSwitcher(blockList, "To");
               ImGui.sameLine(0.0F, 0.0F);
               width = Math.max(width, ImGui.getCursorPosX() - start);
               ImGui.dummy(0.0F, 0.0F);
               if (!copyProperties) {
                  ImGui.tableNextColumn();
                  if (blockConditionWidget.hasEditableProperties()) {
                     ImGui.textDisabled(properties);
                     float propertyWidth = blockConditionWidget.renderPropertySettings("From", docked).width();
                     width = Math.max(width, propertyWidth * 2.0F + 5.0F);
                  }

                  ImGui.tableNextColumn();
                  if (blockStateWidget.hasEditableProperties()) {
                     ImGui.textDisabled(properties);
                     float propertyWidth = blockStateWidget.renderPropertySettings("To", docked);
                     width = Math.max(width, propertyWidth * 2.0F + 5.0F);
                  }
               }

               ImGui.endTable();
            }

            CustomBlock fromBlock = blockConditionWidget.getBlock();
            CustomBlockState toBlockState = blockStateWidget.getBlockState();
            boolean sharesProperties = false;
            if (fromBlock != null && toBlockState != null) {
               for (Property<?> property : blockConditionWidget.getBlock().axiom$getProperties()) {
                  if (toBlockState.axiomHasProperty(property)) {
                     sharesProperties = true;
                  }
               }
            }

            if (sharesProperties) {
               if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.replace.copy_properties"), copyProperties)) {
                  copyProperties = !copyProperties;
               }
            } else {
               copyProperties = false;
            }

            boolean disable = Selection.getSelectionBuffer().isEmpty() || fromBlock == null && blockConditionWidget.getTag() == null || toBlockState == null;
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.replace.do_replace"), width, 0.0F) && !disable) {
               if (copyProperties) {
                  ReplaceCopyPropertiesOperation.replace(BlockCondition.fromCustomBlock(fromBlock), toBlockState);
               } else {
                  ReplaceOperation.replace(blockConditionWidget.createCondition(), toBlockState.getVanillaState());
               }
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.REPLACE.end();
      }
   }
}
