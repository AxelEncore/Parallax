package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.BiomeDataManager;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.SetBiomeOperation;
import imgui.moulberry92.ImGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SetBiomeWindow {
   private static final int[] selectedBiome = new int[]{0};
   private static final List<SetBiomeWindow.BiomeWithIndex> filteredBiomes = new ArrayList<>();
   private static String biomeFilter = "";
   private static boolean fillVertically = false;

   public static void render() {
      if (!EditorWindowType.SET_BIOME.isOpen()) {
         filteredBiomes.clear();
         biomeFilter = "";
      } else {
         if (EditorWindowType.SET_BIOME.begin("###SetBiome", true)) {
            BiomeDataManager biomeDataManager = BiomeDataManager.get();
            if (biomeDataManager == null) {
               ImGui.text(AxiomI18n.get("axiom.widget.error_biome_registry"));
               EditorWindowType.SET_BIOME.end();
               return;
            }

            if (biomeDataManager.justRefreshed()) {
               filteredBiomes.clear();
               biomeFilter = "";
            }

            List<BiomeDataManager.BiomeDataEntry> biomes = biomeDataManager.biomes();
            if (biomes.isEmpty()) {
               ImGui.text(AxiomI18n.get("axiom.widget.error_biome_registry"));
               EditorWindowType.SET_BIOME.end();
               return;
            }

            if (selectedBiome[0] >= biomes.size()) {
               selectedBiome[0] = 0;
            }

            String currentBiome = biomes.get(selectedBiome[0]).prettyName();
            if (!ImGui.beginCombo("##Biomes", biomeFilter.isEmpty() ? currentBiome : biomeFilter)) {
               biomeFilter = "";
            } else {
               ImGui.setNextFrameWantCaptureKeyboard(true);
               String biomeFilterOld = biomeFilter;
               biomeFilter = ImGuiHelper.modifyFromInput(biomeFilter);
               if (biomeFilter.isEmpty()) {
                  for (int i = 0; i < biomes.size(); i++) {
                     ImGui.pushID(i);
                     boolean selected = i == selectedBiome[0];
                     if (ImGui.selectable(biomes.get(i).prettyName(), selected) && !selected) {
                        selectedBiome[0] = i;
                     }

                     if (selected) {
                        ImGui.setItemDefaultFocus();
                     }

                     ImGui.popID();
                  }
               } else {
                  if (!biomeFilter.equals(biomeFilterOld)) {
                     String filterLower = biomeFilter.toLowerCase(Locale.ROOT);
                     filteredBiomes.clear();

                     for (int i = 0; i < biomes.size(); i++) {
                        String biome = biomes.get(i).prettyName();
                        if (biome.toLowerCase(Locale.ROOT).contains(filterLower)) {
                           filteredBiomes.add(new SetBiomeWindow.BiomeWithIndex(biome, i));
                        }
                     }
                  }

                  if (filteredBiomes.size() > 0 && ImGui.isKeyPressed(525)) {
                     selectedBiome[0] = filteredBiomes.get(0).index;
                     ImGui.closeCurrentPopup();
                  }

                  for (int ix = 0; ix < filteredBiomes.size(); ix++) {
                     ImGui.pushID(ix);
                     SetBiomeWindow.BiomeWithIndex biomeWithIndex = filteredBiomes.get(ix);
                     boolean selectedx = biomeWithIndex.index == selectedBiome[0];
                     if (ImGui.selectable(biomeWithIndex.prettyName, selectedx) && !selectedx) {
                        selectedBiome[0] = biomeWithIndex.index;
                     }

                     if (selectedx) {
                        ImGui.setItemDefaultFocus();
                     }

                     ImGui.popID();
                  }
               }

               ImGui.endCombo();
            }

            if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.set_biome.fill_vertically"), fillVertically)) {
               fillVertically = !fillVertically;
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.set_biome.do_set_biome"))) {
               SetBiomeOperation.setBiome(biomes.get(selectedBiome[0]).key(), fillVertically);
            }
         }

         EditorWindowType.SET_BIOME.end();
      }
   }

   private record BiomeWithIndex(String prettyName, int index) {
   }
}
