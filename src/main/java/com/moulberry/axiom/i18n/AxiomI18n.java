package com.moulberry.axiom.i18n;

import java.util.IllegalFormatException;
import net.minecraft.locale.Language;

public class AxiomI18n {
   private static volatile Language language = Language.getInstance();

   private AxiomI18n() {
   }

   public static void setLanguage(Language language) {
      AxiomI18n.language = language;
   }

   public static String get(String string) {
      return language.getOrDefault(string);
   }

   public static String getOrDefault(String string, String defaultValue) {
      return language.getOrDefault(string, defaultValue);
   }

   public static String get(String string, Object... objects) {
      String string2 = language.getOrDefault(string);

      try {
         return String.format(string2, objects);
      } catch (IllegalFormatException var4) {
         return "Format error: " + string2;
      }
   }

   public static boolean exists(String string) {
      return language.has(string);
   }
}
