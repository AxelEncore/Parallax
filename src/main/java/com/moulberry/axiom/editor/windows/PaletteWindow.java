package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.ButtonSpacingSpec;
import com.moulberry.axiom.editor.DragDropPayloads;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.editor.palette.EditorPaletteTombstone;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.type.ImBoolean;
import imgui.moulberry92.type.ImString;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PaletteWindow {
   private static final ImString editorPaletteNameBuffer = new ImString(32);
   private static final int EDITOR_PALETTE_POPUP_HEADER = -1;
   private static final int EDITOR_PALETTE_POPUP_BODY = -2;
   private static int editorPalettePopupType = -1;
   private static int editorPaletteInsertCategoryOffset = 0;
   private static final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   public static int lastDockId = Integer.MIN_VALUE;

   public static void render(ActiveBlockHistory activeBlockHistory, BlockList blockList) {
      if (EditorWindowType.PALETTE.isOpen()) {
         int buttonSize = (int)(32.0F * EditorUI.getUiScale());
         if (EditorWindowType.PALETTE.begin("###Palette", true)) {
            if (!Axiom.configuration.internal.dockedInventoryWithPalette) {
               lastDockId = ImGui.getWindowDockID();
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.palette.recently_used"));
            List<CustomBlockState> blockStates = activeBlockHistory.getRecentlyUsed();
            ButtonSpacingSpec spec = ButtonSpacingSpec.calculate(ImGui.getContentRegionAvailX(), buttonSize, blockStates.size(), 8.0F, false);
            int id = 0;
            CustomBlockState toSetActive = null;
            float currentX = ImGui.getCursorPosX();
            float currentY = ImGui.getCursorPosY();

            for (CustomBlockState blockState : blockStates) {
               ImGui.setCursorPos(currentX, currentY);
               if (ImGuiHelper.blockStateButton(blockState, id, buttonSize)) {
                  toSetActive = blockState;
               }

               if (ImGui.isItemHovered()) {
                  ImGui.setMouseCursor(7);
               }

               ImGuiHelper.blockStateDragDropSource(blockState);
               if (++id == spec.buttonsPerRow()) {
                  break;
               }

               currentX += buttonSize + spec.spacingX();
            }

            if (toSetActive != null) {
               activeBlockHistory.setActive(toSetActive);
            }

            if (Axiom.configuration.internal.rootEditorPalette == null) {
               Axiom.configuration.internal.rootEditorPalette = new EditorPalette("");
            } else {
               Axiom.configuration.internal.rootEditorPalette.reloadIfNeeded();
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.palette.custom"));
            renderCustomChild(activeBlockHistory, blockList);
         }

         EditorWindowType.PALETTE.end();
      }
   }

   private static void renderCustomChild(ActiveBlockHistory activeBlockHistory, BlockList blockList) {
      ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY() - 4.0F);
      if (ImGui.beginChild("Custom")) {
         ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY() + 4.0F);
         List<EditorPalette> categories = Axiom.configuration.internal.rootEditorPalette.getSubcategories();
         if (!categories.isEmpty()) {
            ImBoolean lightBackground = new ImBoolean(true);

            for (EditorPalette child : categories) {
               renderEditorPalette(child, lightBackground, activeBlockHistory, blockList);
            }
         } else {
            ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.palette.custom_empty"));
         }

         ImVec2 cursorPos = ImGui.getCursorScreenPos();
         ImGui.setCursorScreenPos(ImGui.getWindowPosX(), cursorPos.y);
         ImGui.dummy(ImGui.getWindowSizeX(), ImGui.getContentRegionAvailY());
         if (ImGui.beginDragDropTarget()) {
            EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class, 3072);
            if (droppedPalette != null) {
               ImGui.getWindowDrawList()
                  .addRect(ImGui.getWindowPosX(), cursorPos.y, ImGui.getWindowPosX() + ImGui.getWindowSizeX(), cursorPos.y + 1.0F, ImGui.getColorU32(55));
               if (ImGui.isMouseReleased(0)) {
                  droppedPalette.remove();
                  Axiom.configuration.internal.rootEditorPalette.addSubcategory(droppedPalette, EditorPalette.OFFSET_AS_LAST_CHILD);
               }
            }

            ImGui.endDragDropTarget();
         }

         ImGui.setCursorScreenPos(cursorPos.x, cursorPos.y);
         if (!ImGui.isPopupOpen("", 3072) && ImGui.isWindowHovered() && ImGui.isMouseClicked(1)) {
            ImGui.openPopup("EditorPaletteEditRoot");
         }

         String addNewCategory = AxiomI18n.get("axiom.editorui.window.palette.add_new_category");
         boolean openAddCategory = false;
         if (ImGuiHelper.beginPopup("EditorPaletteEditRoot")) {
            if (ImGui.button(addNewCategory)) {
               openAddCategory = true;
               ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
         }

         if (openAddCategory) {
            ImGui.openPopup("###EditorPaletteRootAddCategory");
         }

         float width = (256.0F - ImGui.getStyle().getItemSpacingX()) / 2.0F;
         ImVec2 center = ImGui.getMainViewport().getCenter();
         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(addNewCategory + "###EditorPaletteRootAddCategory", 64)) {
            String error = AxiomI18n.get("axiom.widget.error");
            ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
            if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameHashtag", 64)) {
               ImGui.textColored(-16776961, AxiomI18n.get("axiom.editorui.window.palette.invalid_name_contains_hash"));
               if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
                  openAddCategory = true;
                  ImGui.closeCurrentPopup();
               }

               ImGuiHelper.endPopupModalCloseable();
            }

            ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
            if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameDuplicate", 64)) {
               String message = AxiomI18n.get("axiom.editorui.window.palette.invalid_name_duplicate", ImGuiHelper.getString(editorPaletteNameBuffer));
               ImGui.textColored(-16776961, message);
               if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
                  openAddCategory = true;
                  ImGui.closeCurrentPopup();
               }

               ImGuiHelper.endPopupModalCloseable();
            }

            ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
            if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameBlank", 64)) {
               ImGui.textColored(-16776961, AxiomI18n.get("axiom.editorui.window.palette.invalid_name_blank"));
               if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
                  openAddCategory = true;
                  ImGui.closeCurrentPopup();
               }

               ImGuiHelper.endPopupModalCloseable();
            }

            if (openAddCategory) {
               ImGui.setKeyboardFocusHere();
            }

            ImGui.inputText("###Name", editorPaletteNameBuffer, openAddCategory ? 4096 : 0);
            if (ImGui.button(AxiomI18n.get("axiom.widget.confirm"), width, 0.0F) || ImGui.isKeyPressed(525)) {
               String name = ImGuiHelper.getString(editorPaletteNameBuffer);
               if (name.isBlank()) {
                  ImGui.openPopup("###InvalidNameBlank");
               } else if (name.contains("#")) {
                  ImGui.openPopup("###InvalidNameHashtag");
               } else {
                  EditorPalette subcategory;
                  if ((subcategory = Axiom.configuration.internal.rootEditorPalette.createSubcategory(name)) != null) {
                     subcategory.addBlock((CustomBlockState)Blocks.STONE.defaultBlockState());
                     ImGui.closeCurrentPopup();
                  } else {
                     ImGui.openPopup("###InvalidNameDuplicate");
                  }
               }
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"), width, 0.0F)) {
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }
      }

      ImGui.endChild();
   }

   private static void renderEditorPalette(EditorPalette palette, ImBoolean lightBackground, ActiveBlockHistory activeBlockHistory, BlockList blockList) {
      palette.reloadIfNeeded();
      float itemSpacingY = ImGui.getStyle().getItemSpacingY();
      ImGui.pushID(palette.getName());
      boolean isWindowHovered = ImGui.isWindowHovered();
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      boolean isPopupOpen = ImGui.isPopupOpen("EditorPaletteEdit");
      List<CustomBlockStateOrTombstone> blocks = palette.getBlocks();
      int buttonSize = (int)(32.0F * EditorUI.getUiScale());
      ButtonSpacingSpec spec = ButtonSpacingSpec.calculate(
         ImGui.getContentRegionAvailX() - ImGui.getStyle().getIndentSpacing(), buttonSize, blocks.size(), 8.0F, false
      );
      lightBackground.set(!lightBackground.get());
      ImGui.getStateStorage().setInt(ImGui.getID(palette.getName()), palette.isOpen() ? 1 : 0);
      boolean treeNodeOpened = ImGui.treeNodeEx(palette.getName(), 4096 | (isPopupOpen ? 1 : 0));
      palette.setOpen(treeNodeOpened);
      float yBelowTreeNode = ImGui.getCursorScreenPosY() - itemSpacingY;
      boolean treeNodePopupOpen = ImGui.isItemClicked(1);
      int clickedBlockPopupOpen = -1;
      float maxY = ImGui.getCursorScreenPosY();
      if (treeNodeOpened && blocks.size() > 0) {
         maxY += itemSpacingY;
         maxY += (float)Math.ceil((float)blocks.size() / spec.buttonsPerRow()) * (buttonSize + spec.spacingY());
         maxY -= spec.spacingY();
      }

      if (ImGui.beginDragDropSource()) {
         ImGui.setDragDropPayload(palette);
         ImGui.text(palette.getName());
         ImGui.endDragDropSource();
      }

      if (ImGui.beginDragDropTarget()) {
         DragDropPayloads.PaletteBlock data = (DragDropPayloads.PaletteBlock)ImGui.acceptDragDropPayload(DragDropPayloads.PaletteBlock.class, 3072);
         if (data != null) {
            ImVec2 min = ImGui.getItemRectMin();
            ImVec2 max = ImGui.getItemRectMax();
            ImGui.getForegroundDrawList().addRect(min.x - 3.5F, min.y - 3.5F, max.x + 3.5F, max.y + 3.5F, ImGui.getColorU32(55), 0.0F, 0, 2.0F);
            if (ImGui.isMouseReleased(0)) {
               data.paletteFrom().removeBlock(data.indexFrom());
               palette.addBlock(data.state());
            }
         } else {
            EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class, 3072);
            if (droppedPalette != null) {
               ImVec2 min = ImGui.getItemRectMin();
               ImVec2 max = ImGui.getItemRectMax();
               ImGui.getForegroundDrawList().addRect(min.x - 3.5F, min.y - 3.5F, max.x + 3.5F, max.y + 3.5F, ImGui.getColorU32(55), 0.0F, 0, 2.0F);
               if (ImGui.isMouseReleased(0) && droppedPalette != palette && !droppedPalette.isParentOf(palette)) {
                  droppedPalette.remove();
                  palette.addSubcategory(droppedPalette);
               }
            }
         }

         ImGui.endDragDropTarget();
      }

      if (treeNodeOpened) {
         boolean includeHeader = true;
         int backgroundColour;
         if (isPopupOpen) {
            includeHeader = false;
            backgroundColour = ImGui.getColorU32(24);
         } else if (lightBackground.get()) {
            backgroundColour = ImGui.getColorU32(51);
         } else {
            backgroundColour = ImGui.getColorU32(50);
         }

         if ((backgroundColour & 0xFF000000) != 0) {
            ImGui.getWindowDrawList()
               .addRectFilled(
                  ImGui.getWindowPosX(),
                  includeHeader ? y : ImGui.getCursorScreenPosY() - itemSpacingY,
                  ImGui.getWindowPosX() + ImGui.getWindowSizeX(),
                  maxY,
                  backgroundColour
               );
         }

         int buttonIndexInRow = 0;
         float startX = ImGui.getCursorScreenPosX();
         float currentX = startX;
         float currentY = ImGui.getCursorScreenPosY();
         CustomBlockState blockToAdd = null;
         int removeBlockIndex = -1;
         int addBlockIndex = -1;
         int id = 0;
         BlockState tombstoneBlock = Blocks.BARRIER.defaultBlockState();

         for (CustomBlockStateOrTombstone blockStateOrTombstone : blocks) {
            ImGui.setCursorScreenPos(currentX, currentY);
            if (blockStateOrTombstone instanceof EditorPaletteTombstone tombstone) {
               ImGuiHelper.blockStateButton((CustomBlockState)tombstoneBlock, id, buttonSize);
               if (ImGui.isItemClicked(1)) {
                  clickedBlockPopupOpen = id;
               }

               if (ImGui.isItemHovered()) {
                  ImGui.beginTooltip();
                  ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0F);
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.palette.unknown_custom_block"));
                  ImGui.text(tombstone.key());
                  ImGui.popTextWrapPos();
                  ImGui.endTooltip();
               }
            } else if (blockStateOrTombstone instanceof CustomBlockState blockState) {
               if (ImGuiHelper.blockStateButton(blockState, id, buttonSize)) {
                  activeBlockHistory.setActive(blockState);
               }

               if (ImGui.isItemClicked(1)) {
                  clickedBlockPopupOpen = id;
               }

               if (ImGui.isItemHovered()) {
                  ImGui.beginTooltip();
                  ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0F);
                  ImGui.text(AxiomI18n.get(blockState.getCustomBlock().axiom$translationKey()));
                  ImGui.text(ServerCustomBlocks.serialize(blockState));
                  ImGui.popTextWrapPos();
                  ImGui.endTooltip();
               }

               if (ImGui.beginDragDropSource()) {
                  ImGui.setDragDropPayload(new DragDropPayloads.PaletteBlock(blockState, palette, id));
                  ImGuiHelper.drawBlockState(ImGui.getForegroundDrawList(), blockState, ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY(), buttonSize);
                  ImGui.dummy(buttonSize, buttonSize);
                  ImGui.endDragDropSource();
               }

               Object dragDropResult = editorPaletteDragDrop(spec, currentX, currentY, id == blocks.size() - 1);
               int addOffset;
               if (dragDropResult == null && buttonIndexInRow == 0) {
                  dragDropResult = editorPaletteDragDrop(spec, currentX - buttonSize - spec.spacingX(), currentY, false);
                  addOffset = 0;
               } else {
                  addOffset = 1;
               }

               if (dragDropResult instanceof DragDropPayloads.PaletteBlock paletteBlock) {
                  if (paletteBlock.paletteFrom() == palette) {
                     removeBlockIndex = paletteBlock.indexFrom();
                  } else {
                     paletteBlock.paletteFrom().removeBlock(paletteBlock.indexFrom());
                  }

                  blockToAdd = paletteBlock.state();
                  addBlockIndex = id + addOffset;
               } else if (dragDropResult instanceof CustomBlockState dragBlockState) {
                  blockToAdd = dragBlockState;
                  addBlockIndex = id + addOffset;
               }
            }

            if (buttonIndexInRow + 1 >= spec.buttonsPerRow()) {
               currentX = startX;
               currentY += buttonSize + spec.spacingY();
               buttonIndexInRow = 0;
            } else {
               currentX += buttonSize + spec.spacingX();
               buttonIndexInRow++;
            }

            id++;
         }

         if (removeBlockIndex >= 0) {
            palette.removeBlock(removeBlockIndex);
         }

         if (blockToAdd != null && addBlockIndex >= 0) {
            if (removeBlockIndex >= 0 && removeBlockIndex < addBlockIndex) {
               palette.addBlock(addBlockIndex - 1, blockToAdd);
            } else {
               palette.addBlock(addBlockIndex, blockToAdd);
            }
         }

         for (EditorPalette child : palette.getSubcategories()) {
            renderEditorPalette(child, lightBackground, activeBlockHistory, blockList);
         }

         if (!blocks.isEmpty() || palette.getSubcategoryCount() > 0) {
            ImGui.getWindowDrawList()
               .addLine(
                  x + ImGui.getFontSize() / 2.0F + ImGui.getStyle().getFramePaddingX(),
                  y + ImGui.getTextLineHeight(),
                  x + ImGui.getFontSize() / 2.0F + ImGui.getStyle().getFramePaddingX(),
                  ImGui.getCursorScreenPosY(),
                  ImGui.getColorU32(1)
               );
         }

         ImGui.treePop();
      } else if (lightBackground.get()) {
         ImGui.getWindowDrawList().addRectFilled(ImGui.getWindowPosX(), y, ImGui.getWindowPosX() + ImGui.getWindowSizeX(), maxY, ImGui.getColorU32(51));
      }

      ImVec2 cursorPos = ImGui.getCursorScreenPos();
      ImGui.setCursorScreenPos(ImGui.getWindowPosX(), yBelowTreeNode);
      ImGui.dummy(ImGui.getWindowSizeX(), maxY - 4.0F - yBelowTreeNode);
      if (ImGui.beginDragDropTarget()) {
         EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class, 3072);
         if (droppedPalette != null) {
            float highlightX = x;
            if (palette.getSubcategoryCount() > 0) {
               highlightX = x + ImGui.getStyle().getIndentSpacing();
            }

            ImGui.getWindowDrawList().addRect(highlightX, maxY - 1.0F, x + ImGui.getContentRegionAvailX(), maxY, ImGui.getColorU32(55));
            if (ImGui.isMouseReleased(0) && droppedPalette != palette && !droppedPalette.isParentOf(palette)) {
               if (palette.getSubcategoryCount() == 0) {
                  droppedPalette.remove();
                  palette.addSubcategory(droppedPalette, EditorPalette.OFFSET_NEXT);
               } else {
                  droppedPalette.remove();
                  palette.addSubcategory(droppedPalette, EditorPalette.OFFSET_AS_FIRST_CHILD);
               }
            }
         }

         ImGui.endDragDropTarget();
      }

      ImGui.setCursorScreenPos(ImGui.getWindowPosX(), y - 4.0F);
      ImGui.dummy(ImGui.getWindowSizeX(), 4.0F);
      if (ImGui.beginDragDropTarget()) {
         EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class, 3072);
         if (droppedPalette != null) {
            ImGui.getWindowDrawList().addRect(x, y + 1.0F, x + ImGui.getContentRegionAvailX(), y + 2.0F, ImGui.getColorU32(55));
            if (ImGui.isMouseReleased(0) && droppedPalette != palette && !droppedPalette.isParentOf(palette)) {
               droppedPalette.remove();
               palette.addSubcategory(droppedPalette, EditorPalette.OFFSET_PREVIOUS);
            }
         }

         ImGui.endDragDropTarget();
      }

      ImGui.setCursorScreenPos(cursorPos.x, cursorPos.y);
      if (treeNodePopupOpen) {
         editorPalettePopupType = -1;
         ImGui.openPopup("EditorPaletteEdit");
      } else if (clickedBlockPopupOpen >= 0) {
         editorPalettePopupType = clickedBlockPopupOpen;
         ImGui.openPopup("EditorPaletteEdit");
      } else if (isWindowHovered && ImGui.isMouseClicked(1) && ImGui.getMousePosY() > y && ImGui.getMousePosY() < maxY) {
         editorPalettePopupType = -2;
         ImGui.openPopup("EditorPaletteEdit");
      }

      String quoted = "'" + palette.getName() + "'";
      float width = (256.0F - ImGui.getStyle().getItemSpacingX()) / 2.0F;
      ImVec2 center = ImGui.getMainViewport().getCenter();
      boolean firstOpen = renderEditPopup(palette, blockList, quoted);
      renderRenameModal(palette, quoted, width, firstOpen, center);
      renderConfirmDeleteModal(palette, quoted, width, center);
      renderAddBlockModal(palette, blockList, quoted, center);
      renderAddCategoryModal(palette, width, firstOpen, center);
      ImGui.popID();
   }

   private static boolean renderEditPopup(EditorPalette palette, BlockList blockList, String quoted) {
      int operation = -1;
      if (ImGuiHelper.beginPopup("EditorPaletteEdit")) {
         if (editorPalettePopupType == -1) {
            ImGui.text(palette.getName());
            ImGui.separator();
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.add_new_block"))) {
               operation = 0;
            }

            if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.window.palette.add_new_category"))) {
               if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.before", quoted))) {
                  operation = 1;
               }

               if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.child_of", quoted))) {
                  operation = 2;
               }

               if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.after", quoted))) {
                  operation = 3;
               }

               ImGui.endMenu();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.rename", quoted))) {
               operation = 4;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.delete", quoted))) {
               operation = 5;
            }
         } else if (editorPalettePopupType == -2) {
            ImGui.text(palette.getName());
            ImGui.separator();
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.add_new_block"))) {
               operation = 0;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.add_new_category"))) {
               operation = 2;
            }
         } else {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.add_new_block"))) {
               operation = 0;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.palette.remove_block"))) {
               palette.removeBlock(editorPalettePopupType);
            }
         }

         ImGui.endPopup();
      }

      switch (operation) {
         case 0:
            blockList.search("");
            selectBlockWidget.open();
            break;
         case 1:
         case 2:
         case 3:
            editorPaletteInsertCategoryOffset = EditorPalette.OFFSET_AS_LAST_CHILD;
            if (operation == 1) {
               editorPaletteInsertCategoryOffset = EditorPalette.OFFSET_PREVIOUS;
            }

            if (operation == 3) {
               editorPaletteInsertCategoryOffset = EditorPalette.OFFSET_NEXT;
            }

            editorPaletteNameBuffer.set("");
            ImGui.openPopup("###EditorPaletteAddCategory");
            break;
         case 4:
            editorPaletteNameBuffer.set(palette.getName());
            ImGui.openPopup("###EditorPaletteRename");
            break;
         case 5:
            ImGui.openPopup("###EditorPaletteDeleteConfirm");
      }

      return operation >= 0;
   }

   private static void renderRenameModal(EditorPalette palette, String quoted, float width, boolean firstOpen, ImVec2 center) {
      ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
      String rename = AxiomI18n.get("axiom.editorui.window.palette.rename", quoted);
      if (ImGuiHelper.beginPopupModalCloseable(rename + "###EditorPaletteRename", 320)) {
         String error = AxiomI18n.get("axiom.widget.error");
         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameHashtag", 64)) {
            ImGui.textColored(-16776961, AxiomI18n.get("axiom.editorui.window.palette.invalid_name_contains_hash"));
            if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
               firstOpen = true;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameDuplicate", 64)) {
            String message = AxiomI18n.get("axiom.editorui.window.palette.invalid_name_duplicate", ImGuiHelper.getString(editorPaletteNameBuffer));
            ImGui.textColored(-16776961, message);
            if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
               firstOpen = true;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameBlank", 64)) {
            ImGui.textColored(-16776961, AxiomI18n.get("axiom.editorui.window.palette.invalid_name_blank"));
            if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
               firstOpen = true;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         if (firstOpen) {
            ImGui.setKeyboardFocusHere();
         }

         ImGui.inputText("###Name", editorPaletteNameBuffer, firstOpen ? 4096 : 0);
         if (ImGui.button(AxiomI18n.get("axiom.widget.confirm"), width, 0.0F) || ImGui.isKeyPressed(525)) {
            String value = ImGuiHelper.getString(editorPaletteNameBuffer).trim();
            if (value.equals(palette.getName())) {
               ImGui.closeCurrentPopup();
            } else if (value.isBlank()) {
               ImGui.openPopup("###InvalidNameBlank");
            } else if (value.contains("#")) {
               ImGui.openPopup("###InvalidNameHashtag");
            } else if (palette.rename(value)) {
               ImGui.closeCurrentPopup();
            } else {
               ImGui.openPopup("###InvalidNameDuplicate");
            }
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"), width, 0.0F)) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   private static void renderConfirmDeleteModal(EditorPalette palette, String quoted, float width, ImVec2 center) {
      ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
      String delete = AxiomI18n.get("axiom.editorui.window.palette.delete", quoted);
      if (ImGuiHelper.beginPopupModalCloseable(delete + "###EditorPaletteDeleteConfirm", 64)) {
         ImGui.text(AxiomI18n.get("axiom.editorui.window.palette.are_you_sure_delete", palette.getName()));
         if (palette.getSubcategoryCount() > 0) {
            ImGui.text(AxiomI18n.get("axiom.editorui.window.palette.this_will_delete_children", palette.getSubcategoryCount()));
         }

         ImGui.text(AxiomI18n.get("axiom.editorui.window.palette.cannot_be_undone"));
         if (ImGui.button(AxiomI18n.get("axiom.widget.confirm"), width, 0.0F)) {
            palette.remove();
            ImGui.closeCurrentPopup();
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"), width, 0.0F)) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   private static void renderAddBlockModal(EditorPalette palette, BlockList blockList, String quoted, ImVec2 center) {
      ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
      selectBlockWidget.render(AxiomI18n.get("axiom.editorui.window.palette.add_block_to", quoted), blockList);
      CustomBlockState state = selectBlockWidget.getResultState();
      if (state != null) {
         if (editorPalettePopupType >= 0) {
            palette.addBlock(editorPalettePopupType + 1, state);
         } else {
            palette.addBlock(state);
         }
      }
   }

   private static void renderAddCategoryModal(EditorPalette palette, float width, boolean firstOpen, ImVec2 center) {
      String addNewCategory = AxiomI18n.get("axiom.editorui.window.palette.add_new_category");
      ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
      if (ImGuiHelper.beginPopupModalCloseable(addNewCategory + "###EditorPaletteAddCategory", 64)) {
         String error = AxiomI18n.get("axiom.widget.error");
         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameHashtag", 64)) {
            ImGui.textColored(-16776961, AxiomI18n.get("axiom.editorui.window.palette.invalid_name_contains_hash"));
            if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
               firstOpen = true;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameDuplicate", 64)) {
            String message = AxiomI18n.get("axiom.editorui.window.palette.invalid_name_duplicate", ImGuiHelper.getString(editorPaletteNameBuffer));
            ImGui.textColored(-16776961, message);
            if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
               firstOpen = true;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(error + "###InvalidNameBlank", 64)) {
            ImGui.textColored(-16776961, AxiomI18n.get("axiom.editorui.window.palette.invalid_name_blank"));
            if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
               firstOpen = true;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         if (firstOpen) {
            ImGui.setKeyboardFocusHere();
         }

         ImGui.inputText("###Name", editorPaletteNameBuffer, firstOpen ? 4096 : 0);
         if (ImGui.button(AxiomI18n.get("axiom.widget.confirm"), width, 0.0F) || ImGui.isKeyPressed(525)) {
            String name = ImGuiHelper.getString(editorPaletteNameBuffer);
            if (name.isBlank()) {
               ImGui.openPopup("###InvalidNameBlank");
            } else if (name.contains("#")) {
               ImGui.openPopup("###InvalidNameHashtag");
            } else {
               EditorPalette subcategory;
               if ((subcategory = palette.createSubcategory(name, editorPaletteInsertCategoryOffset)) != null) {
                  subcategory.addBlock((CustomBlockState)Blocks.STONE.defaultBlockState());
                  ImGui.closeCurrentPopup();
               } else {
                  ImGui.openPopup("###InvalidNameDuplicate");
               }
            }
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"), width, 0.0F)) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   private static Object editorPaletteDragDrop(ButtonSpacingSpec spec, float currentX, float currentY, boolean extend) {
      Object returnValue = null;
      int buttonSize = (int)(32.0F * EditorUI.getUiScale());
      ImVec2 cursorPos = ImGui.getCursorScreenPos();
      ImGui.setCursorScreenPos(currentX + 16.0F, currentY - spec.spacingY() / 2.0F);
      ImGui.dummy(extend ? ImGui.getContentRegionAvailX() : buttonSize + spec.spacingX(), buttonSize + spec.spacingY());
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (ImGui.beginDragDropTarget()) {
         DragDropPayloads.PaletteBlock data = (DragDropPayloads.PaletteBlock)ImGui.acceptDragDropPayload(DragDropPayloads.PaletteBlock.class, 3072);
         if (data != null) {
            ImGui.getWindowDrawList()
               .addRect(
                  currentX + buttonSize + spec.spacingX() / 2.0F,
                  currentY - 2.0F,
                  currentX + buttonSize + 1.0F + spec.spacingX() / 2.0F,
                  currentY + buttonSize + 3.0F,
                  ImGui.getColorU32(55)
               );
            if (ImGui.isMouseReleased(0)) {
               returnValue = data;
            }
         } else {
            returnValue = ImGuiHelper.blockStateDragDropTargetInner(
               () -> ImGui.getWindowDrawList()
                  .addRect(
                     currentX + buttonSize + spec.spacingX() / 2.0F,
                     currentY - 2.0F,
                     currentX + buttonSize + 1.0F + spec.spacingX() / 2.0F,
                     currentY + buttonSize + 3.0F,
                     ImGui.getColorU32(55)
                  )
            );
         }

         ImGui.endDragDropTarget();
      }

      ImGui.setCursorScreenPos(cursorPos.x, cursorPos.y);
      return returnValue;
   }
}
