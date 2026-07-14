package com.moulberry.axiom.editor.windows.clipboard;

import java.util.Comparator;

public class NaturalOrderComparator implements Comparator<String> {
   public static NaturalOrderComparator INSTANCE = new NaturalOrderComparator();

   private int compareRight(String a, String b) {
      int bias = 0;
      int ia = 0;
      int ib = 0;

      while (true) {
         char ca = charAt(a, ia);
         char cb = charAt(b, ib);
         if (!isDigit(ca) && !isDigit(cb)) {
            return bias;
         }

         if (!isDigit(ca)) {
            return -1;
         }

         if (!isDigit(cb)) {
            return 1;
         }

         if (ca == 0 && cb == 0) {
            return bias;
         }

         if (bias == 0) {
            if (ca < cb) {
               bias = -1;
            } else if (ca > cb) {
               bias = 1;
            }
         }

         ia++;
         ib++;
      }
   }

   public int compare(String a, String b) {
      int indexA = 0;
      int indexB = 0;

      while (true) {
         int numZeroesB = 0;
         int numZeroesA = 0;
         char charA = charAt(a, indexA);

         char charB;
         for (charB = charAt(b, indexB); Character.isSpaceChar(charA) || charA == '0'; charA = charAt(a, ++indexA)) {
            if (charA == '0') {
               numZeroesA++;
            } else {
               numZeroesA = 0;
            }
         }

         for (; Character.isSpaceChar(charB) || charB == '0'; charB = charAt(b, ++indexB)) {
            if (charB == '0') {
               numZeroesB++;
            } else {
               numZeroesB = 0;
            }
         }

         if (charA == 0 && charB == 0) {
            return compareEqual(a, b, numZeroesA, numZeroesB);
         }

         boolean isDigitA = Character.isDigit(charA);
         boolean isDigitB = Character.isDigit(charB);
         if (isDigitA && isDigitB) {
            int bias = this.compareRight(a.substring(indexA), b.substring(indexB));
            if (bias != 0) {
               return bias;
            }
         }

         if (isDigitA && !isDigitB) {
            return 1;
         }

         if (isDigitB && !isDigitA) {
            return -1;
         }

         int charCompare = Character.compare(charA, charB);
         if (charCompare != 0) {
            return charCompare;
         }

         indexA++;
         indexB++;
      }
   }

   private static boolean isDigit(char c) {
      return Character.isDigit(c) || c == '.' || c == ',';
   }

   private static char charAt(String s, int i) {
      return i >= s.length() ? '\u0000' : s.charAt(i);
   }

   private static int compareEqual(String a, String b, int nza, int nzb) {
      if (nza - nzb != 0) {
         return nza - nzb;
      } else {
         return a.length() == b.length() ? a.compareTo(b) : a.length() - b.length();
      }
   }
}
