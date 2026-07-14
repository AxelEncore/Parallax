package com.moulberry.axiom.editor;

import com.moulberry.axiom.ExpressionEvaluator;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.downgrade.BlockVersionCompatibility;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.BlockRenderCache;
import com.moulberry.axiom.utils.OkLabColourUtils;
import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiListClipper;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.callback.ImListClipperCallback;
import imgui.moulberry92.type.ImBoolean;
import imgui.moulberry92.type.ImString;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImGuiHelper {
   private static boolean allowAcceptDragDropPayloadThisFrame = true;
   private static boolean closeableModalOnTopLast = false;
   private static boolean closeableModalOnTop = false;
   private static boolean inputIntScrolledLastFrame = false;
   private static boolean inputIntScrolledThisFrame = false;
   private static boolean wantSpecialInputLastFrame = false;
   private static boolean wantSpecialInputThisFrame = false;
   private static StringBuilder specialInput = new StringBuilder();
   private static int backspaceCount = 0;
   private static Keybind editingKeybindLastFrame = null;
   private static Keybind editingKeybindThisFrame = null;
   private static boolean handledFocusNext = false;
   private static boolean focusNext = false;
   public static float borderIndentation = 0.0F;
   private static int pushedColors = 0;
   private static int pushedStyleVars = 0;

   public static ImGuiHelper.Label translateLabel(String key) {
      String title = AxiomI18n.get(key);
      String descriptionKey = key + ".description";
      String description = AxiomI18n.get(descriptionKey);
      if (description.equals(descriptionKey)) {
         description = null;
      }

      return new ImGuiHelper.Label(key, title, description);
   }

   public static ImString createResizableString(int initialSize) {
      ImString string = new ImString("", initialSize);
      string.inputData.isResizable = true;
      return string;
   }

   public static void drawBlockState(ImDrawList drawList, CustomBlockState state, float x, float y, float size) {
      if (state != null) {
         if (!BlockVersionCompatibility.isCompatible(state.getVanillaState().getBlock())) {
            drawList.addRectFilled(x, y, x + size, y + size, -16776961);
         }

         float dpiScale = ImGui.getWindowDpiScale();
         int img = BlockRenderCache.request(state, Math.round(size * dpiScale), Math.round(size * dpiScale), false);
         if (img != -1) {
            drawList.addImage(img, x, y, x + size, y + size, 0.0F, 1.0F, 1.0F, 0.0F);
         }
      }
   }

   public static String getString(ImString string) {
      StringBuilder builder = new StringBuilder();
      String str = string.get();

      for (int i = 0; i < str.length(); i++) {
         char c = str.charAt(i);
         if (c == 0) {
            break;
         }

         builder.append(c);
      }

      return builder.toString();
   }

   public static CustomBlockState blockScrollList(List<BlockList.Entry> blocks, int width, int height, boolean firstOpen) {
      if (ImGui.beginChild("##BlockScroller", width, height, true)) {
         if (firstOpen) {
            ImGui.setScrollY(0.0F);
         }

         float available = ImGui.getContentRegionAvailX() + ImGui.getStyle().getItemSpacingX();
         float itemWidth = (int)(32.0F * EditorUI.getUiScale()) + ImGui.getStyle().getItemSpacingX();
         final int count = (int)Math.floor(available / itemWidth);
         final AtomicReference<CustomBlockState> clickedBlock = new AtomicReference<>(null);
         ImGuiListClipper.forEach(Mth.positiveCeilDiv(blocks.size(), count), new ImListClipperCallback() {
            public void accept(int index) {
               int from = index * count;
               int to = Math.min(from + count, blocks.size());

               for (int i = from; i < to; i++) {
                  BlockList.Entry entry = blocks.get(i);
                  if (i != from) {
                     ImGui.sameLine();
                  }

                  if (ImGuiHelper.blockStateButton(entry.state(), i, (int)(32.0F * EditorUI.getUiScale()))) {
                     clickedBlock.set(entry.state());
                  }

                  if (ImGui.isItemHovered()) {
                     ImGui.beginTooltip();
                     ImGui.text(AxiomI18n.get(entry.translationKey()));
                     ImGui.textDisabled(entry.location().toString());
                     ImGui.endTooltip();
                  }
               }
            }
         });
         if (clickedBlock.get() != null) {
            ImGui.endChild();
            return clickedBlock.get();
         }
      }

      ImGui.endChild();
      return null;
   }

   public static BlockList.MinecraftOrCustomTagSet tagScrollList(List<BlockList.TagEntry> tags, int width, int height, boolean firstOpen) {
      if (ImGui.beginChild("##TagScroller", width, height, true)) {
         if (firstOpen) {
            ImGui.setScrollY(0.0F);
         }

         final AtomicReference<BlockList.MinecraftOrCustomTagSet> clickedTag = new AtomicReference<>(null);
         ImGuiListClipper.forEach(
            tags.size(),
            new ImListClipperCallback() {
               public void accept(int index) {
                  BlockList.TagEntry entry = tags.get(index);
                  BlockList.MinecraftOrCustomTagSet tagSet = entry.tag();
                  int minecraftSize = 0;
                  int customSize = 0;
                  if (tagSet.minecraft() != null) {
                     minecraftSize = tagSet.minecraft().size();
                  }

                  if (tagSet.customList() != null) {
                     customSize = tagSet.customList().size();
                  }

                  int size = minecraftSize + customSize;
                  if (ImGui.button(tagSet.name().toString() + " (" + size + ")", -1.0F, 0.0F)) {
                     clickedTag.set(tagSet);
                  }

                  if (ImGui.isItemHovered()) {
                     ImGui.beginTooltip();
                     if (size <= 9) {
                        for (int i = 0; i < size; i++) {
                           Block block;
                           if (i < minecraftSize) {
                              block = (Block)tagSet.minecraft().get(i).value();
                           } else {
                              ResourceLocation blockLocation = tagSet.customList().get(i - minecraftSize);
                              block = (Block)BuiltInRegistries.BLOCK.get(blockLocation);
                           }

                           float posX = ImGui.getCursorScreenPosX();
                           float posY = ImGui.getCursorScreenPosY();
                           ImGui.dummy(32.0F, 32.0F);
                           ImGuiHelper.drawBlockState(ImGui.getWindowDrawList(), (CustomBlockState)block.defaultBlockState(), posX, posY, 32.0F);
                           if (i != 2 && i != 5) {
                              ImGui.sameLine();
                           }
                        }
                     } else {
                        float hSpacing = 32.0F + ImGui.getStyle().getItemSpacingX();
                        float vSpacing = 32.0F + ImGui.getStyle().getItemSpacingY();
                        int verticalCount = (size + 2) / 3;
                        long time = System.currentTimeMillis();
                        int scroll = (int)(time / 500L);
                        float partial = (float)(time - time / 500L * 500L) / 500.0F;
                        scroll %= verticalCount;
                        if (scroll < 0) {
                           scroll += verticalCount;
                        }

                        for (int i = 0; i < 15; i++) {
                           int j = scroll * 3 + i - 3;
                           if (j < 0) {
                              j += verticalCount * 3;
                           }

                           if (j / 3 >= verticalCount) {
                              j -= verticalCount * 3;
                           }

                           if (j < size) {
                              Block blockx;
                              if (j < minecraftSize) {
                                 blockx = (Block)tagSet.minecraft().get(j).value();
                              } else {
                                 ResourceLocation blockLocation = tagSet.customList().get(j - minecraftSize);
                                 blockx = (Block)BuiltInRegistries.BLOCK.get(blockLocation);
                              }

                              float posX = ImGui.getCursorScreenPosX() + hSpacing * (i % 3);
                              float posY = ImGui.getCursorScreenPosY() + vSpacing * (i / 3 - partial);
                              if (j == 0) {
                                 ImGui.getWindowDrawList()
                                    .addLine(
                                       ImGui.getCursorScreenPosX(),
                                       posY - 3.0F,
                                       ImGui.getCursorScreenPosX() + hSpacing * 3.0F,
                                       posY - 3.0F,
                                       ImGui.getColorU32(27)
                                    );
                              }

                              ImGuiHelper.drawBlockState(ImGui.getWindowDrawList(), (CustomBlockState)blockx.defaultBlockState(), posX, posY, 32.0F);
                           }
                        }

                        ImGui.dummy(hSpacing * 3.0F - ImGui.getStyle().getItemSpacingX(), vSpacing * 3.0F - ImGui.getStyle().getItemSpacingY());
                     }

                     ImGui.endTooltip();
                  }
               }
            }
         );
         ImGui.endChild();
         if (clickedTag.get() != null) {
            return clickedTag.get();
         }
      }

      return null;
   }

   public static void colorPicker3WithBlockDrop(String label, float[] values) {
      ImGui.colorPicker3(label, values);
      CustomBlockState blockState = blockStateDragDropTarget();
      if (blockState != null) {
         Vec3 lab = BlockColourMap.getLab(blockState.getCustomBlock());
         if (lab != null) {
            int rgb = OkLabColourUtils.lab2rgb(lab.x, lab.y, lab.z);
            values[0] = (rgb >> 16 & 0xFF) / 255.0F;
            values[1] = (rgb >> 8 & 0xFF) / 255.0F;
            values[2] = (rgb & 0xFF) / 255.0F;
         }
      }
   }

   public static CustomBlockState blockStateWidget(SelectBlockWidget selectBlockWidget, CustomBlockState state, String label, int id) {
      int size = (int)(32.0F * EditorUI.getUiScale());
      ImVec2 originalPos = ImGui.getCursorScreenPos();
      boolean blockButtonClick = blockStateButton(state, id, size);
      CustomBlockState dragDropped = blockStateDragDrop(state);
      if (dragDropped != null) {
         state = dragDropped;
      }

      String blockText = state.getCustomBlock().axiom$getIdentifier().toString();
      String text = label == null ? blockText : label + " (" + blockText + ")";
      ImGui.getWindowDrawList()
         .addText(
            originalPos.x + size + ImGui.getStyle().getItemSpacingX(), originalPos.y + (size - ImGui.getTextLineHeight()) / 2.0F, ImGui.getColorU32(0), text
         );
      if (blockButtonClick) {
         selectBlockWidget.open(id);
      }

      selectBlockWidget.render(AxiomI18n.get("axiom.widget.select_block"), EditorUI.getBlockList(), id);
      CustomBlockState result = selectBlockWidget.getResultState();
      if (result != null) {
         state = result;
      }

      return state;
   }

   public static boolean blockStateButton(CustomBlockState state, int id, int size) {
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      ImGui.pushID(id);
      boolean pressed = ImGui.button("", size, size);
      if (size > 4 && ImGui.isItemVisible()) {
         drawBlockState(ImGui.getWindowDrawList(), state, x + 2.0F, y + 2.0F, size - 4);
      }

      ImGui.popID();
      return pressed;
   }

   public static int blockStateButtonLeftOrRightClick(CustomBlockState state, int id, int size) {
      int pressed = 0;
      float x = ImGui.getCursorScreenPosX();
      float y = ImGui.getCursorScreenPosY();
      ImGui.pushID(id);
      if (ImGui.button("", size, size)) {
         pressed |= 1;
      }

      if (ImGui.isItemClicked(1)) {
         pressed |= 2;
      }

      if (size > 4 && ImGui.isItemVisible()) {
         drawBlockState(ImGui.getWindowDrawList(), state, x + 2.0F, y + 2.0F, size - 4);
      }

      ImGui.popID();
      return pressed;
   }

   public static void blockStateDragDropSource(CustomBlockState state) {
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (ImGui.beginDragDropSource()) {
         ImGui.setDragDropPayload("BlockState", state);
         drawBlockState(ImGui.getForegroundDrawList(), state, ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY(), 32.0F);
         ImGui.dummy(32.0F, 32.0F);
         ImGui.endDragDropSource();
      }
   }

   public static CustomBlockState blockStateDragDropTarget(Runnable customRender) {
      CustomBlockState ret = null;
      if (ImGui.beginDragDropTarget()) {
         ret = blockStateDragDropTargetInner(customRender);
         ImGui.endDragDropTarget();
      }

      return ret;
   }

   public static CustomBlockState blockStateDragDropTargetInner(Runnable customRender) {
      if (!allowAcceptDragDropPayloadThisFrame) {
         return null;
      } else {
         allowAcceptDragDropPayloadThisFrame = false;
         int flags = customRender == null ? 0 : 3072;
         CustomBlockState droppedBlock = (CustomBlockState)ImGui.acceptDragDropPayload("BlockState", flags);
         if (droppedBlock != null) {
            if (customRender != null) {
               customRender.run();
               droppedBlock = (CustomBlockState)ImGui.acceptDragDropPayload("BlockState", 2048);
            }

            return droppedBlock;
         } else {
            DragDropPayloads.PaletteBlock droppedPaletteBlock = (DragDropPayloads.PaletteBlock)ImGui.acceptDragDropPayload(
               DragDropPayloads.PaletteBlock.class, flags
            );
            if (droppedPaletteBlock != null) {
               if (customRender != null) {
                  customRender.run();
                  droppedPaletteBlock = (DragDropPayloads.PaletteBlock)ImGui.acceptDragDropPayload(DragDropPayloads.PaletteBlock.class, 2048);
               }

               return droppedPaletteBlock != null ? droppedPaletteBlock.state() : null;
            } else {
               DragDropPayloads.NoisePainterBlock noisePainterBlock = (DragDropPayloads.NoisePainterBlock)ImGui.acceptDragDropPayload(
                  "NoisePainterBlock", flags
               );
               if (noisePainterBlock != null) {
                  if (customRender != null) {
                     customRender.run();
                     noisePainterBlock = (DragDropPayloads.NoisePainterBlock)ImGui.acceptDragDropPayload("NoisePainterBlock", 2048);
                  }

                  return noisePainterBlock != null ? noisePainterBlock.block().blockState() : null;
               } else {
                  return null;
               }
            }
         }
      }
   }

   public static CustomBlockState blockStateDragDropTarget() {
      return blockStateDragDropTarget(null);
   }

   public static CustomBlockState blockStateDragDrop(CustomBlockState state) {
      blockStateDragDropSource(state);
      return blockStateDragDropTarget();
   }

   public static int elementList(String name, List<String> elements, float width, int minLines, int maxLines, boolean framed, IntConsumer buttonConsumer) {
      if (minLines > maxLines) {
         throw new IllegalArgumentException();
      } else {
         if (width <= 0.0F) {
            width = Math.max(ImGui.getContentRegionAvailX() + width, 4.0F);
         }

         float itemSpacingX = ImGui.getStyle().getItemSpacingX();
         float framePaddingX = ImGui.getStyle().getFramePaddingX();
         float availableSpace = width;
         if (framed) {
            availableSpace = width - framePaddingX * 2.0F;
         }

         int lines = 0;
         boolean scrollbar = false;
         if (minLines == maxLines) {
            lines = minLines;
            scrollbar = true;
         } else {
            float consumedWidth = 0.0F;

            for (String element : elements) {
               float elementWidth = framePaddingX * 2.0F + calcTextWidth(element);
               if (consumedWidth + elementWidth < availableSpace && consumedWidth > 0.0F) {
                  consumedWidth += elementWidth + itemSpacingX;
               } else {
                  consumedWidth = elementWidth + itemSpacingX;
                  if (++lines > maxLines) {
                     lines = maxLines;
                     scrollbar = true;
                     break;
                  }
               }
            }

            if (lines < minLines) {
               lines = minLines;
            }
         }

         float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0F + ImGui.getStyle().getItemSpacingY();
         boolean visible;
         if (framed) {
            visible = ImGui.beginChild(
               ImGui.getID(name), width, lineHeight * lines + ImGui.getStyle().getFramePaddingY() * 2.0F - ImGui.getStyle().getItemSpacingY(), 128
            );
         } else {
            visible = ImGui.beginChild(name, width, lineHeight * lines - ImGui.getStyle().getItemSpacingY());
         }

         int ret = -1;
         if (visible) {
            if (scrollbar) {
               availableSpace -= ImGui.getStyle().getScrollbarSize();
            }

            float consumedWidth = 0.0F;

            for (int i = 0; i < elements.size(); i++) {
               String elementx = elements.get(i);
               float elementWidth = framePaddingX * 2.0F + calcTextWidth(elementx);
               if (consumedWidth + elementWidth < availableSpace && consumedWidth > 0.0F) {
                  ImGui.sameLine();
                  consumedWidth += elementWidth + itemSpacingX;
               } else {
                  consumedWidth = elementWidth + itemSpacingX;
               }

               ImGui.pushID(i);
               if (ImGui.button(elementx)) {
                  ret = i;
               }

               if (buttonConsumer != null) {
                  buttonConsumer.accept(i);
               }

               ImGui.popID();
            }
         }

         ImGui.endChild();
         return ret;
      }
   }

   public static boolean combo(String label, int[] currentItem, String[] values) {
      return combo(label, currentItem, values, 0);
   }

   public static boolean combo(String label, int[] currentItem, String[] values, int imguiComboFlags) {
      boolean changed = false;
      if (ImGui.beginCombo(label, currentItem[0] < 0 ? "" : values[currentItem[0]], imguiComboFlags)) {
         for (int i = 0; i < values.length; i++) {
            ImGui.pushID(i);
            boolean selected = i == currentItem[0];
            if (ImGui.selectable(values[i], selected) && !selected) {
               currentItem[0] = i;
               changed = true;
            }

            if (selected) {
               ImGui.setItemDefaultFocus();
            }

            ImGui.popID();
         }

         ImGui.endCombo();
      }

      return changed;
   }

   public static void endFrame() {
      closeableModalOnTopLast = closeableModalOnTop;
      inputIntScrolledLastFrame = inputIntScrolledThisFrame;
      inputIntScrolledThisFrame = false;
      wantSpecialInputLastFrame = wantSpecialInputThisFrame;
      wantSpecialInputThisFrame = false;
      editingKeybindLastFrame = editingKeybindThisFrame;
      editingKeybindThisFrame = null;
      handledFocusNext = false;
      focusNext = false;
      allowAcceptDragDropPayloadThisFrame = true;
      if (!wantSpecialInputLastFrame) {
         specialInput.setLength(0);
      }
   }

   public static boolean isGlfwBindingDown(int key) {
      if (key == 0) {
         return false;
      } else if (key < 0) {
         int mouse = -key - 1;
         return mouse >= 5 ? false : ImGui.isMouseDown(mouse);
      } else {
         int namedKey = CustomImGuiImplGlfw.glfwKeyToImGuiKey(key);
         return namedKey >= 512 && namedKey < 667 ? ImGui.isKeyDown(namedKey) : false;
      }
   }

   public static boolean isGlfwBindingClicked(int key, boolean repeat) {
      if (key == 0) {
         return false;
      } else if (key < 0) {
         int mouse = -key - 1;
         return mouse >= 5 ? false : ImGui.isMouseClicked(mouse, repeat);
      } else {
         int namedKey = CustomImGuiImplGlfw.glfwKeyToImGuiKey(key);
         return namedKey >= 512 && namedKey < 667 ? ImGui.isKeyPressed(namedKey, repeat) : false;
      }
   }

   public static void setEditingKeybind(Keybind keybind) {
      editingKeybindThisFrame = keybind;
   }

   public static Keybind getEditingKeybind() {
      return editingKeybindLastFrame;
   }

   public static String modifyFromInput(String existing) {
      wantSpecialInputThisFrame = true;
      String newInput = specialInput.toString();
      existing = existing.substring(0, Math.max(0, existing.length() - backspaceCount));
      existing = existing + newInput;
      specialInput.setLength(0);
      backspaceCount = 0;
      return existing;
   }

   public static boolean getWantsSpecialInput() {
      return wantSpecialInputLastFrame;
   }

   public static boolean addInputCharacter(char c) {
      if (wantSpecialInputLastFrame) {
         specialInput.append(c);
         return true;
      } else {
         return false;
      }
   }

   public static boolean backspaceInput(int mods) {
      if (wantSpecialInputLastFrame) {
         if ((mods & 2) != 0) {
            specialInput.setLength(0);
            backspaceCount = 10000;
            return true;
         } else {
            if (specialInput.length() > 0) {
               specialInput.setLength(specialInput.length() - 1);
            } else {
               backspaceCount++;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean beginPopup(String id) {
      return beginPopup(id, 0);
   }

   public static boolean beginPopup(String id, int imGuiWindowFlags) {
      if (ImGui.beginPopup(id, imGuiWindowFlags | 4 | 256)) {
         closeableModalOnTop = false;
         return true;
      } else {
         return false;
      }
   }

   public static boolean beginPopupModal(String id) {
      return beginPopupModal(id, 0);
   }

   public static boolean beginPopupModal(String id, int imGuiWindowFlags) {
      if (EditorUI.isFirstFrame()) {
         return beginPopup(id, imGuiWindowFlags);
      } else if (ImGui.beginPopupModal(id, imGuiWindowFlags | 256)) {
         closeableModalOnTop = false;
         return true;
      } else {
         return false;
      }
   }

   public static boolean beginPopupModalCloseable(String id) {
      return beginPopupModalCloseable(id, 0);
   }

   public static boolean beginPopupModalCloseable(String id, int imGuiWindowFlags) {
      if (EditorUI.isFirstFrame()) {
         return beginPopup(id, imGuiWindowFlags);
      } else if (ImGui.beginPopupModal(id, new ImBoolean(true), imGuiWindowFlags | 256)) {
         closeableModalOnTop = true;
         return true;
      } else {
         return false;
      }
   }

   public static void endPopupModalCloseable() {
      if (closeableModalOnTop && closeableModalOnTopLast && EditorUI.consumeNavClose()) {
         ImGui.closeCurrentPopup();
      }

      ImGui.endPopup();
   }

   public static void setupBorder() {
      ImGui.beginGroup();
      float windowPadding = ImGui.getStyle().getWindowPaddingX();
      borderIndentation += windowPadding;
      ImGui.indent(windowPadding);
      ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getStyle().getWindowPaddingY());
   }

   public static void finishBorder() {
      float windowPadding = ImGui.getStyle().getWindowPaddingX();
      borderIndentation -= windowPadding;
      ImGui.unindent(windowPadding);
      ImGui.endGroup();
      ImGui.getWindowDrawList()
         .addRect(
            ImGui.getItemRectMinX(),
            ImGui.getItemRectMinY(),
            ImGui.getItemRectMinX() + ImGui.getContentRegionAvailX(),
            ImGui.getItemRectMaxY() + ImGui.getStyle().getWindowPaddingY(),
            ImGui.getColorU32(27)
         );
      ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getStyle().getWindowPaddingY());
   }

   public static float calcTextWidth(String text) {
      ImVec2 textSizeVec = new ImVec2();
      ImGui.calcTextSize(textSizeVec, text);
      return textSizeVec.x;
   }

   public static ImGuiHelper.LabelPosition calcLabelPosition(ImGuiHelper.Label... labels) {
      float availableSpace = ImGui.getWindowWidth() * 0.35F - ImGui.getCursorPosX() - ImGui.getStyle().getItemInnerSpacingX();

      for (ImGuiHelper.Label label : labels) {
         float requiredSpace = calcTextWidth(label.title) + ImGui.getStyle().getWindowPaddingX();
         if (label.description != null) {
            requiredSpace += ImGui.getStyle().getItemSpacingX() + calcTextWidth("(?)");
         }

         if (availableSpace < requiredSpace) {
            return ImGuiHelper.LabelPosition.LABEL_ABOVE;
         }
      }

      return ImGuiHelper.LabelPosition.LABEL_RIGHT;
   }

   public static void helpMarker(String message) {
      ImGui.textDisabled("(?)");
      tooltip(message);
   }

   public static void tooltip(String message) {
      tooltip(message, 0);
   }

   public static void openCommercialLicenseOnClick() {
      if (ImGui.isItemClicked()) {
         String page = "https://axiom.moulberry.com/commercial";
         Minecraft.getInstance().setScreen(new ConfirmLinkScreen(open -> {
            if (open) {
               try {
                  Util.getPlatform().openUri(new URI(page));
               } catch (Exception var3) {
               }
            }

            Minecraft.getInstance().setScreen(null);
         }, page, false));
      }
   }

   public static void tooltip(String message, int imGuiHoveredFlags) {
      if (ImGui.isItemHovered(imGuiHoveredFlags)) {
         ImGui.beginTooltip();
         ImGui.pushTextWrapPos(ImGui.getFontSize() * 35.0F);
         ImGui.textUnformatted(message);
         ImGui.popTextWrapPos();
         ImGui.endTooltip();
      }
   }

   public static void disabledMenuItem(String label, String disabledMessage) {
      ImGui.beginDisabled();
      ImGui.menuItem(label);
      ImGui.endDisabled();
      tooltip(disabledMessage, 1024);
   }

   public static boolean drawLabelledWidget(ImGuiHelper.Label label, ImGuiHelper.LabelPosition style, Predicate<String> predicate) {
      boolean ret = false;
      switch (style) {
         case LABEL_ABOVE:
            ImGui.text(label.title);
            if (label.description != null) {
               ImGui.sameLine();
               helpMarker(label.description);
            }

            ret = predicate.test("###" + label.key);
            break;
         case LABEL_RIGHT:
            ret = predicate.test(label.title + "###" + label.key);
            if (label.description != null) {
               ImGui.sameLine();
               helpMarker(label.description);
            }
      }

      return ret;
   }

   public static void separatorWithText(String text) {
      float textStartX = ImGui.getCursorScreenPosX() + ImGui.getStyle().getIndentSpacing();
      float size = ImGui.getWindowSizeX() - borderIndentation;
      if (ImGui.isRectVisible(size, ImGui.getFontSize())) {
         float textEndX = textStartX + calcTextWidth(text);
         float lineEndX = ImGui.getWindowPosX() + size;
         float lineY = ImGui.getCursorScreenPosY() + ImGui.getFontSize() / 2.0F;
         ImGui.getWindowDrawList().addLine(ImGui.getCursorScreenPosX() - 4.0F, lineY, Math.min(lineEndX, textStartX) - 4.0F, lineY, ImGui.getColorU32(27));
         if (textEndX + 4.0F < lineEndX) {
            ImGui.getWindowDrawList().addLine(textEndX + 4.0F, lineY, lineEndX - 4.0F, lineY, ImGui.getColorU32(27));
         }
      }

      ImGui.setCursorScreenPos(textStartX, ImGui.getCursorScreenPosY());
      ImGui.textColored(ImGui.getColorU32(1), text);
   }

   public static boolean inputInt(String label, int[] value) {
      return inputInt(label, value, false);
   }

   public static boolean inputInt(String label, int[] value, boolean tryUseRemainingSpace) {
      if (value.length == 0) {
         ImGui.text(label);
         return false;
      } else {
         boolean canScroll = ImGui.getScrollMaxY() <= 0.0F;
         boolean valueChanged = false;
         float availableWidth = ImGui.calcItemWidth();
         if (tryUseRemainingSpace) {
            availableWidth -= (ImGui.getCursorPosX() - ImGui.getStyle().getWindowPaddingX()) * 2.0F / 3.0F;
         }

         float innerSpacing = ImGui.getStyle().getItemInnerSpacingX();
         float widthItemOne = Math.max(1.0F, (float)Math.floor((availableWidth - innerSpacing * (value.length - 1)) / value.length));
         float widthItemLast = Math.max(1.0F, (float)Math.floor(availableWidth - (widthItemOne + innerSpacing) * (value.length - 1)));
         ImGui.beginGroup();
         ImGui.pushID(label);

         for (int i = 0; i < value.length; i++) {
            ImGui.pushID(i);
            if (i > 0) {
               ImGui.sameLine(0.0F, innerSpacing);
            }

            ImString imString = new ImString(value[i] + "", 100);
            if (i < value.length - 1) {
               ImGui.setNextItemWidth(widthItemOne);
            } else {
               ImGui.setNextItemWidth(widthItemLast);
            }

            if (focusNext) {
               ImGui.setKeyboardFocusHere();
               focusNext = false;
            }

            int flags = 4097;
            if (inputIntScrolledLastFrame) {
               flags |= 512;
            }

            if (ImGui.inputText("", imString, flags)) {
               valueChanged = true;

               try {
                  value[i] = (int)Math.round(ExpressionEvaluator.evaluateFallible(getString(imString)));
               } catch (Exception var13) {
                  if (ImGui.isItemDeactivatedAfterEdit()) {
                     value[i] = 0;
                  }
               }
            }

            if (!handledFocusNext && ImGui.isItemActive() && ImGui.isKeyPressed(512, false)) {
               handledFocusNext = true;
               focusNext = true;
            }

            if (ImGui.isItemHovered() && canScroll) {
               float scrollY = EditorUI.getIO().getMouseWheel();
               if (scrollY > 0.0F) {
                  value[i]++;
                  valueChanged = true;
                  inputIntScrolledThisFrame = true;
               } else if (scrollY < 0.0F) {
                  value[i]--;
                  valueChanged = true;
                  inputIntScrolledThisFrame = true;
               }
            }

            ImGui.popID();
         }

         ImGui.popID();
         String renderedText = label.split("##")[0];
         if (!renderedText.isEmpty()) {
            ImGui.sameLine(0.0F, innerSpacing);
            ImGui.text(renderedText);
         }

         ImGui.endGroup();
         return valueChanged;
      }
   }

   public static boolean inputFloat(String label, float[] value) {
      if (value.length == 0) {
         ImGui.text(label);
         return false;
      } else {
         boolean valueChanged = false;
         float availableWidth = ImGui.calcItemWidth();
         float innerSpacing = ImGui.getStyle().getItemInnerSpacingX();
         float widthItemOne = Math.max(1.0F, (float)Math.floor((availableWidth - innerSpacing * (value.length - 1)) / value.length));
         float widthItemLast = Math.max(1.0F, (float)Math.floor(availableWidth - (widthItemOne + innerSpacing) * (value.length - 1)));
         ImGui.beginGroup();
         ImGui.pushID(label);

         for (int i = 0; i < value.length; i++) {
            ImGui.pushID(i);
            if (i > 0) {
               ImGui.sameLine(0.0F, innerSpacing);
            }

            ImString imString = new ImString(value[i] + "", 100);
            if (i < value.length - 1) {
               ImGui.setNextItemWidth(widthItemOne);
            } else {
               ImGui.setNextItemWidth(widthItemLast);
            }

            int flags = 4097;
            if (inputIntScrolledLastFrame) {
               flags |= 512;
            }

            if (ImGui.inputText("", imString, flags)) {
               valueChanged = true;

               try {
                  value[i] = (float)ExpressionEvaluator.evaluateFallible(getString(imString));
               } catch (Exception var11) {
                  if (ImGui.isItemDeactivatedAfterEdit()) {
                     value[i] = 0.0F;
                  }
               }
            }

            if (ImGui.isItemHovered()) {
               float scrollY = EditorUI.getIO().getMouseWheel();
               if (scrollY > 0.0F) {
                  value[i]++;
                  valueChanged = true;
                  inputIntScrolledThisFrame = true;
               } else if (scrollY < 0.0F) {
                  value[i]--;
                  valueChanged = true;
                  inputIntScrolledThisFrame = true;
               }
            }

            ImGui.popID();
         }

         ImGui.popID();
         String renderedText = label.split("##")[0];
         if (!renderedText.isEmpty()) {
            ImGui.sameLine(0.0F, innerSpacing);
            ImGui.text(renderedText);
         }

         ImGui.endGroup();
         return valueChanged;
      }
   }

   public static boolean radio(String label, int[] currentItem, String[] values) {
      int item = currentItem[0];
      int newItem = -1;

      for (int i = 0; i < values.length; i++) {
         String value = values[i];
         if (i > 0) {
            ImGui.sameLine();
         }

         if (item == i) {
            ImGui.beginDisabled();
         }

         if (ImGui.button(value)) {
            newItem = i;
         }

         if (item == i) {
            ImGui.endDisabled();
         }
      }

      if (label != null) {
         ImGui.sameLine();
         ImGui.text(label);
      }

      if (newItem != -1) {
         currentItem[0] = newItem;
         return true;
      } else {
         return false;
      }
   }

   public static int buttons(String... labels) {
      if (labels.length == 0) {
         return -1;
      } else {
         float[] widths = new float[labels.length];
         float totalRawWidth = 0.0F;

         for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            float width = calcTextWidth(label);
            widths[i] = width;
            totalRawWidth += width;
         }

         float availableWidth = ImGui.getContentRegionAvailX();
         float freeSpace = availableWidth - totalRawWidth - ImGui.getStyle().getItemSpacingX() * (labels.length - 1);
         float extraPadding = Math.max(ImGui.getStyle().getFramePaddingX() * 2.0F, freeSpace / labels.length);
         float height = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0F;
         int pressed = -1;

         for (int i = 0; i < labels.length; i++) {
            if (ImGui.button(labels[i], widths[i] + extraPadding, height)) {
               pressed = i;
            }

            if (i != labels.length - 1) {
               ImGui.sameLine();
            }
         }

         return pressed;
      }
   }

   public static void popAllStyleColors() {
      ImGui.popStyleColor(pushedColors);
      pushedColors = 0;
   }

   public static void popAllStyleVars() {
      ImGui.popStyleVar(pushedStyleVars);
      pushedStyleVars = 0;
   }

   public static void pushStyleColor(int imGuiCol, float r, float g, float b, float a) {
      ImGui.pushStyleColor(imGuiCol, r, g, b, a);
      pushedColors++;
   }

   public static void pushStyleColor(int imGuiCol, int r, int g, int b, int a) {
      ImGui.pushStyleColor(imGuiCol, r, g, b, a);
      pushedColors++;
   }

   public static void pushStyleColor(int imGuiCol, int col) {
      ImGui.pushStyleColor(imGuiCol, col);
      pushedColors++;
   }

   public static void popStyleColor() {
      if (pushedColors >= 1) {
         ImGui.popStyleColor();
         pushedColors--;
      }
   }

   public static void popStyleColor(int count) {
      count = Math.min(count, pushedColors);
      if (count != 0) {
         ImGui.popStyleColor(count);
         pushedColors -= count;
      }
   }

   public static void pushStyleVar(int imGuiStyleVar, float val) {
      ImGui.pushStyleVar(imGuiStyleVar, val);
      pushedStyleVars++;
   }

   public static void pushStyleVar(int imGuiStyleVar, float valX, float valY) {
      ImGui.pushStyleVar(imGuiStyleVar, valX, valY);
      pushedStyleVars++;
   }

   public static void popStyleVar() {
      if (pushedStyleVars >= 1) {
         ImGui.popStyleVar();
         pushedStyleVars--;
      }
   }

   public static void popStyleVar(int count) {
      count = Math.min(count, pushedStyleVars);
      if (count != 0) {
         ImGui.popStyleVar(count);
         pushedStyleVars -= count;
      }
   }

   public record Label(@NotNull String key, @NotNull String title, @Nullable String description) {
   }

   public static enum LabelPosition {
      LABEL_ABOVE,
      LABEL_RIGHT;
   }
}
