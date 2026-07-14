package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.tutorial.TutorialStage;
import com.moulberry.axiom.editor.widgets.BlockStateWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;

public class CurrentBlockWindow {
   private static final BlockStateWidget blockStateWidget = new BlockStateWidget(Blocks.STONE);

   public static void render(ActiveBlockHistory activeBlockHistory, BlockList blockList) {
      boolean showTutorial = TutorialManager.getCurrentStage() == TutorialStage.ACTIVE_BLOCK;
      if (EditorWindowType.ACTIVE_BLOCK.isOpen() || showTutorial) {
         if (EditorWindowType.ACTIVE_BLOCK.begin("###ActiveBlock", true)) {
            CustomBlockState blockState = activeBlockHistory.getActive();
            blockStateWidget.setBlockState(blockState);
            blockStateWidget.renderBlockSwitcher(blockList, "Block");
            ImGui.sameLine();
            ImGui.beginGroup();
            ImGuiHelper.pushStyleVar(14, ImGui.getStyle().getItemSpacingX(), 1.0F);
            ImGui.text(AxiomI18n.get(blockState.getCustomBlock().axiom$translationKey()));
            ImGui.text(blockState.getCustomBlock().axiom$getIdentifier().toString());
            if (blockStateWidget.hasEditableProperties()) {
               ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.active_block.edit_properties_below"));
            }

            ImGuiHelper.popStyleVar();
            ImGui.endGroup();
            blockStateWidget.renderPropertySettings("Block", ImGui.isWindowDocked());
            activeBlockHistory.setActive(blockStateWidget.getBlockState());
            if (showTutorial) {
               TutorialStage.ACTIVE_BLOCK.render(ImGui.getWindowPos(), ImGui.getWindowSize());
            }
         } else if (showTutorial) {
            TutorialManager.nextTutorialStage();
         }

         EditorWindowType.ACTIVE_BLOCK.end();
      }
   }
}
