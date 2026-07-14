package com.moulberry.axiom.editor.windows;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.ButtonSpacingSpec;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import imgui.moulberry92.ImGui;
import java.time.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class InventoryWindow {
   private static final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private static final Cache<ItemStack, CustomBlockState> itemStackCache = Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(1L)).build();

   public static void render(ActiveBlockHistory activeBlockHistory, BlockList blockList) {
      if (EditorWindowType.INVENTORY.isOpen()) {
         int buttonSize = (int)(28.0F * EditorUI.getUiScale());
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            if (!Axiom.configuration.internal.dockedInventoryWithPalette && EditorWindowType.PALETTE.isOpen()) {
               Axiom.configuration.internal.dockedInventoryWithPalette = true;
               if (PaletteWindow.lastDockId != Integer.MIN_VALUE) {
                  ImGui.setNextWindowDockID(PaletteWindow.lastDockId);
               }
            }

            if (EditorWindowType.INVENTORY.begin("###Inventory", true)) {
               ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.inventory.hotbar"));
               float availX = (float)Math.floor(ImGui.getContentRegionAvailX());
               availX = Math.max(availX, (float)(buttonSize * 4));
               ButtonSpacingSpec spec = ButtonSpacingSpec.calculate(availX, buttonSize, 9, 6.0F, false);
               CustomBlockState toSetActive = null;
               float currentX = ImGui.getCursorPosX();
               float originalPositionX = currentX;
               float currentY = ImGui.getCursorPosY();
               int rowCount = 0;

               for (int id = 0; id < 36; id++) {
                  if (id == 9) {
                     ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.inventory.inventory"));
                     currentX = ImGui.getCursorPosX();
                     originalPositionX = currentX;
                     currentY = ImGui.getCursorPosY();
                     rowCount = 0;
                  }

                  ItemStack inventoryItem;
                  if (EditorUI.inventoryOverrides.containsKey(id)) {
                     inventoryItem = (ItemStack)EditorUI.inventoryOverrides.get(id);
                  } else {
                     inventoryItem = player.getInventory().getItem(id);
                  }

                  CustomBlockState customBlockState = (CustomBlockState)itemStackCache.get(inventoryItem, InventoryWindow::getCustomBlockStateForItemStack);
                  ImGui.setCursorPos(currentX, currentY);
                  if (ImGuiHelper.blockStateButton(customBlockState, id, buttonSize)) {
                     toSetActive = customBlockState;
                  }

                  if (ImGui.isItemClicked(1)) {
                     selectBlockWidget.open(id);
                  }

                  selectBlockWidget.render(AxiomI18n.get("axiom.widget.select_block"), blockList, id);
                  CustomBlockState result = selectBlockWidget.getResultState();
                  if (result != null) {
                     EditorUI.inventoryOverrides.put(id, result.getCustomBlock().axiom$asItemStack());
                  }

                  if (ImGui.isItemHovered()) {
                     ImGui.setMouseCursor(7);
                  }

                  ImGuiHelper.blockStateDragDropSource(customBlockState);
                  if (++rowCount == spec.buttonsPerRow()) {
                     currentX = originalPositionX;
                     currentY += buttonSize + spec.spacingY();
                     rowCount = 0;
                  } else {
                     currentX += buttonSize + spec.spacingX();
                  }
               }

               if (toSetActive != null) {
                  activeBlockHistory.setActive(toSetActive);
               }
            }

            EditorWindowType.INVENTORY.end();
         }
      }
   }

   private static CustomBlockState getCustomBlockStateForItemStack(ItemStack inventoryItem) {
      CustomBlock customBlock = ServerCustomBlocks.getFromItemStack(inventoryItem);
      CustomBlockState customBlockState = customBlock.axiom$defaultCustomState();
      BlockState vanillaState = customBlockState.getVanillaState();
      BlockState updatedVanillaState = ItemStackDataHelper.updateBlockStateFromTag(vanillaState, inventoryItem);
      return vanillaState != updatedVanillaState ? (CustomBlockState)updatedVanillaState : customBlockState;
   }
}
