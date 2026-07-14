package com.moulberry.axiom.utils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.DragDropPayloads;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.editor.widgets.BlockStateWidget;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public record BlockWithFloat(CustomBlockState blockState, float[] percentage, @Nullable Set<Property<?>> randomProperties) {
   private static DragDropPayloads.NoisePainterBlock DRAG_DROP_PAYLOAD = null;
   private static final List<BlockState> DEFAULT_BLOCKS = List.of(
      Blocks.STONE.defaultBlockState(),
      Blocks.GRANITE.defaultBlockState(),
      Blocks.DIORITE.defaultBlockState(),
      Blocks.ANDESITE.defaultBlockState(),
      Blocks.DIRT.defaultBlockState()
   );
   private static final LoadingCache<BlockWithFloat, CustomBlockState> RANDOMIZED_STATE_CACHE = Caffeine.newBuilder()
      .expireAfterWrite(250L, TimeUnit.MILLISECONDS)
      .build(blockWithFloat -> BlockManipulation.applyRandomProperties(blockWithFloat.blockState, blockWithFloat.randomProperties));

   public static boolean ensureFilledToMinimum(List<BlockWithFloat> blockWithFloats, int min) {
      boolean changed;
      for (changed = false; blockWithFloats.size() < min; changed = true) {
         addNew(blockWithFloats, (CustomBlockState)DEFAULT_BLOCKS.get(blockWithFloats.size() % DEFAULT_BLOCKS.size()));
      }

      return changed;
   }

   public static boolean renderList(
      List<BlockWithFloat> blockThresholds,
      SelectBlockWidget selectBlockWidget,
      BlockWithFloat.ExtraRenderType extraRenderType,
      int min,
      boolean allowRandomProperties
   ) {
      boolean changed = ensureFilledToMinimum(blockThresholds, min);
      float itemWidth = ImGui.calcItemWidth();
      int scaledSize = (int)(32.0F * EditorUI.getUiScale());
      int removeBlockThreshold = -1;
      float spacingY = ImGui.getStyle().getItemSpacingY();
      boolean canRemoveABlockThreshold = blockThresholds.size() > min;
      ImVec2 startPosition = ImGui.getCursorScreenPos();

      for (int i = 0; i < blockThresholds.size(); i++) {
         ImGui.pushID(i);
         BlockWithFloat blockWithFloat = blockThresholds.get(i);
         boolean hasNoisePainterBlockDragDropTarget = false;
         ImVec2 position = ImGui.getCursorScreenPos();
         ImGui.setCursorScreenPos(ImGui.getWindowPosX(), position.y - spacingY);
         ImGui.dummy(itemWidth, scaledSize / 2.0F + spacingY);
         if (ImGui.beginDragDropTarget()) {
            DragDropPayloads.NoisePainterBlock noisePainterBlock = (DragDropPayloads.NoisePainterBlock)ImGui.acceptDragDropPayload("NoisePainterBlock", 3072);
            if (noisePainterBlock != null) {
               hasNoisePainterBlockDragDropTarget = true;
               if (noisePainterBlock.index() != i - 1 && noisePainterBlock.index() != i) {
                  ImGui.getWindowDrawList()
                     .addRectFilled(
                        position.x,
                        position.y - spacingY / 2.0F - 1.0F,
                        position.x + ImGui.getContentRegionAvailX(),
                        position.y - spacingY / 2.0F + 1.0F,
                        ImGui.getColorU32(55)
                     );
                  if (ImGui.isMouseReleased(0)) {
                     if (noisePainterBlock.index() < blockThresholds.size()) {
                        blockThresholds.remove(noisePainterBlock.index());
                     }

                     if (i > 0 && i > noisePainterBlock.index()) {
                        blockThresholds.add(i - 1, noisePainterBlock.block());
                     } else {
                        blockThresholds.add(i, noisePainterBlock.block());
                     }
                  }
               }
            }

            ImGui.endDragDropTarget();
         }

         if (!hasNoisePainterBlockDragDropTarget) {
            ImGui.setCursorScreenPos(ImGui.getWindowPosX(), position.y + scaledSize / 2.0F);
            ImGui.dummy(itemWidth, scaledSize / 2.0F);
            if (ImGui.beginDragDropTarget()) {
               DragDropPayloads.NoisePainterBlock noisePainterBlock = (DragDropPayloads.NoisePainterBlock)ImGui.acceptDragDropPayload("NoisePainterBlock", 3072);
               if (noisePainterBlock != null) {
                  hasNoisePainterBlockDragDropTarget = true;
                  if (noisePainterBlock.index() != i && noisePainterBlock.index() != i + 1) {
                     ImGui.getWindowDrawList()
                        .addRectFilled(
                           position.x,
                           position.y + scaledSize + spacingY / 2.0F - 1.0F,
                           position.x + ImGui.getContentRegionAvailX(),
                           position.y + scaledSize + spacingY / 2.0F + 1.0F,
                           ImGui.getColorU32(55)
                        );
                     if (ImGui.isMouseReleased(0)) {
                        if (noisePainterBlock.index() < blockThresholds.size()) {
                           blockThresholds.remove(noisePainterBlock.index());
                        }

                        if (i + 1 > noisePainterBlock.index()) {
                           blockThresholds.add(i, noisePainterBlock.block());
                        } else {
                           blockThresholds.add(i + 1, noisePainterBlock.block());
                        }
                     }
                  }
               }

               ImGui.endDragDropTarget();
            }
         }

         ImGui.setCursorScreenPos(position.x, position.y);
         CustomBlockState visualBlockState;
         if (blockWithFloat.randomProperties != null) {
            visualBlockState = (CustomBlockState)RANDOMIZED_STATE_CACHE.get(blockWithFloat);
         } else {
            visualBlockState = blockWithFloat.blockState;
         }

         int blockButtonClick = ImGuiHelper.blockStateButtonLeftOrRightClick(visualBlockState, i, scaledSize);
         if (!blockWithFloat.blockState.getProperties().isEmpty()) {
            ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.block_list.right_click_to_edit_properties"));
            if ((blockButtonClick & 2) != 0) {
               ImGui.openPopup("##Properties");
            }

            if (ImGui.beginPopup("##Properties")) {
               BlockStateWidget blockStateWidget = new BlockStateWidget(blockWithFloat.blockState);
               blockStateWidget.allowRandomProperties = allowRandomProperties;
               blockStateWidget.setRandomizedProperties(blockWithFloat.randomProperties);
               ImGui.textDisabled(AxiomI18n.get("axiom.widget.block_properties"));
               blockStateWidget.renderPropertySettings("BlockListProperties", false);
               CustomBlockState newState = blockStateWidget.getBlockState();
               Set<Property<?>> newRandomProperties = allowRandomProperties ? blockStateWidget.getRandomProperties() : null;
               if (newState != blockWithFloat.blockState || !Objects.equals(newRandomProperties, blockWithFloat.randomProperties)) {
                  changed = true;
                  blockThresholds.set(i, new BlockWithFloat(newState, blockWithFloat.percentage(), blockStateWidget.getRandomProperties()));
               }

               ImGui.endPopup();
            }
         }

         if (!hasNoisePainterBlockDragDropTarget) {
            CustomBlockState dragDropped = ImGuiHelper.blockStateDragDropTarget();
            if (dragDropped != null) {
               blockThresholds.set(i, new BlockWithFloat(dragDropped, blockWithFloat.percentage(), null));
            }
         }

         if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(7);
         }

         if (ImGui.beginDragDropSource()) {
            DRAG_DROP_PAYLOAD = new DragDropPayloads.NoisePainterBlock(i, blockWithFloat);
            ImGui.setDragDropPayload("NoisePainterBlock", DRAG_DROP_PAYLOAD);
            ImVec2 originalPos = ImGui.getCursorScreenPos();
            ImGuiHelper.drawBlockState(
               ImGui.getForegroundDrawList(), DRAG_DROP_PAYLOAD.block().blockState(), ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY(), scaledSize
            );

            String text = switch (extraRenderType) {
               case PERCENTAGE -> String.format("%.1f%%", DRAG_DROP_PAYLOAD.block().percentage()[0]);
               case FLOAT -> String.format("%.3f", DRAG_DROP_PAYLOAD.block().percentage()[0] / 100.0F);
               case BLOCKNAME -> DRAG_DROP_PAYLOAD.block().blockState().getCustomBlock().axiom$getIdentifier().toString();
            };
            ImGui.getWindowDrawList()
               .addText(
                  originalPos.x + scaledSize + ImGui.getStyle().getItemSpacingX(),
                  originalPos.y + (scaledSize - ImGui.getTextLineHeight()) / 2.0F,
                  ImGui.getColorU32(0),
                  text
               );
            ImGui.dummy(scaledSize + ImGui.getStyle().getItemSpacingX() + ImGuiHelper.calcTextWidth(text), scaledSize);
            ImGui.endDragDropSource();
         }

         ImGui.sameLine();
         ImGuiHelper.pushStyleVar(11, 4.0F, 8.0F * EditorUI.getUiScale());
         float sliderSize = Math.min(ImGui.getContentRegionAvailX(), itemWidth) - scaledSize - 4.0F;
         switch (extraRenderType) {
            case PERCENTAGE:
               ImGui.setNextItemWidth(sliderSize);
               changed |= ImGui.sliderFloat("##BlockThreshold" + i, blockWithFloat.percentage(), 0.0F, 100.0F, "%.1f%%", 1536);
               break;
            case FLOAT:
               float[] threshold = new float[]{blockWithFloat.percentage()[0] / 100.0F};
               ImGui.setNextItemWidth(sliderSize);
               changed |= ImGui.sliderFloat("##BlockThreshold" + i, threshold, 0.0F, 1.0F, "%.3f", 1536);
               blockWithFloat.percentage()[0] = threshold[0] * 100.0F;
               break;
            case BLOCKNAME:
               String text = blockWithFloat.blockState().getCustomBlock().axiom$getIdentifier().toString();
               ImGui.getWindowDrawList()
                  .addText(
                     position.x + scaledSize + ImGui.getStyle().getItemSpacingX(),
                     position.y + (scaledSize - ImGui.getTextLineHeight()) / 2.0F,
                     ImGui.getColorU32(0),
                     text
                  );
               ImGui.dummy(sliderSize, 0.0F);
         }

         ImGuiHelper.popStyleVar();
         if (canRemoveABlockThreshold) {
            float previousY = ImGui.getCursorPosY();
            ImGui.sameLine();
            if (ImGui.button("-##RemoveBlock" + i, scaledSize, scaledSize)) {
               removeBlockThreshold = i;
            }

            ImGui.setCursorPosY(previousY);
         }

         if ((blockButtonClick & 1) != 0) {
            selectBlockWidget.open(i);
         }

         selectBlockWidget.render(AxiomI18n.get("axiom.widget.select_block"), EditorUI.getBlockList(), i);
         CustomBlockState result = selectBlockWidget.getResultState();
         if (result != null) {
            blockThresholds.set(i, new BlockWithFloat(result, blockWithFloat.percentage(), null));
            changed = true;
         }

         ImGui.popID();
      }

      if (removeBlockThreshold >= 0) {
         blockThresholds.remove(removeBlockThreshold);
         changed = true;
      }

      ImVec2 beforePos = ImGui.getCursorScreenPos();
      ImGui.setCursorScreenPos(beforePos.x, startPosition.y);
      ImGui.dummy(ImGui.getContentRegionAvailX(), beforePos.y - startPosition.y - ImGui.getStyle().getItemSpacingY());
      if (ImGui.beginDragDropTarget()) {
         EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class);
         if (droppedPalette != null) {
            List<CustomBlockState> customBlockStates = new ArrayList<>();

            for (CustomBlockStateOrTombstone block : droppedPalette.getBlocks()) {
               if (block instanceof CustomBlockState customBlockState) {
                  customBlockStates.add(customBlockState);
               }
            }

            if (!customBlockStates.isEmpty()) {
               blockThresholds.clear();
               float amount = 100.0F / customBlockStates.size();

               for (CustomBlockState customBlockState : customBlockStates) {
                  blockThresholds.add(new BlockWithFloat(customBlockState, new float[]{amount}, null));
               }
            }
         }

         ImGui.endDragDropTarget();
      }

      ImGui.setCursorScreenPos(beforePos.x, beforePos.y);
      if (ImGui.button("+##AddBlock", scaledSize, scaledSize)) {
         addNew(blockThresholds, (CustomBlockState)DEFAULT_BLOCKS.get(blockThresholds.size() % DEFAULT_BLOCKS.size()));
         changed = true;
      }

      CustomBlockState dragDropped = ImGuiHelper.blockStateDragDropTarget();
      if (dragDropped != null) {
         addNew(blockThresholds, dragDropped);
         changed = true;
      }

      return changed;
   }

   public static void addNew(List<BlockWithFloat> list, CustomBlockState blockState) {
      float total = 0.0F;

      for (BlockWithFloat blockWithFloat : list) {
         total += blockWithFloat.percentage[0];
      }

      if (total < 99.0F) {
         list.add(new BlockWithFloat(blockState, new float[]{100.0F - total}, null));
      } else {
         float factor = (float)list.size() / (list.size() + 1);

         for (BlockWithFloat blockWithFloat : list) {
            blockWithFloat.percentage[0] = blockWithFloat.percentage[0] * factor;
         }

         list.add(new BlockWithFloat(blockState, new float[]{100.0F / (list.size() + 1)}, null));
      }
   }

   public static enum ExtraRenderType {
      PERCENTAGE,
      FLOAT,
      BLOCKNAME;
   }
}
