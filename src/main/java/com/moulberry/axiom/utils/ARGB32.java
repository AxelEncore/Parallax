package com.moulberry.axiom.utils;

public class ARGB32 {
   public static int red(int r) {
      return r >> 16 & 0xFF;
   }

   public static int green(int g) {
      return g >> 8 & 0xFF;
   }

   public static int blue(int b) {
      return b & 0xFF;
   }
}
