package com.moulberry.axiom;

import com.moulberry.axiom.screen.SwitchBuilderToolScreen;
import com.moulberry.axiom.screen.SwitchHotbarScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class ContextMenuManager {
   private static final ContextMenuManager INSTANCE = new ContextMenuManager();
   private int lastScreenWidth;
   private int lastScreenHeight = 0;
   private Screen activeScreen;

   public static ContextMenuManager getInstance() {
      return INSTANCE;
   }

   public void open(Screen screen) {
      if (this.activeScreen == null) {
         Minecraft.getInstance().mouseHandler.releaseMouse();
      }

      Minecraft.getInstance().options.keyUse.setDown(false);
      Minecraft.getInstance().options.keyAttack.setDown(false);
      this.activeScreen = screen;
      this.activeScreen.init(Minecraft.getInstance(), this.lastScreenWidth, this.lastScreenHeight);
   }

   public void close() {
      if (this.activeScreen != null) {
         this.activeScreen.onClose();
         this.activeScreen = null;
         Minecraft.getInstance().mouseHandler.grabMouse();
      }
   }

   public boolean isActive() {
      return this.activeScreen != null;
   }

   public Screen getActiveScreen() {
      return this.activeScreen;
   }

   public void slotSelected(int index) {
      if (this.activeScreen instanceof SwitchHotbarScreen switchHotbarScreen) {
         switchHotbarScreen.slotSelected(index);
      }

      if (this.activeScreen instanceof SwitchBuilderToolScreen switchBuilderToolScreen) {
         switchBuilderToolScreen.slotSelected(index);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.activeScreen.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   public void mouseClicked(double mx, double my, int button) {
      this.activeScreen.mouseClicked(mx, my, button);
   }

   public void mouseReleased(double mx, double my, int button) {
      this.activeScreen.mouseReleased(mx, my, button);
   }

   public void mouseDragged(double mx, double my, int button, double dx, double dy) {
      this.activeScreen.mouseDragged(mx, my, button, dx, dy);
   }

   public void mouseMoved(double mx, double my) {
      this.activeScreen.mouseMoved(mx, my);
   }

   public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      if (this.activeScreen != null) {
         if (this.lastScreenWidth != screenWidth || this.lastScreenHeight != screenHeight) {
            this.activeScreen.resize(Minecraft.getInstance(), screenWidth, screenHeight);
         }

         Minecraft client = Minecraft.getInstance();
         int mx = (int)(client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth());
         int my = (int)(client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight());
         this.activeScreen.render(guiGraphics, mx, my, partialTick);
      }

      this.lastScreenWidth = screenWidth;
      this.lastScreenHeight = screenHeight;
   }
}
