package com.moulberry.axiom.utils;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;

public class StringUtils {
   private static final CharSet ILLEGAL_CHARACTERS = new CharOpenHashSet();
   private static final Pattern WINDOWS_RESERVED_REGEX;

   public static String convertResourceToPretty(ResourceLocation resource) {
      StringBuilder builder = new StringBuilder();
      if (!resource.getNamespace().equals("minecraft")) {
         builder.append('(');
         builder.append(convertSnakeToWords(resource.getNamespace()));
         builder.append(") ");
      }

      builder.append(convertSnakeToWords(resource.getPath()));
      return builder.toString().strip();
   }

   public static String convertSnakeToWords(String snake) {
      StringBuilder builder = new StringBuilder();
      boolean capitalize = true;
      int length = snake.length();

      for (int i = 0; i < length; i++) {
         char c = snake.charAt(i);
         if (c == '_') {
            builder.append(' ');
            capitalize = true;
         } else if (capitalize) {
            builder.append(Character.toUpperCase(c));
            capitalize = false;
         } else {
            builder.append(Character.toLowerCase(c));
         }
      }

      if (capitalize) {
         builder.append('_');
      }

      return builder.toString().strip();
   }

   public static String convertSnakeToCamel(String snake) {
      StringBuilder builder = new StringBuilder();
      boolean capitalize = false;
      int length = snake.length();

      for (int i = 0; i < length; i++) {
         char c = snake.charAt(i);
         if (c == '_') {
            capitalize = true;
         } else if (capitalize) {
            builder.append(Character.toUpperCase(c));
            capitalize = false;
         } else {
            builder.append(Character.toLowerCase(c));
         }
      }

      if (capitalize) {
         builder.append('_');
      }

      return builder.toString();
   }

   public static String sanitizePath(String string) {
      StringBuilder builder = new StringBuilder();

      for (char c : string.toCharArray()) {
         if (!ILLEGAL_CHARACTERS.contains(c)) {
            builder.append(c);
         }
      }

      string = builder.toString();
      if (WINDOWS_RESERVED_REGEX.matcher(string).matches()) {
         string = "_" + string + "_";
      }

      string = string.strip();

      while (string.startsWith(".")) {
         string = string.substring(1);
      }

      return string.isEmpty() ? "__EMPTY__" : string;
   }

   static {
      ILLEGAL_CHARACTERS.add('/');
      ILLEGAL_CHARACTERS.add('?');
      ILLEGAL_CHARACTERS.add('<');
      ILLEGAL_CHARACTERS.add('>');
      ILLEGAL_CHARACTERS.add('\\');
      ILLEGAL_CHARACTERS.add(':');
      ILLEGAL_CHARACTERS.add('*');
      ILLEGAL_CHARACTERS.add('%');
      ILLEGAL_CHARACTERS.add('|');
      ILLEGAL_CHARACTERS.add('"');

      for (char c = 0; c <= 31; c++) {
         ILLEGAL_CHARACTERS.add(c);
      }

      for (char c = 128; c <= 159; c++) {
         ILLEGAL_CHARACTERS.add(c);
      }

      WINDOWS_RESERVED_REGEX = Pattern.compile("(?:con|prn|aux|nul|com[0-9]|lpt[0-9])(?:\\..*)?$", 2);
   }
}
