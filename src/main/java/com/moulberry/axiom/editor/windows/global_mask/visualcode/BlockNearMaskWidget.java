package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import net.minecraft.world.level.block.Blocks;

public class BlockNearMaskWidget extends MaskWidget {
   private final BlockConditionWidget blockConditionWidget = new BlockConditionWidget(Blocks.STONE);
   private final int[] radius = new int[]{1};

   public BlockNearMaskWidget() {
   }

   public BlockNearMaskWidget(BlockConditionWidget.BlockConditionState state, int radius) {
      this.blockConditionWidget.revertState(state);
      this.radius[0] = radius;
   }

   @Override
   public MaskWidget copy() {
      return new BlockNearMaskWidget(this.blockConditionWidget.createState(), this.radius[0]);
   }

   @Override
   public void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      int height = (int)(52.0F * EditorUI.getUiScale());
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_near") + " =", 0.0F, height);
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

      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.beginGroup();
      int buttonCol = ImGui.getColorU32(21);
      ImGuiHelper.pushStyleColor(23, buttonCol);
      ImGuiHelper.pushStyleColor(22, buttonCol);
      ImGui.button(AxiomI18n.get("axiom.mask.near.radius"), 64.0F, height - ImGui.getFrameHeight());
      ImGuiHelper.popStyleColor(2);
      ImGui.setNextItemWidth(64.0F);
      ImGui.setCursorPosY(ImGui.getCursorPosY() - ImGui.getStyle().getItemSpacingY());
      if (ImGuiHelper.inputInt("##RadiusInput", this.radius)) {
         changed = true;
      }

      ImGui.endGroup();
      if (changed) {
         ToolMaskWindow.markDirty(this);
      }
   }

   public int getRadius() {
      return this.radius[0];
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
