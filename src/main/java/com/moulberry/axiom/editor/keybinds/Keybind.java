package com.moulberry.axiom.editor.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.lattice.keybind.KeybindInterface;
import com.moulberry.lattice.keybind.LatticeInputType;
import imgui.moulberry92.ImGuiIO;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class Keybind implements KeybindInterface {
   private final String description;
   private int key;
   private boolean shiftMod;
   private boolean ctrlMod;
   private boolean altMod;
   private boolean superMod;
   private boolean ingameDownLastTime = false;

   public Keybind(String description, int key, boolean shiftMod, boolean ctrlMod, boolean altMod, boolean superMod) {
      this.description = description;
      this.key = key;
      this.shiftMod = shiftMod;
      this.ctrlMod = ctrlMod;
      this.altMod = altMod;
      this.superMod = superMod;
      Keybinds.updateMapping(this, 0);
   }

   public int getKey() {
      return this.key;
   }

   public boolean isShiftMod() {
      return this.shiftMod;
   }

   public boolean isCtrlMod() {
      return this.ctrlMod;
   }

   public boolean isAltMod() {
      return this.altMod;
   }

   public boolean isSuperMod() {
      return this.superMod;
   }

   public void clear() {
      int oldKey = this.key;
      this.key = 0;
      this.shiftMod = false;
      this.ctrlMod = false;
      this.altMod = false;
      this.superMod = false;
      Keybinds.updateMapping(this, oldKey);
   }

   public void set(Keybind other) {
      if (other.description.equals(this.description)) {
         int oldKey = this.key;
         this.key = other.key;
         this.shiftMod = other.shiftMod;
         this.ctrlMod = other.ctrlMod;
         this.altMod = other.altMod;
         this.superMod = other.superMod;
         Keybinds.updateMapping(this, oldKey);
      }
   }

   public String toConfigValue() {
      if (this.key == 0) {
         return "none";
      } else {
         String key = KeybindHelper.glfwToConfig(this.key);
         if (key.equals("none")) {
            return "none";
         } else {
            StringBuilder builder = new StringBuilder();
            if (this.shiftMod) {
               builder.append("shift+");
            }

            if (this.ctrlMod) {
               builder.append("ctrl+");
            }

            if (this.altMod) {
               builder.append("alt+");
            }

            if (this.superMod) {
               builder.append("super+");
            }

            builder.append(key);
            return builder.toString();
         }
      }
   }

   public void loadFromConfigValue(String configValue) {
      configValue = configValue.toLowerCase(Locale.ROOT);
      configValue = configValue.replaceAll("[^a-z0-9+_]", "");
      if (configValue.equals("none")) {
         int oldKey = this.key;
         this.key = 0;
         this.shiftMod = this.ctrlMod = this.altMod = this.superMod = false;
         Keybinds.updateMapping(this, oldKey);
      } else {
         boolean shiftMod = false;
         boolean ctrlMod = false;
         boolean altMod = false;
         boolean superMod = false;

         while (true) {
            while (!configValue.startsWith("shift+")) {
               if (configValue.startsWith("ctrl+")) {
                  ctrlMod = true;
                  configValue = configValue.substring(5);
               } else if (configValue.startsWith("alt+")) {
                  altMod = true;
                  configValue = configValue.substring(4);
               } else {
                  if (!configValue.startsWith("super+")) {
                     configValue = configValue.substring(configValue.lastIndexOf("+") + 1);
                     int key = KeybindHelper.configToGlfw(configValue);
                     if (key != 0) {
                        int oldKey = this.key;
                        this.key = key;
                        this.shiftMod = shiftMod;
                        this.ctrlMod = ctrlMod;
                        this.altMod = altMod;
                        this.superMod = superMod;
                        Keybinds.updateMapping(this, oldKey);
                     } else {
                        Axiom.dbg("Invalid keybind in config for " + this.description + ": " + configValue);
                     }

                     return;
                  }

                  superMod = true;
                  configValue = configValue.substring(6);
               }
            }

            shiftMod = true;
            configValue = configValue.substring(6);
         }
      }
   }

   public Keybind copy() {
      return new Keybind(this.description, this.key, this.shiftMod, this.ctrlMod, this.altMod, this.superMod);
   }

   public void set(int key, boolean shiftMod, boolean ctrlMod, boolean altMod, boolean superMod) {
      int oldKey = this.key;
      this.key = key;
      this.shiftMod = shiftMod;
      this.ctrlMod = ctrlMod;
      this.altMod = altMod;
      this.superMod = superMod;
      Keybinds.updateMapping(this, oldKey);
   }

   public boolean hasSameMods(Keybind other) {
      return this.shiftMod == other.shiftMod && this.ctrlMod == other.ctrlMod && this.altMod == other.altMod && this.superMod == other.superMod;
   }

   private static String getKeyNameFor(int key) {
      return InputConstants.getKey(key, -1).getDisplayName().getString();
   }

   public String shortKeyIdentifier() {
      if (this.key == 0) {
         return "";
      } else {
         String keyName;
         if (this.key < 0) {
            keyName = "M" + -this.key;
         } else {
            keyName = getKeyNameFor(this.key);
         }

         if (!this.shiftMod && !this.ctrlMod && !this.altMod && !this.superMod) {
            return keyName;
         } else {
            char macCmd = 8984;
            char alt = 9095;
            char shift = 8679;
            char superKey = 10070;
            StringBuilder builder = new StringBuilder();
            if (this.shiftMod) {
               builder.append('⇧');
            }

            if (this.ctrlMod) {
               if (Minecraft.ON_OSX) {
                  builder.append('⌘');
               } else {
                  builder.append(AxiomI18n.get("axiom.keymod.ctrl")).append("+");
               }
            }

            if (this.altMod) {
               builder.append('⎇');
            }

            if (this.superMod) {
               if (Minecraft.ON_OSX) {
                  builder.append(AxiomI18n.get("axiom.keymod.ctrl")).append("+");
               } else {
                  builder.append('❖');
               }
            }

            builder.append(keyName);
            return builder.toString();
         }
      }
   }

   public String longKeyIdentifier() {
      if (this.key == 0) {
         return AxiomI18n.get("key.keyboard.unknown");
      } else {
         String keyName;
         if (this.key < 0) {
            keyName = Type.MOUSE.getOrCreate(-this.key - 1).getDisplayName().getString();
         } else {
            keyName = getKeyNameFor(this.key);
         }

         if (!this.shiftMod && !this.ctrlMod && !this.altMod && !this.superMod) {
            return keyName;
         } else {
            StringBuilder builder = new StringBuilder();
            if (this.shiftMod) {
               builder.append(AxiomI18n.get("axiom.keymod.shift")).append("+");
            }

            if (this.ctrlMod) {
               if (Minecraft.ON_OSX) {
                  builder.append(AxiomI18n.get("axiom.keymod.mac_cmd")).append("+");
               } else {
                  builder.append(AxiomI18n.get("axiom.keymod.ctrl")).append("+");
               }
            }

            if (this.altMod) {
               builder.append(AxiomI18n.get("axiom.keymod.alt")).append("+");
            }

            if (this.superMod) {
               if (Minecraft.ON_OSX) {
                  builder.append(AxiomI18n.get("axiom.keymod.ctrl")).append("+");
               } else if (Util.getPlatform() == OS.WINDOWS) {
                  builder.append(AxiomI18n.get("axiom.keymod.win_super")).append("+");
               } else {
                  builder.append(AxiomI18n.get("axiom.keymod.super")).append("+");
               }
            }

            builder.append(keyName);
            return builder.toString();
         }
      }
   }

   public boolean wouldBePressed(int key, boolean shiftMod, boolean ctrlMod, boolean altMod, boolean superMod) {
      if (this.key == 0) {
         return false;
      } else if (this.key != key) {
         return false;
      } else if (this.shiftMod != shiftMod) {
         return false;
      } else if (this.ctrlMod != (Minecraft.ON_OSX ? superMod : ctrlMod)) {
         return false;
      } else {
         return this.altMod != altMod ? false : this.superMod == (Minecraft.ON_OSX ? ctrlMod : superMod);
      }
   }

   public boolean isDownUsingGLFW() {
      if (this.key == 0) {
         return false;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         long window = minecraft.getWindow().getWindow();
         if (this.key < 0) {
            if (GLFW.glfwGetMouseButton(window, -this.key - 1) == 0) {
               return false;
            }
         } else if (GLFW.glfwGetKey(window, this.key) == 0) {
            return false;
         }

         boolean ctrlMod = this.ctrlMod;
         boolean superMod = this.superMod;
         if (Minecraft.ON_OSX) {
            ctrlMod = this.superMod;
            superMod = this.ctrlMod;
         }

         if (!isShift(this.key) && this.shiftMod != isShiftDownGLFW(window)) {
            return false;
         } else if (!isCtrl(this.key) && ctrlMod != isCtrlDownGLFW(window)) {
            return false;
         } else {
            return !isAlt(this.key) && this.altMod != isAltDownGLFW(window) ? false : isSuper(this.key) || superMod == isSuperDownGLFW(window);
         }
      }
   }

   private static boolean isShift(int key) {
      return key == 340 || key == 344;
   }

   private static boolean isCtrl(int key) {
      return key == 341 || key == 345;
   }

   private static boolean isAlt(int key) {
      return key == 342 || key == 346;
   }

   private static boolean isSuper(int key) {
      return key == 343 || key == 347;
   }

   private static boolean isShiftDownGLFW(long window) {
      return GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1;
   }

   private static boolean isCtrlDownGLFW(long window) {
      return GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
   }

   private static boolean isAltDownGLFW(long window) {
      return GLFW.glfwGetKey(window, 342) == 1 || GLFW.glfwGetKey(window, 346) == 1;
   }

   private static boolean isSuperDownGLFW(long window) {
      return GLFW.glfwGetKey(window, 343) == 1 || GLFW.glfwGetKey(window, 347) == 1;
   }

   public boolean isDown() {
      return this.areAllModifiersDown() && this.isDownIgnoreMods();
   }

   public boolean isDownIgnoreMods() {
      if (this.key == 0) {
         return false;
      } else if (EditorUI.isActive() && EditorUI.isImGuiContextActive()) {
         return ImGuiHelper.isGlfwBindingDown(this.key);
      } else {
         long window = Minecraft.getInstance().getWindow().getWindow();
         return this.key < 0 ? GLFW.glfwGetMouseButton(window, -this.key - 1) != 0 : GLFW.glfwGetKey(window, this.key) != 0;
      }
   }

   public boolean isPressed(boolean repeat) {
      return this.areAllModifiersDown() && this.isPressedIgnoreMods(repeat);
   }

   public boolean isPressedIgnoreMods(boolean repeat) {
      if (this.key == 0) {
         return false;
      } else if (EditorUI.isActive() && EditorUI.isImGuiContextActive()) {
         return ImGuiHelper.isGlfwBindingClicked(this.key, repeat);
      } else {
         boolean down = this.isDownIgnoreMods();
         boolean wasDown = this.ingameDownLastTime;
         this.ingameDownLastTime = down;
         return down && !wasDown;
      }
   }

   public boolean areAllModifiersDown() {
      if (this.key == 0) {
         return false;
      } else {
         if (EditorUI.isActive() && EditorUI.isImGuiContextActive()) {
            ImGuiIO io = EditorUI.getIO();
            if (!isShift(this.key) && this.shiftMod != io.getKeyShift()) {
               return false;
            }

            if (!isCtrl(this.key) && this.ctrlMod != io.getKeyCtrl()) {
               return false;
            }

            if (!isAlt(this.key) && this.altMod != io.getKeyAlt()) {
               return false;
            }

            if (!isSuper(this.key) && this.superMod != io.getKeySuper()) {
               return false;
            }
         } else {
            boolean ctrlMod = this.ctrlMod;
            boolean superMod = this.superMod;
            if (Minecraft.ON_OSX) {
               ctrlMod = this.superMod;
               superMod = this.ctrlMod;
            }

            Minecraft minecraft = Minecraft.getInstance();
            long window = minecraft.getWindow().getWindow();
            if (!isShift(this.key) && this.shiftMod != isShiftDownGLFW(window)) {
               return false;
            }

            if (!isCtrl(this.key) && ctrlMod != isCtrlDownGLFW(window)) {
               return false;
            }

            if (!isAlt(this.key) && this.altMod != isAltDownGLFW(window)) {
               return false;
            }

            if (!isSuper(this.key) && superMod != isSuperDownGLFW(window)) {
               return false;
            }
         }

         return true;
      }
   }

   public String getDescriptionRaw() {
      return this.description;
   }

   public String getDescription() {
      return AxiomI18n.get("axiom.keybinds." + this.description);
   }

   public Component getKeyMessage() {
      return Component.literal(this.longKeyIdentifier());
   }

   public void setKey(LatticeInputType type, int value, boolean shiftMod, boolean ctrlMod, boolean altMod, boolean superMod) {
      int oldKey = this.key;
      switch (type) {
         case KEYSYM:
            this.key = value;
            break;
         case SCANCODE:
            return;
         case MOUSE:
            this.key = -value - 1;
      }

      this.shiftMod = shiftMod;
      this.ctrlMod = Minecraft.ON_OSX ? superMod : ctrlMod;
      this.altMod = altMod;
      this.superMod = Minecraft.ON_OSX ? ctrlMod : superMod;
      Keybinds.updateMapping(this, oldKey);
   }

   public void setUnbound() {
      int oldKey = this.key;
      this.key = 0;
      this.shiftMod = this.ctrlMod = this.altMod = this.superMod = false;
      Keybinds.updateMapping(this, oldKey);
   }

   public Collection<Component> getConflicts() {
      return List.of();
   }
}
