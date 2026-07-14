package com.moulberry.axiom.screen;

import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CustomCreativeTabButton extends AbstractButton {
   private static final ResourceLocation ACTIVE = ResourceLocation.parse("axiom:custom_tab_active");
   private static final ResourceLocation INACTIVE = ResourceLocation.parse("axiom:custom_tab_inactive");
   private final Runnable onPress;
   private final ItemStack displayItemStack;

   public CustomCreativeTabButton(int x, int y, int w, int h, Component component, ItemStack displayItemStack, Runnable onPress) {
      super(x, y, w, h, component);
      this.displayItemStack = displayItemStack;
      this.onPress = onPress;
   }

   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }

   public void onPress() {
      this.onPress.run();
   }

   protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      ResourceLocation location = this.active ? INACTIVE : ACTIVE;
      VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, location, this.getX(), this.getY(), this.getWidth(), this.getHeight());
      guiGraphics.renderFakeItem(this.displayItemStack, this.getX() + this.getWidth() / 2 - 8, this.getY() + this.getHeight() / 2 - 8);
      if (this.isHovered()) {
         guiGraphics.renderTooltip(Minecraft.getInstance().font, this.getMessage(), mouseX, mouseY);
      }
   }
}
