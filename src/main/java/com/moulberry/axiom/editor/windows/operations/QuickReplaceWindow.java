package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.block_maps.LegacyBlocks;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.ReplaceCopyPropertiesOperation;
import com.moulberry.axiom.operations.ReplaceOperation;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.utils.BlockCondition;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.type.ImString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class QuickReplaceWindow {
   private static final List<CustomBlock> fromBlocks = new ArrayList<>();
   private static boolean fromBlocksHasProperties = false;
   private static BlockList.MinecraftOrCustomTagSet fromTag;
   private static final SelectBlockWidget toBlockWidget = new SelectBlockWidget(false);
   public static boolean openQuickReplaceWindow = false;
   private static Set<Block> selectedBlocks = null;
   private static boolean filterSelectedBlocks = true;
   private static boolean copyProperties = true;
   private static boolean fromJustOpened = false;
   private static boolean fromShowingTags = false;
   private static List<BlockList.Entry> fromSearchedBlocks = null;

   public static boolean fromHasProperties() {
      return fromBlocksHasProperties || fromTag != null;
   }

   public static void render(BlockList blockList) {
      if (!Placement.INSTANCE.isPlacing()) {
         boolean openFrom = EditorUI.canProcessKeybinds && Keybinds.QUICK_REPLACE.isPressed(false) && !Selection.getSelectionBuffer().isEmpty();
         if (!openFrom && !openQuickReplaceWindow) {
            if (Selection.getSelectionBuffer().isEmpty()) {
               return;
            }
         } else {
            if (ToolManager.isToolActive()) {
               ToolManager.getCurrentTool().reset();
            }

            if (Selection.getSelectionBuffer().isEmpty()) {
               return;
            }

            ImGui.openPopup("###SelectBlockModalFrom");
            selectedBlocks = null;
            fromBlocks.clear();
            fromBlocksHasProperties = false;
            fromTag = null;
            fromJustOpened = true;
            fromShowingTags = false;
            openQuickReplaceWindow = false;
         }

         if (renderFromSelector(AxiomI18n.get("axiom.keybinds.quick_replace.replace_from"), blockList)) {
            toBlockWidget.open();
         }

         if (!fromBlocks.isEmpty() || fromTag != null) {
            toBlockWidget.render(AxiomI18n.get("axiom.keybinds.quick_replace.replace_to"), blockList);
            CustomBlockState blockState = toBlockWidget.getResultState();
            if (blockState != null) {
               BlockCondition blockCondition = null;
               if (fromTag != null) {
                  blockCondition = BlockCondition.fromMinecraftOrCustomTag(fromTag);
               } else if (!fromBlocks.isEmpty()) {
                  blockCondition = BlockCondition.fromCustomBlocks(fromBlocks);
               }

               if (blockCondition != null) {
                  if (copyProperties && fromHasProperties()) {
                     ReplaceCopyPropertiesOperation.replace(blockCondition, blockState);
                  } else {
                     ReplaceOperation.replace(blockCondition, blockState.getVanillaState());
                  }
               }

               fromBlocks.clear();
               fromBlocksHasProperties = false;
               fromTag = null;
            }
         }
      }
   }

   public static boolean renderFromSelector(String label, BlockList blockList) {
      boolean justOpened = fromJustOpened;
      fromJustOpened = false;
      if (justOpened) {
         blockList.search("", getFromFilter(), true);
         fromSearchedBlocks = blockList.getBlocks();
      }

      ImVec2 center = ImGui.getMainViewport().getCenter();
      ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
      if (ImGuiHelper.beginPopupModalCloseable(label + "###SelectBlockModalFrom", 320)) {
         ImString searchInput = new ImString(blockList.getLastSearchRaw(), 64);
         if (justOpened) {
            ImGui.setKeyboardFocusHere();
         }

         if (ImGui.inputText(AxiomI18n.get("axiom.widget.search_block"), searchInput)) {
            String search = ImGuiHelper.getString(searchInput);
            blockList.search(search, getFromFilter());
            fromSearchedBlocks = blockList.getBlocks();
            String[] split = search.split(":");
            if (split.length >= 1) {
               try {
                  int blockId = Integer.parseInt(split[0]);
                  int blockData = 0;
                  if (split.length >= 2) {
                     blockData = Integer.parseInt(split[1]);
                  }

                  if (blockId <= 255 && blockData <= 15) {
                     BlockState blockState = LegacyBlocks.getLegacyBlocks()[blockId * 16 + blockData];
                     if (blockState != null) {
                        fromSearchedBlocks = new ArrayList<>(fromSearchedBlocks);
                        fromSearchedBlocks.add(0, BlockList.createEntry(blockState));
                     }
                  }
               } catch (NumberFormatException var14) {
               }
            }
         }

         ImGui.sameLine();
         boolean showTagsBecauseOfPrefix = blockList.getLastSearchRaw().startsWith("#");
         boolean effectiveShowingTags = fromShowingTags || showTagsBecauseOfPrefix;
         if (effectiveShowingTags) {
            ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());
         }

         if (ImGui.button(AxiomI18n.get("axiom.widget.block_tags"))) {
            fromShowingTags = !effectiveShowingTags;
            if (!fromShowingTags && showTagsBecauseOfPrefix) {
               blockList.search(blockList.getLastSearchRaw().substring(1), getFromFilter());
            }
         }

         if (effectiveShowingTags) {
            ImGuiHelper.popStyleVar();
         }

         if (effectiveShowingTags) {
            List<BlockList.TagEntry> tags = blockList.getTags();
            if (!justOpened && ImGui.isKeyPressed(525) && !tags.isEmpty()) {
               BlockList.MinecraftOrCustomTagSet clickedTag = tags.get(0).tag();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               fromTag = clickedTag;
               return true;
            }

            BlockList.MinecraftOrCustomTagSet selectedTag = ImGuiHelper.tagScrollList(tags, 384, 256, justOpened);
            if (selectedTag != null) {
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               fromTag = selectedTag;
               return true;
            }
         } else {
            if (fromSearchedBlocks == null) {
               fromSearchedBlocks = blockList.getBlocks();
            }

            if (!justOpened && ImGui.isKeyPressed(525) && !fromSearchedBlocks.isEmpty()) {
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               CustomBlock customBlock = fromSearchedBlocks.get(0).state().getCustomBlock();
               if (!fromBlocks.contains(customBlock)) {
                  fromBlocks.add(customBlock);
                  fromBlocksHasProperties = fromBlocksHasProperties | !customBlock.axiom$getProperties().isEmpty();
               }

               return true;
            }

            CustomBlockState selectedBlock = ImGuiHelper.blockScrollList(
               fromSearchedBlocks, (int)(384.0F * EditorUI.getUiScale()), (int)(256.0F * EditorUI.getUiScale()), justOpened
            );
            if (selectedBlock != null) {
               CustomBlock customBlock = selectedBlock.getCustomBlock();
               if (!fromBlocks.contains(customBlock)) {
                  fromBlocks.add(customBlock);
                  fromBlocksHasProperties = fromBlocksHasProperties | !customBlock.axiom$getProperties().isEmpty();
               }

               if (!EditorUI.isCtrlOrCmdDown()) {
                  ImGui.closeCurrentPopup();
                  ImGui.endPopup();
                  return true;
               }
            }
         }

         if (!fromShowingTags) {
            if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.filter_selected_blocks"), filterSelectedBlocks)) {
               filterSelectedBlocks = !filterSelectedBlocks;
               blockList.search(blockList.getLastSearchRaw(), getFromFilter(), true);
               fromSearchedBlocks = blockList.getBlocks();
            }

            if (Axiom.configuration.internal.showQuickReplaceCtrlClickTip) {
               ImGui.separator();
               ImGui.pushTextWrapPos(380.0F);
               ImGui.text(AxiomI18n.get("axiom.hardcoded.tip_ctrl_click_multi"));
               ImGui.popTextWrapPos();
               if (ImGui.smallButton(AxiomI18n.get("axiom.hardcoded.dont_show_tip_again"))) {
                  Axiom.configuration.internal.showQuickReplaceCtrlClickTip = false;
               }

               if (fromBlocks.isEmpty()) {
                  ImGui.separator();
               }
            }

            if (!fromBlocks.isEmpty()) {
               ImGuiHelper.separatorWithText("Replace List");
               float available = ImGui.getContentRegionAvailX() + ImGui.getStyle().getItemSpacingX();
               float itemWidth = (int)(32.0F * EditorUI.getUiScale()) + ImGui.getStyle().getItemSpacingX();
               int count = (int)Math.max(1.0, Math.floor(available / itemWidth));
               int removeBlock = -1;

               for (int i = 0; i < fromBlocks.size(); i++) {
                  CustomBlock entry = fromBlocks.get(i);
                  if (i != 0 && i % count != 0) {
                     ImGui.sameLine();
                  }

                  if (ImGuiHelper.blockStateButton(entry.axiom$defaultCustomState(), i, (int)(32.0F * EditorUI.getUiScale()))) {
                     removeBlock = i;
                  }

                  if (ImGui.isItemHovered()) {
                     ImGui.beginTooltip();
                     ImGui.text(AxiomI18n.get(entry.axiom$translationKey()));
                     ImGui.textDisabled(entry.axiom$getIdentifier().toString());
                     ImGui.endTooltip();
                  }
               }

               if (removeBlock >= 0) {
                  fromBlocks.remove(removeBlock);
                  fromBlocksHasProperties = false;

                  for (CustomBlock fromBlock : fromBlocks) {
                     fromBlocksHasProperties = fromBlocksHasProperties | !fromBlock.axiom$getProperties().isEmpty();
                  }
               }

               if (ImGui.button(AxiomI18n.get("axiom.keybinds.quick_replace.replace_all"), ImGui.getContentRegionAvailX(), 0.0F)) {
                  ImGui.closeCurrentPopup();
                  ImGui.endPopup();
                  return true;
               }
            }
         }

         if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"), ImGui.getContentRegionAvailX(), 0.0F)) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }

      return false;
   }

   public static Predicate<CustomBlockState> getFromFilter() {
      if (!filterSelectedBlocks) {
         return null;
      } else {
         if (selectedBlocks == null) {
            selectedBlocks = new HashSet<>();
            SelectionBuffer buffer = Selection.getSelectionBuffer();
            ClientLevel level = Minecraft.getInstance().level;
            if (buffer instanceof SelectionBuffer.AABB aabb) {
               BlockPos min = aabb.min();
               BlockPos max = aabb.max();
               int minX = min.getX();
               int minY = min.getY();
               int minZ = min.getZ();
               int maxX = max.getX();
               int maxY = max.getY();
               int maxZ = max.getZ();
               int minChunkX = minX >> 4;
               int minChunkY = minY >> 4;
               int minChunkZ = minZ >> 4;
               int maxChunkX = maxX >> 4;
               int maxChunkY = maxY >> 4;
               int maxChunkZ = maxZ >> 4;

               for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                  for (int cy = minChunkY; cy <= maxChunkY; cy++) {
                     for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                        int sectionIndex = level.getSectionIndexFromSectionY(cy);
                        if (sectionIndex >= 0 && sectionIndex < level.getSectionsCount()) {
                           LevelChunk levelChunk = (LevelChunk)level.getChunk(cx, cz, ChunkStatus.FULL, false);
                           if (levelChunk != null) {
                              LevelChunkSection section = levelChunk.getSection(sectionIndex);
                              if (section.hasOnlyAir()) {
                                 selectedBlocks.add(Blocks.AIR);
                              } else {
                                 PalettedContainer<BlockState> states = section.getStates();
                                 boolean maybeHasUnseenValue = states.maybeHas(blockState -> !selectedBlocks.contains(blockState.getBlock()));
                                 if (maybeHasUnseenValue) {
                                    int minRelativeX = Math.max(0, minX - (cx << 4));
                                    int minRelativeY = Math.max(0, minY - (cy << 4));
                                    int minRelativeZ = Math.max(0, minZ - (cz << 4));
                                    int maxRelativeX = Math.min(15, maxX - (cx << 4));
                                    int maxRelativeY = Math.min(15, maxY - (cy << 4));
                                    int maxRelativeZ = Math.min(15, maxZ - (cz << 4));
                                    if (minRelativeX == 0
                                       && minRelativeY == 0
                                       && minRelativeZ == 0
                                       && maxRelativeX == 15
                                       && maxRelativeY == 15
                                       && maxRelativeZ == 15) {
                                       states.getAll(blockState -> selectedBlocks.add(blockState.getBlock()));
                                    } else {
                                       for (int y = minRelativeY; y <= maxRelativeY; y++) {
                                          for (int z = minRelativeZ; z <= maxRelativeZ; z++) {
                                             for (int x = minRelativeX; x <= maxRelativeX; x++) {
                                                selectedBlocks.add(((BlockState)states.get(x, y, z)).getBlock());
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            } else if (buffer instanceof SelectionBuffer.Set set) {
               set.selectionRegion.forEachChunk((cxx, cyx, czx, chunk) -> {
                  int sectionIndexx = level.getSectionIndexFromSectionY(cyx);
                  if (sectionIndexx >= 0 && sectionIndexx < level.getSectionsCount()) {
                     LevelChunk levelChunkx = (LevelChunk)level.getChunk(cxx, czx, ChunkStatus.FULL, false);
                     if (levelChunkx != null) {
                        LevelChunkSection sectionx = levelChunkx.getSection(sectionIndexx);
                        if (sectionx.hasOnlyAir()) {
                           selectedBlocks.add(Blocks.AIR);
                        } else {
                           PalettedContainer<BlockState> statesx = sectionx.getStates();
                           boolean maybeHasUnseenValuex = statesx.maybeHas(blockState -> !selectedBlocks.contains(blockState.getBlock()));
                           if (maybeHasUnseenValuex) {
                              int index = 0;

                              for (int zx = 0; zx < 16; zx++) {
                                 for (int yx = 0; yx < 16; yx++) {
                                    short v = chunk[index++];
                                    if (v == -1) {
                                       selectedBlocks.add(((BlockState)statesx.get(0, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(1, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(2, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(3, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(4, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(5, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(6, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(7, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(8, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(9, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(10, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(11, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(12, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(13, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(14, yx, zx)).getBlock());
                                       selectedBlocks.add(((BlockState)statesx.get(15, yx, zx)).getBlock());
                                    } else {
                                       for (int xx = 0; xx < 16; xx++) {
                                          if ((v & 1 << xx) != 0) {
                                             selectedBlocks.add(((BlockState)statesx.get(xx, yx, zx)).getBlock());
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               });
            } else {
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               buffer.forEach((xx, yx, zx) -> {
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(xx, yx, zx));
                  selectedBlocks.add(blockState.getBlock());
               });
            }
         }

         if (selectedBlocks.isEmpty()) {
            return null;
         } else {
            Set<Block> selectedBlocksCopy = selectedBlocks;
            return block -> selectedBlocksCopy.contains(block.getVanillaState().getBlock());
         }
      }
   }

   static {
      toBlockWidget.extraRender = () -> {
         if (fromHasProperties() && ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.replace.copy_properties"), copyProperties)) {
            copyProperties = !copyProperties;
         }
      };
   }
}
