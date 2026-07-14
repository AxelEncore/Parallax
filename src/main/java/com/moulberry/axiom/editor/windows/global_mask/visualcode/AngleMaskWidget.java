package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;

public class AngleMaskWidget extends MaskWidget {
   private final int[] value = new int[]{0};
   private final int[] range = new int[]{20};

   public AngleMaskWidget(int angle, int range) {
      this.value[0] = angle;
      this.range[0] = range;
   }

   public AngleMaskWidget() {
   }

   @Override
   public MaskWidget copy() {
      return new AngleMaskWidget(this.value[0], this.range[0]);
   }

   @Override
   public void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      int height = (int)ImGui.getFrameHeight() * 2;
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_angle") + " =", 0.0F, height);
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
      }

      boolean changed = false;
      int buttonCol = ImGui.getColorU32(21);
      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.beginGroup();
      ImGuiHelper.pushStyleColor(23, buttonCol);
      ImGuiHelper.pushStyleColor(22, buttonCol);
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_angle"), 64.0F, height - ImGui.getFrameHeight());
      ImGuiHelper.tooltip(AxiomI18n.get("axiom.mask.angle.angle_tooltip"));
      ImGuiHelper.popStyleColor(2);
      ImGui.setNextItemWidth(64.0F);
      ImGui.setCursorPosY(ImGui.getCursorPosY() - ImGui.getStyle().getItemSpacingY());
      if (ImGuiHelper.inputInt("##Angle", this.value)) {
         changed = true;
      }

      ImGui.endGroup();
      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.beginGroup();
      ImGuiHelper.pushStyleColor(23, buttonCol);
      ImGuiHelper.pushStyleColor(22, buttonCol);
      ImGui.button(AxiomI18n.get("axiom.mask.angle.range"), 64.0F, height - ImGui.getFrameHeight());
      ImGuiHelper.tooltip(AxiomI18n.get("axiom.mask.angle.range_tooltip"));
      ImGuiHelper.popStyleColor(2);
      ImGui.setNextItemWidth(64.0F);
      ImGui.setCursorPosY(ImGui.getCursorPosY() - ImGui.getStyle().getItemSpacingY());
      if (ImGuiHelper.inputInt("##RangeInput", this.range)) {
         changed = true;
      }

      ImGui.endGroup();
      if (changed) {
         ToolMaskWindow.markDirty(this);
      }
   }

   public int getAngle() {
      return this.value[0];
   }

   public int getRange() {
      return this.range[0];
   }
}
