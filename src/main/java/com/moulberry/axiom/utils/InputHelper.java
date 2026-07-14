package com.moulberry.axiom.utils;

import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class InputHelper {
   private static final boolean ON_OSX = Util.getPlatform() == OS.OSX;
   public static final int EDIT_SHORTCUT_KEY_LEFT = ON_OSX ? 343 : 341;
   public static final int EDIT_SHORTCUT_KEY_RIGHT = ON_OSX ? 347 : 345;

   public static boolean isCtrlOrCmdDownRaw() {
      long window = Minecraft.getInstance().getWindow().getWindow();
      return GLFW.glfwGetKey(window, EDIT_SHORTCUT_KEY_LEFT) != 0 || GLFW.glfwGetKey(window, EDIT_SHORTCUT_KEY_RIGHT) != 0;
   }
}
