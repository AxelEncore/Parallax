package com.moulberry.axiom.render;

public class Effects {
   public static final int BLUE = 1;
   public static final int RED = 2;
   public static final int OUTLINE = 4;
   public static final int REMOVAL = 8;
   public static final int SELECTION = 7;
   private static final int ALL = 15;

   public static boolean blueOrRed(int effects) {
      return (effects & 3) != 0;
   }

   public static boolean any(int effects) {
      return (effects & 15) != 0;
   }
}
