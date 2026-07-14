package com.moulberry.axiom.editor.widgets;

import com.moulberry.axiom.editor.ImGuiHelper;
import imgui.moulberry92.ImGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchableCombo {
   private String filter = "";
   private final List<SearchableCombo.StringWithIndex> available = new ArrayList<>();
   private final List<SearchableCombo.StringWithIndex> filtered = new ArrayList<>();

   public SearchableCombo(String[] values) {
      this.setElements(values);
   }

   public void setElements(String[] values) {
      this.available.clear();

      for (String value : values) {
         this.available.add(new SearchableCombo.StringWithIndex(value, this.available.size()));
      }
   }

   public boolean render(String label, int[] selectedIndex) {
      int oldValue = selectedIndex[0];
      this.renderInner(label, selectedIndex);
      return selectedIndex[0] != oldValue;
   }

   private void renderInner(String label, int[] selectedIndex) {
      if (selectedIndex[0] >= this.available.size()) {
         selectedIndex[0] = 0;
      }

      String currentBiome = selectedIndex[0] < 0 ? "" : this.available.get(selectedIndex[0]).string;
      if (ImGui.beginCombo(label, this.filter.isEmpty() ? currentBiome : this.filter)) {
         ImGui.setNextFrameWantCaptureKeyboard(true);
         String filterOld = this.filter;
         this.filter = ImGuiHelper.modifyFromInput(this.filter);
         boolean enterPressed = ImGui.isKeyPressed(512);
         if (this.filter.isEmpty()) {
            for (int i = 0; i < this.available.size(); i++) {
               ImGui.pushID(i);
               boolean selected = i == selectedIndex[0];
               if (ImGui.selectable(this.available.get(i).string, selected) && !selected) {
                  selectedIndex[0] = i;
               }

               if (enterPressed && ImGui.isItemFocused()) {
                  selectedIndex[0] = i;
                  ImGui.popID();
                  ImGui.closeCurrentPopup();
                  ImGui.endCombo();
                  return;
               }

               if (selected) {
                  ImGui.setItemDefaultFocus();
               }

               ImGui.popID();
            }

            if (enterPressed) {
               selectedIndex[0] = 0;
               ImGui.closeCurrentPopup();
            }
         } else {
            if (!this.filter.equals(filterOld)) {
               String filterLower = this.filter.toLowerCase(Locale.ROOT);
               this.filtered.clear();

               for (int i = 0; i < this.available.size(); i++) {
                  String name = this.available.get(i).string;
                  if (name.toLowerCase(Locale.ROOT).contains(filterLower)) {
                     this.filtered.add(new SearchableCombo.StringWithIndex(name, i));
                  }
               }
            }

            for (int ix = 0; ix < this.filtered.size(); ix++) {
               ImGui.pushID(ix);
               SearchableCombo.StringWithIndex stringWithIndex = this.filtered.get(ix);
               boolean selectedx = stringWithIndex.index == selectedIndex[0];
               if (ImGui.selectable(stringWithIndex.string, selectedx) && !selectedx) {
                  selectedIndex[0] = stringWithIndex.index;
               }

               if (enterPressed && ImGui.isItemFocused()) {
                  selectedIndex[0] = stringWithIndex.index;
                  ImGui.popID();
                  ImGui.closeCurrentPopup();
                  ImGui.endCombo();
                  return;
               }

               if (selectedx) {
                  ImGui.setItemDefaultFocus();
               }

               ImGui.popID();
            }

            if (enterPressed && !this.filtered.isEmpty()) {
               selectedIndex[0] = this.filtered.get(0).index;
               ImGui.closeCurrentPopup();
            }
         }

         ImGui.endCombo();
      } else {
         this.filter = "";
      }
   }

   private record StringWithIndex(String string, int index) {
   }
}
