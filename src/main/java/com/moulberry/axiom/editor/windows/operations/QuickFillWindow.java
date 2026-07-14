package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.FillOperation;
import com.moulberry.axiom.tools.ToolManager;

public class QuickFillWindow {
   private static final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   public static boolean openQuickFillWindow = false;

   public static void render(BlockList blockList) {
      if (!Placement.INSTANCE.isPlacing()) {
         boolean open = EditorUI.canProcessKeybinds && Keybinds.QUICK_FILL.isPressed(false) && !Selection.getSelectionBuffer().isEmpty();
         if (!open && !openQuickFillWindow) {
            if (Selection.getSelectionBuffer().isEmpty()) {
               return;
            }
         } else {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            if (Selection.getSelectionBuffer().isEmpty()) {
               return;
            }

            selectBlockWidget.open();
            openQuickFillWindow = false;
         }

         selectBlockWidget.render(AxiomI18n.get("axiom.keybinds.quick_fill.fill_with"), blockList);
         CustomBlockState blockState = selectBlockWidget.getResultState();
         if (blockState != null) {
            FillOperation.fill(blockState.getVanillaState());
         }
      }
   }
}
