package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;

public class BlockBelowMaskWidget extends MaskWidget {
   private final BlockConditionWidget blockConditionWidget = new BlockConditionWidget(Blocks.STONE);

   public BlockBelowMaskWidget() {
   }

   public BlockBelowMaskWidget(BlockConditionWidget.BlockConditionState state) {
      this.blockConditionWidget.revertState(state);
   }

   @Override
   public MaskWidget copy() {
      return new BlockBelowMaskWidget(this.blockConditionWidget.createState());
   }

   @Override
   public void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_below") + " =", 0.0F, (int)(52.0F * EditorUI.getUiScale()));
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
      }

      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      boolean changed = this.blockConditionWidget.renderBlockSwitcher(EditorUI.getBlockList(), "From", allowDragDropTarget);
      if (this.blockConditionWidget.hasEditableProperties()) {
         ImGui.sameLine();
         ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
         if (ImGui.button(" * ", 0.0F, (int)(52.0F * EditorUI.getUiScale()))) {
            ImGui.openPopup("##Properties");
         }

         if (ImGui.beginPopup("##Properties")) {
            ImGui.textDisabled(AxiomI18n.get("axiom.widget.block_properties"));
            changed |= this.blockConditionWidget.renderPropertySettings("From", false).changed();
            ImGui.endPopup();
         }
      }

      if (changed) {
         ToolMaskWindow.markDirty(this);
      }
   }

   public BlockCondition createCondition() {
      return this.blockConditionWidget.createCondition();
   }

   public BlockConditionWidget.BlockConditionState createState() {
      return this.blockConditionWidget.createState();
   }

   public void revertState(BlockConditionWidget.BlockConditionState state) {
      this.blockConditionWidget.revertState(state);
      ToolMaskWindow.markDirty(this);
   }
}
