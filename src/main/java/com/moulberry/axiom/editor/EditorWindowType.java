package com.moulberry.axiom.editor;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.tutorial.TutorialStage;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;
import imgui.moulberry92.type.ImBoolean;
import java.util.ArrayList;
import java.util.List;

public enum EditorWindowType {
   TOOLS("tools", true, true, 8),
   TOOL_OPTIONS("tool_options", true, true, 8),
   PALETTE("palette", true, true),
   INVENTORY("inventory", true, true, 4104),
   ACTIVE_BLOCK("active_block", true, true, 24),
   HISTORY("history", true, true),
   WORLD_PROPERTIES("world_properties", true, true),
   CLIPBOARD("clipboard", true, true),
   TARGET_INFO("target_info", true, true),
   TEXT_ANNOTATIONS("text_annotations", true, false),
   KEYBINDS("keybinds", true, false),
   BLUEPRINT_BROWSER("blueprint_browser", true, false),
   TOOL_MASKS("tool_masks", true, false),
   STYLE_EDITOR("style_editor", true, false),
   FILTER_SELECTION("filter_selection", false, false, 64),
   EXPAND_SELECTION("expand_selection", false, false, 64),
   SHRINK_SELECTION("shrink_selection", false, false, 64),
   DISTORT_SELECTION("distort_selection", false, false, 64),
   SMOOTH_SELECTION("smooth_selection", false, false, 64),
   FILL("fill", false, false, 88),
   REPLACE("replace", false, false, 88),
   TYPE_REPLACE("type_replace", false, false, 88),
   SET_BIOME("set_biome", false, false, 88),
   AUTOSHADE("autoshade", false, false, 72),
   ANALYZE("analyze", false, false, 64),
   ANIMATED_REBUILD("animated_rebuild", false, false, 64),
   ROTATE_PLACEMENT("rotate_placement", false, false, 64);

   private final String nameKey;
   private final boolean important;
   private final ImBoolean open;
   private final boolean openByDefault;
   private final int extraFlags;
   private boolean justOpened = false;
   private boolean docked = false;
   private int dockId = -1;
   private boolean focused = false;
   private boolean disabled = false;
   private boolean disabledDimBg = false;

   private EditorWindowType(String nameKey, boolean important, boolean openByDefault, int extraFlags) {
      this.nameKey = nameKey;
      this.important = important;
      this.open = new ImBoolean(openByDefault);
      this.openByDefault = openByDefault;
      this.extraFlags = extraFlags;
   }

   private EditorWindowType(String nameKey, boolean important, boolean openByDefault) {
      this(nameKey, important, openByDefault, 0);
   }

   public static void setOpenToDefault() {
      for (EditorWindowType value : values()) {
         value.open.set(value.openByDefault);
      }
   }

   public static List<String> getOpenByName() {
      List<String> open = new ArrayList<>();

      for (EditorWindowType value : values()) {
         if (value.open.get()) {
            open.add(value.nameKey);
         }
      }

      return open;
   }

   public static void setOpenByName(List<String> open) {
      for (EditorWindowType value : values()) {
         value.open.set(open.contains(value.nameKey));
      }
   }

   public boolean begin(String suffix, boolean useFlags) {
      if (BuildConfig.DEBUG && !suffix.startsWith("###")) {
         throw new FaultyImplementationError("Suffix must start with ###");
      } else {
         if (this.justOpened) {
            this.justOpened = false;
            if (!this.docked && !EditorUI.isFirstFrame()) {
               ImGuiViewport viewport = ImGui.getMainViewport();
               ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), 8, 0.5F, 0.5F);
            }
         }

         int additionalFlags = 0;
         TutorialStage stage = TutorialManager.getCurrentStage();
         if (stage != null && stage.getLinkedWindow() != this) {
            ImGui.beginDisabled();
            additionalFlags |= 197120;
            this.disabled = true;
         }

         boolean begin;
         if (this.docked && !Axiom.configuration.internal.showCloseWindowButton) {
            begin = ImGui.begin(this.getName() + suffix, (useFlags ? this.getFlags() : 0) | additionalFlags);
         } else {
            begin = ImGui.begin(this.getName() + suffix, this.open, (useFlags ? this.getFlags() : 0) | additionalFlags);
         }

         if (begin) {
            this.docked = ImGui.isWindowDocked();
            this.focused = ImGui.isWindowFocused();
            this.disabledDimBg = this.disabled;
            return true;
         } else {
            this.disabledDimBg = false;
            return false;
         }
      }
   }

   public void end() {
      if (this.disabled) {
         this.disabled = false;
         if (this.disabledDimBg) {
            this.disabledDimBg = false;
            float x = ImGui.getWindowPosX();
            float y = ImGui.getWindowPosY();
            float width = ImGui.getWindowWidth();
            float height = ImGui.getWindowHeight();
            ImGui.getForegroundDrawList().addRectFilled(x, y, x + width, y + height, ImGui.getColorU32(61));
         }

         ImGui.end();
         ImGui.endDisabled();
      } else {
         ImGui.end();
      }
   }

   public int getFlags() {
      return this.extraFlags;
   }

   public boolean isOpen() {
      TutorialStage stage = TutorialManager.getCurrentStage();
      return stage != null && stage.getLinkedWindow() == this ? true : this.open.get();
   }

   public boolean isOpenAndActive() {
      return this.isOpen() && (!this.docked || this.focused);
   }

   public String getName() {
      return AxiomI18n.get("axiom.editorui.window." + this.nameKey);
   }

   public boolean isDocked() {
      return this.docked;
   }

   public boolean isImportant() {
      return this.important;
   }

   public void setOpen(boolean open) {
      if (this.open.get() != open) {
         if (open) {
            this.justOpened = true;
         }

         this.open.set(open);
      }
   }
}
