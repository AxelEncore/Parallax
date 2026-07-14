package com.moulberry.axiom.editor.styles;

import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiStyle;

public class BuiltinStyles {
   public static final ImGuiStyle IMGUI_DARK = new ImGuiStyle();
   public static final ImGuiStyle IMGUI_LIGHT = new ImGuiStyle();
   public static final ImGuiStyle[] BASE_STYLES = new ImGuiStyle[]{IMGUI_DARK, IMGUI_LIGHT};

   public static String[] getBaseStyleNames() {
      return new String[]{AxiomI18n.get("axiom.editorui.styles.imgui_dark"), AxiomI18n.get("axiom.editorui.styles.imgui_light")};
   }

   public static String getStringId(int index) {
      return switch (index) {
         case 1 -> "ImGuiLight";
         default -> "ImGuiDark";
      };
   }

   public static int getIndexForStringId(String id) {
      byte var2 = -1;
      switch (id.hashCode()) {
         case 2119051775:
            if (id.equals("ImGuiLight")) {
               var2 = 1;
            }
         default:
            return switch (var2) {
               case 1 -> 1;
               default -> 0;
            };
      }
   }

   /**
    * A softer, more minimalistic look for the Parallax editor: rounded corners, thin clean borders and
    * a little more breathing room. Purely visual — applied on top of the default ImGui dark/light themes,
    * with no behavioural changes. Users can still fully customise or override this via the theme editor.
    */
   private static void applyMinimalStyle(ImGuiStyle style) {
      style.setWindowRounding(6.0F);
      style.setChildRounding(6.0F);
      style.setFrameRounding(4.0F);
      style.setPopupRounding(6.0F);
      style.setScrollbarRounding(8.0F);
      style.setGrabRounding(4.0F);
      style.setTabRounding(4.0F);
      style.setWindowBorderSize(1.0F);
      style.setChildBorderSize(1.0F);
      style.setPopupBorderSize(1.0F);
      style.setFrameBorderSize(0.0F);
      style.setTabBorderSize(0.0F);
      style.setWindowPadding(8.0F, 8.0F);
      style.setFramePadding(6.0F, 4.0F);
      style.setItemSpacing(8.0F, 6.0F);
   }

   static {
      ImGui.styleColorsDark(IMGUI_DARK);
      ImGui.styleColorsLight(IMGUI_LIGHT);
      for (ImGuiStyle style : BASE_STYLES) {
         applyMinimalStyle(style);
      }
   }
}
