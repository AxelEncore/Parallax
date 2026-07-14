package com.moulberry.axiom.editor.widgets;

import com.moulberry.axiom.block_maps.LegacyBlocks;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;

public class SelectBlockWidget {
   private final boolean allowTags;
   private final int randomId = ThreadLocalRandom.current().nextInt();
   private boolean justOpened = false;
   private CustomBlockState resultState = null;
   private BlockList.MinecraftOrCustomTagSet resultTag = null;
   private Predicate<CustomBlockState> filter = null;
   private List<BlockList.Entry> searchedBlocks = null;
   private boolean showingTags = false;
   public Runnable extraRender = null;

   public SelectBlockWidget(boolean allowTags) {
      this.allowTags = allowTags;
   }

   public void open() {
      this.open(0);
   }

   public void open(int id) {
      ImGui.openPopup("###SelectBlockModal" + this.randomId + id);
      this.resultState = null;
      this.resultTag = null;
      this.justOpened = true;
      this.showingTags = false;
   }

   public void render(String label, BlockList blockList) {
      this.render(label, blockList, 0);
   }

   public void render(String label, BlockList blockList, int id) {
      boolean justOpened = this.justOpened;
      this.justOpened = false;
      if (justOpened) {
         blockList.search("", this.filter);
         this.searchedBlocks = blockList.getBlocks();
      }

      if (ImGuiHelper.beginPopupModalCloseable(label + "###SelectBlockModal" + this.randomId + id, 320)) {
         ImString searchInput = new ImString(blockList.getLastSearchRaw(), 64);
         if (justOpened) {
            ImGui.setKeyboardFocusHere();
         }

         if (ImGui.inputText(AxiomI18n.get("axiom.widget.search_block"), searchInput)) {
            String search = ImGuiHelper.getString(searchInput);
            blockList.search(search, this.filter);
            this.searchedBlocks = blockList.getBlocks();
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
                        this.searchedBlocks = new ArrayList<>(this.searchedBlocks);
                        this.searchedBlocks.add(0, BlockList.createEntry(blockState));
                     }
                  }
               } catch (NumberFormatException var11) {
               }
            }
         }

         boolean effectiveShowingTags = false;
         if (this.allowTags) {
            ImGui.sameLine();
            boolean showTagsBecauseOfPrefix = blockList.getLastSearchRaw().startsWith("#");
            effectiveShowingTags = this.showingTags || showTagsBecauseOfPrefix;
            if (effectiveShowingTags) {
               ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());
            }

            if (ImGui.button(AxiomI18n.get("axiom.widget.block_tags"))) {
               this.showingTags = !effectiveShowingTags;
               if (!this.showingTags && showTagsBecauseOfPrefix) {
                  blockList.search(blockList.getLastSearchRaw().substring(1), this.filter);
               }
            }

            if (effectiveShowingTags) {
               ImGuiHelper.popStyleVar();
            }
         }

         if (effectiveShowingTags) {
            List<BlockList.TagEntry> tags = blockList.getTags();
            if (!justOpened && ImGui.isKeyPressed(525) && !tags.isEmpty()) {
               BlockList.MinecraftOrCustomTagSet clickedTag = tags.get(0).tag();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               this.resultTag = clickedTag;
               return;
            }

            BlockList.MinecraftOrCustomTagSet selectedTag = ImGuiHelper.tagScrollList(tags, 384, 256, justOpened);
            if (selectedTag != null) {
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               this.resultTag = selectedTag;
               return;
            }
         } else {
            if (this.searchedBlocks == null) {
               this.searchedBlocks = blockList.getBlocks();
            }

            if (!justOpened && ImGui.isKeyPressed(525) && !this.searchedBlocks.isEmpty()) {
               CustomBlockState clickedBlock = this.searchedBlocks.get(0).state();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               this.resultState = clickedBlock;
               return;
            }

            CustomBlockState selectedBlock = ImGuiHelper.blockScrollList(
               this.searchedBlocks, (int)(384.0F * EditorUI.getUiScale()), (int)(256.0F * EditorUI.getUiScale()), justOpened
            );
            if (selectedBlock != null) {
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               this.resultState = selectedBlock;
               return;
            }
         }

         if (this.extraRender != null) {
            this.extraRender.run();
         }

         if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"), ImGui.getContentRegionAvailX(), 0.0F)) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   public void setFilter(Predicate<CustomBlockState> filter) {
      this.filter = filter;
   }

   public CustomBlockState getResultState() {
      CustomBlockState result = this.resultState;
      this.resultState = null;
      return result;
   }

   public BlockList.MinecraftOrCustomTagSet getResultTag() {
      BlockList.MinecraftOrCustomTagSet result = this.resultTag;
      this.resultTag = null;
      return result;
   }
}
