package com.moulberry.axiom.editor.widgets;

import com.google.common.base.Splitter;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.type.ImString;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TagListWidget {
   private final List<String> tags = new ArrayList<>();
   private final ImString tagSearchField = new ImString(20);
   private String lastSearch = "";
   private String exactSearchMatch = null;
   private List<String> allTags;
   private List<String> searchedTags;
   private static final Splitter SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

   public TagListWidget() {
      this.searchedTags = this.allTags = SPLITTER.splitToList(Axiom.configuration.blueprint.defaultTags);
   }

   public List<String> tags() {
      return this.tags;
   }

   public void render(int width) {
      ImGui.getWindowDrawList()
         .addText(
            ImGui.getCursorScreenPosX() + width + ImGui.getStyle().getItemInnerSpacingX(),
            ImGui.getCursorScreenPosY() + ImGui.getStyle().getFramePaddingY(),
            -1,
            AxiomI18n.get("axiom.widget.blueprint_tags")
         );
      List<String> tags = new ArrayList<>(this.tags);
      tags.add("+");
      boolean openTagPicker = false;
      int selected = ImGuiHelper.elementList(AxiomI18n.get("axiom.widget.blueprint_tags"), tags, width, 1, 3, true, null);
      if (selected == tags.size() - 1) {
         openTagPicker = true;
      } else if (selected >= 0) {
         this.tags.remove(selected);
      }

      if (openTagPicker) {
         ImGui.openPopup("###TagPicker");
         this.tagSearchField.clear();
         this.search("");
      }

      ImVec2 center = ImGui.getMainViewport().getCenter();
      ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
      if (ImGuiHelper.beginPopupModalCloseable(AxiomI18n.get("axiom.widget.add_blueprint_tag") + "###TagPicker", 64)) {
         if (openTagPicker) {
            ImGui.setKeyboardFocusHere();
         }

         if (ImGui.inputText("##TagSearch", this.tagSearchField, 16)) {
            this.search(ImGuiHelper.getString(this.tagSearchField));
         }

         String tagSearch = ImGuiHelper.getString(this.tagSearchField);
         ImGui.sameLine();
         boolean empty = tagSearch.isEmpty();
         if (empty) {
            ImGui.beginDisabled();
         }

         if (!empty && this.exactSearchMatch != null) {
            boolean useTag = ImGui.button(AxiomI18n.get("axiom.widget.use_blueprint_tag"));
            if (ImGui.isKeyPressed(525)) {
               useTag = true;
            }

            if (useTag) {
               if (!this.tags.contains(this.exactSearchMatch)) {
                  this.tags.add(this.exactSearchMatch);
               }

               ImGui.closeCurrentPopup();
            }
         } else if (ImGui.button(AxiomI18n.get("axiom.widget.create_blueprint_tag"))) {
            String newTag = tagSearch.toLowerCase(Locale.ROOT).trim();
            String allTags = Axiom.configuration.blueprint.defaultTags;
            if (allTags.isEmpty()) {
               allTags = newTag;
            } else {
               allTags = allTags + "," + newTag;
            }

            Axiom.configuration.blueprint.defaultTags = allTags;
            this.tagSearchField.clear();
            this.updateAllTags();
            this.search("");
            if (!this.tags.contains(newTag)) {
               this.tags.add(newTag);
            }

            ImGui.closeCurrentPopup();
         }

         if (empty) {
            ImGui.endDisabled();
         }

         selected = ImGuiHelper.elementList("TagList", this.searchedTags, 0.0F, 8, 8, false, index -> {
            if (ImGui.isItemClicked(1)) {
               ImGui.openPopup("##TagListButtonPopup" + index);
            }

            if (ImGuiHelper.beginPopup("##TagListButtonPopup" + index)) {
               if (ImGui.menuItem(AxiomI18n.get("axiom.widget.remove_blueprint_tag"))) {
                  String toRemove = this.searchedTags.get(index);
                  StringBuilder allTagsx = new StringBuilder();
                  boolean first = true;

                  for (String tagx : this.allTags) {
                     if (!tagx.equals(toRemove)) {
                        if (first) {
                           first = false;
                        } else {
                           allTagsx.append(",");
                        }

                        allTagsx.append(tagx);
                     }
                  }

                  Axiom.configuration.blueprint.defaultTags = allTagsx.toString();
                  this.updateAllTags();
                  this.lastSearch = "";
                  this.search(this.lastSearch);
                  ImGui.closeCurrentPopup();
               }

               ImGui.endPopup();
            }
         });
         if (selected >= 0) {
            String tag = this.searchedTags.get(selected);
            if (!this.tags.contains(tag)) {
               this.tags.add(tag);
            }

            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   public void updateAllTags() {
      boolean searchedTagsToo = this.searchedTags == this.allTags;
      this.allTags = List.of(Axiom.configuration.blueprint.defaultTags.split(","));
      if (searchedTagsToo) {
         this.searchedTags = this.allTags;
      }
   }

   public void search(String search) {
      search = search.toLowerCase(Locale.ROOT);
      if (!search.equals(this.lastSearch)) {
         this.exactSearchMatch = null;
         if (search.isBlank()) {
            this.searchedTags = this.allTags;
         } else if (this.searchedTags != this.allTags && search.startsWith(this.lastSearch)) {
            String searchFinal = search;
            this.searchedTags.removeIf(str -> {
               if (!str.contains(searchFinal)) {
                  return true;
               } else {
                  if (str.length() == searchFinal.length()) {
                     this.exactSearchMatch = str;
                  }

                  return false;
               }
            });
         } else {
            this.searchedTags = new ArrayList<>();

            for (String tag : Axiom.configuration.blueprint.defaultTags.split(",")) {
               if (tag.contains(search)) {
                  this.searchedTags.add(tag);
                  if (tag.length() == search.length()) {
                     this.exactSearchMatch = tag;
                  }
               }
            }
         }

         this.lastSearch = search;
      }
   }
}
