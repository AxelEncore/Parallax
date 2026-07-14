package com.moulberry.axiom.editor.styles;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.ImGuiHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiStyle;
import imgui.moulberry92.type.ImString;

public class StyleManager {
   private static int baseStyleIndex = 0;
   private static final ImString customThemeName = new ImString(64);

   public static void initialize() {
      if (Axiom.configuration.internal.savedCustomTheme != null) {
         StyleHelper.Theme theme = StyleHelper.Theme.convertFromBase64(Axiom.configuration.internal.savedCustomTheme.trim());
         if (theme != null) {
            loadTheme(theme);
         }
      }
   }

   public static ImGuiStyle getBaseStyle() {
      return BuiltinStyles.BASE_STYLES[baseStyleIndex];
   }

   public static int getBaseStyleIndex() {
      return baseStyleIndex;
   }

   public static ImString getCustomThemeName() {
      return customThemeName;
   }

   public static StyleHelper.Theme createTheme() {
      if (ImGui.getCurrentContext().isNotValidPtr()) {
         return null;
      } else {
         StyleHelper.ModifiedStyleValues modified = StyleHelper.calcModifiedStyleValues(getBaseStyle(), ImGui.getStyle());
         String themeId = BuiltinStyles.getStringId(baseStyleIndex);
         return new StyleHelper.Theme(ImGuiHelper.getString(getCustomThemeName()), themeId, modified);
      }
   }

   public static void loadTheme(StyleHelper.Theme theme) {
      customThemeName.set(theme.name());
      int styleIndex = BuiltinStyles.getIndexForStringId(theme.baseTheme());
      resetToBaseStyle(styleIndex);
      StyleHelper.applyModifiedStyleValues(ImGui.getStyle(), theme.values());
   }

   public static void switchBaseStyle(int newBaseStyleIndex) {
      StyleHelper.ModifiedStyleValues modified = StyleHelper.calcModifiedStyleValues(getBaseStyle(), ImGui.getStyle());
      baseStyleIndex = newBaseStyleIndex;
      StyleHelper.copyStyleValues(getBaseStyle(), ImGui.getStyle());
      StyleHelper.applyModifiedStyleValues(ImGui.getStyle(), modified);
   }

   public static void resetToBaseStyle(int baseStyleIndex) {
      StyleManager.baseStyleIndex = baseStyleIndex;
      StyleHelper.copyStyleValues(getBaseStyle(), ImGui.getStyle());
   }

   public static void reset() {
      StyleHelper.copyStyleValues(getBaseStyle(), ImGui.getStyle());
      getCustomThemeName().set("");
   }
}
