package com.moulberry.axiom.utils;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;

public class WindowSizeTracker {
   private static int lastFramebufferWidth;
   private static int lastFramebufferHeight;
   private static int realFramebufferWidth;
   private static int realFramebufferHeight;

   public static int getWidth(Window window) {
      if (lastFramebufferWidth != window.framebufferWidth) {
         recalculate(window);
      }

      return realFramebufferWidth;
   }

   public static int getHeight(Window window) {
      if (lastFramebufferHeight != window.framebufferHeight) {
         recalculate(window);
      }

      return realFramebufferHeight;
   }

   public static void dirty() {
      lastFramebufferWidth = 0;
      lastFramebufferHeight = 0;
   }

   private static void recalculate(Window window) {
      int[] width = new int[1];
      int[] height = new int[1];
      GLFW.glfwGetFramebufferSize(window.getWindow(), width, height);
      realFramebufferWidth = width[0] > 0 ? width[0] : 1;
      realFramebufferHeight = height[0] > 0 ? height[0] : 1;
      lastFramebufferWidth = window.framebufferWidth;
      lastFramebufferHeight = window.framebufferHeight;
   }
}
