package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiomclientapi.pathers.BallShape;

public class ExpandSelection {
   public static void expand(int amount) {
      if (amount != 0) {
         if (!Selection.getSelectionBuffer().isEmpty()) {
            if (amount < 0) {
               shrink(-amount);
            } else {
               Selection.modify(region -> {
                  region.expand(BallShape.SPHERE, amount);
                  return region;
               });
            }
         }
      }
   }

   public static void shrink(int amount) {
      if (amount != 0) {
         if (!Selection.getSelectionBuffer().isEmpty()) {
            if (amount < 0) {
               expand(-amount);
            } else {
               Selection.modify(region -> {
                  region.shrink(BallShape.SPHERE, amount);
                  return region;
               });
            }
         }
      }
   }
}
