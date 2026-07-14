package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.hooks.CreativeModeInventoryScreenExt;
import com.moulberry.axiom.screen.CreativeColourScreen;
import com.moulberry.axiom.screen.CreativeGradientScreen;
import com.moulberry.axiom.screen.CustomCreativeTabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CreativeModeInventoryScreen.class})
public abstract class MixinCreativeModeInventoryScreen extends AbstractContainerScreen<ItemPickerMenu> implements CreativeModeInventoryScreenExt {
   private static final ResourceLocation RETURN = ResourceLocation.parse("axiom:return");
   @Shadow
   private static CreativeModeTab selectedTab;
   @Shadow
   private EditBox searchBox;
   @Shadow
   private boolean ignoreTextInput;
   @Unique
   private boolean forceDeselected = false;

   @Shadow
   protected abstract void renderTabButton(GuiGraphics var1, CreativeModeTab var2);

   @Shadow
   protected abstract boolean checkTabHovering(GuiGraphics var1, CreativeModeTab var2, int var3, int var4);

   @Shadow
   protected abstract boolean checkTabClicked(CreativeModeTab var1, double var2, double var4);

   @Shadow
   protected abstract void selectTab(CreativeModeTab var1);

   public MixinCreativeModeInventoryScreen(ItemPickerMenu abstractContainerMenu, Inventory inventory, Component component) {
      super(abstractContainerMenu, inventory, component);
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
         shift = Shift.BEFORE
      )}
   )
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive() && Axiom.configuration.creativeMenu.searchQuickAdd) {
         if (selectedTab.getType() == Type.SEARCH
            && this.searchBox.isFocused()
            && this.searchBox.isVisible()
            && !this.searchBox.getValue().isEmpty()
            && !((ItemPickerMenu)(Object)this.menu).items.isEmpty()) {
            VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, RETURN, this.leftPos + 157, this.topPos + 4, 14, 12);
            if (mouseX >= this.leftPos + 157 && mouseY >= this.topPos + 4 && mouseX <= this.leftPos + 171 && mouseY <= this.topPos + 16) {
               guiGraphics.renderTooltip(this.font, Component.translatable("axiom.creative_search_quick_add"), mouseX, mouseY);
            }
         }
      }
   }

   @Inject(
      method = {"keyPressed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void keyPressed(int key, int j, int k, CallbackInfoReturnable<Boolean> cir) {
      if (AxiomClient.isAxiomActive() && Axiom.configuration.creativeMenu.searchQuickAdd) {
         if (key == 257
            && selectedTab.getType() == Type.SEARCH
            && this.searchBox.isFocused()
            && this.searchBox.isVisible()
            && !this.searchBox.getValue().isEmpty()
            && !((ItemPickerMenu)(Object)this.menu).items.isEmpty()) {
            ItemStack itemStack = (ItemStack)((ItemPickerMenu)(Object)this.menu).items.get(0);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
               itemStack = itemStack.copy();
               Inventory inventory = player.getInventory();
               inventory.setPickedItem(itemStack);
               Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
               Minecraft.getInstance().setScreen(null);
               cir.setReturnValue(true);
            }
         }
      }
   }

   @Inject(
      method = {"init"},
      at = {@At("RETURN")}
   )
   public void init(CallbackInfo ci) {
      if (this.minecraft != null && this.minecraft.player.hasInfiniteMaterials() && Minecraft.getInstance().screen == (Object)this && AxiomClient.isAxiomActive()) {
         if (Axiom.configuration.creativeMenu.showColorPicker) {
            this.addRenderableWidget(
               new CustomCreativeTabButton(
                  this.leftPos - 36,
                  this.topPos + 30,
                  26,
                  26,
                  Component.translatable("axiom.color_picker"),
                  new ItemStack(Items.YELLOW_DYE),
                  () -> Minecraft.getInstance().setScreen(new CreativeColourScreen(this.minecraft.player, (CreativeModeInventoryScreen)(Object)this))
               )
            );
         }

         if (Axiom.configuration.creativeMenu.showGradientHelper) {
            this.addRenderableWidget(
               new CustomCreativeTabButton(
                  this.leftPos - 36,
                  this.topPos + 30 + 36,
                  26,
                  26,
                  Component.translatable("axiom.gradient_helper"),
                  new ItemStack(Items.BRUSH),
                  () -> Minecraft.getInstance().setScreen(new CreativeGradientScreen(this.minecraft.player, (CreativeModeInventoryScreen)(Object)this))
               )
            );
         }
      }
   }

   @Override
   public void axiom$renderTabButton(GuiGraphics guiGraphics, int mouseX, int mouseY, CreativeModeTab creativeModeTab, boolean forceDeselected) {
      this.forceDeselected = forceDeselected;
      this.renderTabButton(guiGraphics, creativeModeTab);
      this.forceDeselected = false;
   }

   @Override
   public boolean axiom$checkTabHovering(GuiGraphics guiGraphics, CreativeModeTab creativeModeTab, int mouseX, int mouseY) {
      return this.checkTabHovering(guiGraphics, creativeModeTab, mouseX, mouseY);
   }

   @Override
   public boolean axiom$checkTabClicked(CreativeModeTab creativeModeTab, double mouseX, double mouseY) {
      return this.checkTabClicked(creativeModeTab, mouseX, mouseY);
   }

   @Override
   public void axiom$selectTab(CreativeModeTab creativeModeTab) {
      this.ignoreTextInput = true;
      this.selectTab(creativeModeTab);
   }

   @WrapOperation(
      method = {"renderTabButton"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;selectedTab:Lnet/minecraft/world/item/CreativeModeTab;"
      )}
   )
   public CreativeModeTab renderTabButton_selectedTab(Operation<CreativeModeTab> original) {
      return this.forceDeselected ? null : (CreativeModeTab)original.call(new Object[0]);
   }
}
