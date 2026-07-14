package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import java.util.Locale;
import net.minecraft.core.Direction.Axis;

public class CoordMaskWidget extends MaskWidget {
   private static final String[] COMPARISON_TYPES = new String[]{"=", "!=", "<", ">", "<=", ">="};
   private Axis axis = Axis.Y;
   private final int[] comparisonType = new int[]{0};
   private final int[] value = new int[]{62};

   public CoordMaskWidget(Axis axis, int yLevel, int comparison) {
      this.axis = axis;
      this.value[0] = yLevel;
      this.comparisonType[0] = comparison;
   }

   public CoordMaskWidget() {
   }

   @Override
   public MaskWidget copy() {
      return new CoordMaskWidget(this.axis, this.value[0], this.comparisonType[0]);
   }

   @Override
   public void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      ImGui.button(this.axis.getName().toUpperCase(Locale.ROOT), 0.0F, ImGui.getFrameHeight());
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
      }

      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.setNextItemWidth(ImGuiHelper.calcTextWidth(COMPARISON_TYPES[this.comparisonType[0]]) + ImGui.getStyle().getFramePaddingX() * 2.0F);
      ImGuiHelper.pushStyleColor(7, MaskWidget.makeSlightlyLighter(ImGui.getColorU32(7)));
      ImGuiHelper.pushStyleColor(9, MaskWidget.makeSlightlyLighter(ImGui.getColorU32(9)));
      ImGuiHelper.pushStyleColor(8, MaskWidget.makeSlightlyLighter(ImGui.getColorU32(8)));
      if (ImGuiHelper.combo("###ComparisonCombo", this.comparisonType, COMPARISON_TYPES, 32)) {
         ToolMaskWindow.markDirty(this);
      }

      ImGuiHelper.popStyleColor(3);
      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      ImGui.setNextItemWidth(64.0F);
      if (ImGuiHelper.inputInt("##Input", this.value)) {
         ToolMaskWindow.markDirty(this);
      }
   }

   public Axis getAxis() {
      return this.axis;
   }

   public int getComparisonType() {
      return this.comparisonType[0];
   }

   public int getYValue() {
      return this.value[0];
   }
}
