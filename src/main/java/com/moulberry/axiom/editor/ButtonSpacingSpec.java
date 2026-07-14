package com.moulberry.axiom.editor;

public record ButtonSpacingSpec(int buttonsPerRow, float spacingX, float spacingY) {
   public static ButtonSpacingSpec calculate(float availableSpace, float sizeX, int maxItems, float maxSpacing, boolean center) {
      int buttonsPerRow = (int)Math.floor(availableSpace / sizeX);
      if (maxItems > 0 && buttonsPerRow > maxItems) {
         buttonsPerRow = maxItems;
      }

      float spacingX;
      if (buttonsPerRow <= 1) {
         buttonsPerRow = 1;
         spacingX = 0.0F;
      } else {
         for (spacingX = (availableSpace - buttonsPerRow * sizeX) / (buttonsPerRow + (center ? 1 : -1));
            spacingX < 2.0F && buttonsPerRow > 1;
            spacingX = (availableSpace - buttonsPerRow * sizeX) / (buttonsPerRow + (center ? 1 : -1))
         ) {
            buttonsPerRow--;
         }

         if (spacingX > maxSpacing) {
            spacingX = maxSpacing;
         }
      }

      if (buttonsPerRow == 1) {
         if (center) {
            spacingX = availableSpace / 2.0F - sizeX / 2.0F;
         } else {
            spacingX = 0.0F;
         }
      }

      float spacingY = spacingX;
      if (buttonsPerRow == 1) {
         spacingY = 2.0F;
      } else if (spacingX < 2.0F) {
         spacingY = 2.0F;
      }

      return center
         ? new ButtonSpacingSpec(buttonsPerRow, spacingX, spacingY)
         : new ButtonSpacingSpec(buttonsPerRow, (float)Math.floor(spacingX), (float)Math.floor(spacingY));
   }
}
