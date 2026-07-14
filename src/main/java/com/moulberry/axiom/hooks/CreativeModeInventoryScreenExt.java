package com.moulberry.axiom.hooks;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.CreativeModeTab;

public interface CreativeModeInventoryScreenExt {
   void axiom$renderTabButton(GuiGraphics var1, int var2, int var3, CreativeModeTab var4, boolean var5);

   boolean axiom$checkTabHovering(GuiGraphics var1, CreativeModeTab var2, int var3, int var4);

   boolean axiom$checkTabClicked(CreativeModeTab var1, double var2, double var4);

   void axiom$selectTab(CreativeModeTab var1);
}
