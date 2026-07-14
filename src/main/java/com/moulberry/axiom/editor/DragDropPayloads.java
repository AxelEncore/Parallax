package com.moulberry.axiom.editor;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.utils.BlockWithFloat;

public class DragDropPayloads {
   public record NoisePainterBlock(int index, BlockWithFloat block) {
   }

   public record PaletteBlock(CustomBlockState state, EditorPalette paletteFrom, int indexFrom) {
   }
}
