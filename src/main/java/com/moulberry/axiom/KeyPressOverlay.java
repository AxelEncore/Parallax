package com.moulberry.axiom;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.moulberry.axiom.i18n.AxiomI18n;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

public class KeyPressOverlay {
   public static final int SCROLL_UP = -200;
   public static final int SCROLL_DOWN = -201;
   public static final int SCROLL_LEFT = -202;
   public static final int SCROLL_RIGHT = -203;
   private static Int2IntMap visuallyPressed = new Int2IntLinkedOpenHashMap();
   private static final IntSet currentlyPressed = new IntLinkedOpenHashSet();

   public static void addKey(int key) {
      if (currentlyPressed.add(key)) {
         visuallyPressed.put(key, 20);
         visuallyPressed.keySet().removeIf(k -> !currentlyPressed.contains(k));
      }
   }

   public static void removeKey(int key) {
      currentlyPressed.remove(key);
   }

   public static List<KeyPressOverlay.StringWithOpacity> getStrings(Font font) {
      if (visuallyPressed.isEmpty()) {
         return List.of();
      } else {
         List<KeyPressOverlay.StringWithOpacity> strings = new ArrayList<>();
         StringBuilder builder = new StringBuilder();
         boolean empty = true;
         int builderOpacity = 255;
         ObjectIterator string = visuallyPressed.int2IntEntrySet().iterator();

         while (string.hasNext()) {
            Entry entry = (Entry)string.next();
            int opacity = entry.getIntValue() * 255 / 20;
            if (builderOpacity != opacity) {
               if (!builder.isEmpty()) {
                  String stringx = builder.toString();
                  strings.add(new KeyPressOverlay.StringWithOpacity(stringx, builderOpacity, font.width(stringx)));
                  builder.setLength(0);
               }

               builderOpacity = opacity;
            }

            if (!empty) {
               builder.append("+");
            } else {
               empty = false;
            }

            builder.append(getKeyString(entry.getIntKey()));
         }

         if (!builder.isEmpty()) {
            String stringx = builder.toString();
            strings.add(new KeyPressOverlay.StringWithOpacity(builder.toString(), builderOpacity, font.width(stringx)));
         }

         return strings;
      }
   }

   private static String getKeyString(int key) {
      if (key < 0) {
         if (key == -200) {
            return AxiomI18n.get("axiom.key.scroll_up");
         } else if (key == -201) {
            return AxiomI18n.get("axiom.key.scroll_down");
         } else if (key == -202) {
            return AxiomI18n.get("axiom.key.scroll_left");
         } else {
            return key == -203 ? AxiomI18n.get("axiom.key.scroll_right") : Type.MOUSE.getOrCreate(-key - 1).getDisplayName().getString();
         }
      } else {
         return InputConstants.getKey(key, -1).getDisplayName().getString();
      }
   }

   public static void tick() {
      Int2IntMap newVisuallyPressed = new Int2IntLinkedOpenHashMap();
      ObjectIterator var1 = visuallyPressed.int2IntEntrySet().iterator();

      while (var1.hasNext()) {
         Entry entry = (Entry)var1.next();
         boolean isCurrentlyPressed = currentlyPressed.contains(entry.getIntKey());
         if (isCurrentlyPressed) {
            newVisuallyPressed.put(entry.getIntKey(), 20);
         } else {
            int newValue = entry.getIntValue() - 1;
            if (newValue > 0) {
               newVisuallyPressed.put(entry.getIntKey(), newValue);
            }
         }
      }

      visuallyPressed = newVisuallyPressed;
   }

   public record StringWithOpacity(String string, int opacity, int width) {
   }
}
