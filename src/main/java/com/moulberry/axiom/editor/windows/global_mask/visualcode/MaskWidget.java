package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import java.util.concurrent.ThreadLocalRandom;

public abstract class MaskWidget {
   private MaskWidgetWithChildren parent;
   private boolean dragging = false;
   protected final int randomId = ThreadLocalRandom.current().nextInt();

   public abstract MaskWidget copy();

   protected abstract void doRender(boolean var1, BooleanWrapper var2);

   public void render(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      ImGui.pushID(this.randomId);
      this.doRender(allowDragDropSource, allowDragDropTarget);
      ImGui.popID();
   }

   public MaskWidgetWithChildren parent() {
      return this.parent;
   }

   public void parent(MaskWidgetWithChildren parent) {
      this.parent = parent;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public void setDragging(boolean dragging) {
      this.dragging = dragging;
   }

   public static int makeSlightlyLighter(int argb) {
      int alpha = argb >> 24 & 0xFF;
      int red = argb >> 16 & 0xFF;
      int green = argb >> 8 & 0xFF;
      int blue = argb & 0xFF;
      alpha = Math.min(255, alpha * 7 / 6);
      red = Math.min(255, red * 7 / 6);
      green = Math.min(255, green * 7 / 6);
      blue = Math.min(255, blue * 7 / 6);
      return alpha << 24 | red << 8 | green << 16 | blue;
   }
}
