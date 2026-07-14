package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;

public class OffsetMaskWidget extends MaskWidgetWithChildren {
   private final int[] offsetX = new int[]{0};
   private final int[] offsetY = new int[]{0};
   private final int[] offsetZ = new int[]{0};

   public OffsetMaskWidget(int offsetX, int offsetY, int offsetZ) {
      super(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_offset"), 1);
      this.offsetX[0] = offsetX;
      this.offsetY[0] = offsetY;
      this.offsetZ[0] = offsetZ;
   }

   @Override
   public MaskWidget copy() {
      OffsetMaskWidget widget = new OffsetMaskWidget(this.offsetX[0], this.offsetY[0], this.offsetZ[0]);

      for (MaskWidget child : this.children) {
         widget.addChild(-1, child.copy());
      }

      return widget;
   }

   @Override
   protected void renderExtra() {
      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.setNextItemWidth(30.0F);
      boolean changed = ImGuiHelper.inputInt("##OffsetX", this.offsetX);
      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.setNextItemWidth(30.0F);
      changed |= ImGuiHelper.inputInt("##OffsetY", this.offsetY);
      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.setNextItemWidth(30.0F);
      changed |= ImGuiHelper.inputInt("##OffsetZ", this.offsetZ);
      if (changed) {
         ToolMaskWindow.markDirty(this);
      }
   }

   public int getOffsetX() {
      return this.offsetX[0];
   }

   public int getOffsetY() {
      return this.offsetY[0];
   }

   public int getOffsetZ() {
      return this.offsetZ[0];
   }
}
