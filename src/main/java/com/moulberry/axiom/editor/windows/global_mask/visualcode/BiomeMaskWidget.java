package com.moulberry.axiom.editor.windows.global_mask.visualcode;

import com.moulberry.axiom.BiomeDataManager;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class BiomeMaskWidget extends MaskWidget {
   private final int[] selectedBiome = new int[]{0};
   private final List<BiomeMaskWidget.BiomeWithIndex> filteredBiomes = new ArrayList<>();
   private String biomeFilter = "";

   public BiomeMaskWidget() {
   }

   public BiomeMaskWidget(ResourceKey<Biome> biome) {
      BiomeDataManager biomeDataManager = BiomeDataManager.get();
      if (biomeDataManager != null) {
         List<BiomeDataManager.BiomeDataEntry> biomes = biomeDataManager.biomes();
         if (!biomes.isEmpty()) {
            for (int i = 0; i < biomes.size(); i++) {
               BiomeDataManager.BiomeDataEntry biomeDataEntry = biomes.get(i);
               if (biomeDataEntry.key().equals(biome)) {
                  this.selectedBiome[0] = i;
                  break;
               }
            }
         }
      }
   }

   @Override
   public MaskWidget copy() {
      BiomeMaskWidget widget = new BiomeMaskWidget();
      widget.selectedBiome[0] = this.selectedBiome[0];
      widget.filteredBiomes.addAll(this.filteredBiomes);
      widget.biomeFilter = this.biomeFilter;
      return widget;
   }

   @Override
   public void doRender(boolean allowDragDropSource, BooleanWrapper allowDragDropTarget) {
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_biome") + " =");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (allowDragDropSource && ImGui.beginDragDropSource()) {
         ToolMaskWindow.setDragDroppingWidget(this);
         ImGui.endDragDropSource();
      }

      ImGui.sameLine();
      ImGui.setCursorPosX(ImGui.getCursorPosX() - ImGui.getStyle().getItemSpacingX());
      boolean changed = false;
      BiomeDataManager biomeDataManager = BiomeDataManager.get();
      if (biomeDataManager == null) {
         ImGui.text(AxiomI18n.get("axiom.widget.error_biome_registry"));
      } else {
         if (biomeDataManager.justRefreshed()) {
            this.filteredBiomes.clear();
            this.biomeFilter = "";
         }

         List<BiomeDataManager.BiomeDataEntry> biomes = biomeDataManager.biomes();
         if (biomes.isEmpty()) {
            ImGui.text(AxiomI18n.get("axiom.widget.error_biome_registry"));
         } else {
            if (this.selectedBiome[0] >= biomes.size()) {
               this.selectedBiome[0] = 0;
            }

            String currentBiome = biomes.get(this.selectedBiome[0]).prettyName();
            if (ImGui.beginCombo("##Biomes", this.biomeFilter.isEmpty() ? currentBiome : this.biomeFilter)) {
               ImGui.setNextFrameWantCaptureKeyboard(true);
               String biomeFilterOld = this.biomeFilter;
               this.biomeFilter = ImGuiHelper.modifyFromInput(this.biomeFilter);
               if (this.biomeFilter.isEmpty()) {
                  for (int i = 0; i < biomes.size(); i++) {
                     ImGui.pushID(i);
                     boolean selected = i == this.selectedBiome[0];
                     if (ImGui.selectable(biomes.get(i).prettyName(), selected) && !selected) {
                        this.selectedBiome[0] = i;
                        changed = true;
                     }

                     if (selected) {
                        ImGui.setItemDefaultFocus();
                     }

                     ImGui.popID();
                  }
               } else {
                  if (!this.biomeFilter.equals(biomeFilterOld)) {
                     String filterLower = this.biomeFilter.toLowerCase(Locale.ROOT);
                     this.filteredBiomes.clear();

                     for (int i = 0; i < biomes.size(); i++) {
                        String biome = biomes.get(i).prettyName();
                        if (biome.toLowerCase(Locale.ROOT).contains(filterLower)) {
                           this.filteredBiomes.add(new BiomeMaskWidget.BiomeWithIndex(biome, i));
                        }
                     }
                  }

                  if (this.filteredBiomes.size() > 0 && ImGui.isKeyPressed(525)) {
                     this.selectedBiome[0] = this.filteredBiomes.get(0).index;
                     changed = true;
                     ImGui.closeCurrentPopup();
                  }

                  for (int ix = 0; ix < this.filteredBiomes.size(); ix++) {
                     ImGui.pushID(ix);
                     BiomeMaskWidget.BiomeWithIndex biomeWithIndex = this.filteredBiomes.get(ix);
                     boolean selectedx = biomeWithIndex.index == this.selectedBiome[0];
                     if (ImGui.selectable(biomeWithIndex.prettyName, selectedx) && !selectedx) {
                        this.selectedBiome[0] = biomeWithIndex.index;
                        changed = true;
                     }

                     if (selectedx) {
                        ImGui.setItemDefaultFocus();
                     }

                     ImGui.popID();
                  }
               }

               ImGui.endCombo();
            } else {
               this.biomeFilter = "";
            }

            if (changed) {
               ToolMaskWindow.markDirty(this);
            }
         }
      }
   }

   public ResourceKey<Biome> getBiome() {
      BiomeDataManager biomeDataManager = BiomeDataManager.get();
      if (biomeDataManager == null) {
         return null;
      } else {
         List<BiomeDataManager.BiomeDataEntry> biomes = biomeDataManager.biomes();
         return biomes.isEmpty() ? null : biomes.get(this.selectedBiome[0]).key();
      }
   }

   private record BiomeWithIndex(String prettyName, int index) {
   }
}
