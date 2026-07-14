package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.styles.BuiltinStyles;
import com.moulberry.axiom.editor.styles.StyleHelper;
import com.moulberry.axiom.editor.styles.StyleManager;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiStyle;
import imgui.moulberry92.ImVec4;
import imgui.moulberry92.type.ImString;
import net.minecraft.client.Minecraft;

public class StyleEditorWindow {
   private static final ImVec4 colorBuf = new ImVec4();
   private static final float[] colorBufArr = new float[4];
   private static final ImString demoTextInput = new ImString(64);
   private static boolean demoCheckbox = true;
   private static final int[] demoIntSlider = new int[]{0};
   private static long copiedToClipboardMillis = 0L;
   private static String lastStyleOnClipboard;

   public static void render() {
      if (EditorWindowType.STYLE_EDITOR.isOpen()) {
         ImGui.setNextWindowSizeConstraints(500.0F, 350.0F, 5000.0F, 3000.0F);
         if (EditorWindowType.STYLE_EDITOR.begin("###StyleEditor", true)) {
            int[] baseStyleIndex = new int[]{StyleManager.getBaseStyleIndex()};
            ImGui.setNextItemWidth(200.0F);
            if (ImGuiHelper.combo(AxiomI18n.get("axiom.editorui.window.style_editor.base_style"), baseStyleIndex, BuiltinStyles.getBaseStyleNames())) {
               StyleManager.switchBaseStyle(baseStyleIndex[0]);
            }

            ImGuiStyle baseStyle = StyleManager.getBaseStyle();
            ImGuiStyle currentStyle = ImGui.getStyle();
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.style_editor.custom_theme"));
            ImGui.setNextItemWidth(200.0F);
            ImGui.inputText(AxiomI18n.get("axiom.editorui.window.style_editor.theme_name"), StyleManager.getCustomThemeName());
            long currentTime = System.currentTimeMillis();
            long copyDelta = currentTime - copiedToClipboardMillis;
            String clipboardText = copyDelta >= 0L && copyDelta < 3000L
               ? AxiomI18n.get("axiom.editorui.window.style_editor.copied")
               : AxiomI18n.get("axiom.editorui.window.style_editor.export_to_clipboard");
            if (ImGui.button(clipboardText + "##ClipboardExport", 200.0F, 0.0F)) {
               StyleHelper.Theme theme = StyleManager.createTheme();
               if (theme != null) {
                  String base64 = theme.convertToBase64();
                  Minecraft.getInstance().keyboardHandler.setClipboard(base64);
                  copiedToClipboardMillis = currentTime;
               }
            }

            String clipboard = EditorUI.getClipboard();
            if (clipboard.startsWith("AS")) {
               lastStyleOnClipboard = clipboard;
            } else {
               lastStyleOnClipboard = null;
            }

            if (lastStyleOnClipboard == null) {
               ImGui.beginDisabled();
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.style_editor.import_from_clipboard"), 200.0F, 0.0F)) {
               StyleHelper.Theme theme = StyleHelper.Theme.convertFromBase64(lastStyleOnClipboard.trim());
               if (theme != null) {
                  StyleManager.loadTheme(theme);
               }
            }

            if (lastStyleOnClipboard == null) {
               ImGui.endDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.style_editor.reset"))) {
               StyleManager.reset();
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.style_editor.theme_parameters"));
            boolean showSizesOptions = false;
            boolean showColorOptions = false;
            if (ImGui.beginTabBar("###StyleOptions")) {
               if (ImGui.beginTabItem(AxiomI18n.get("axiom.editorui.window.style_editor.sizes") + "###SizesOptions")) {
                  showSizesOptions = true;
                  ImGui.endTabItem();
               }

               if (ImGui.beginTabItem(AxiomI18n.get("axiom.editorui.window.style_editor.color") + "###ColorOptions")) {
                  showColorOptions = true;
                  ImGui.endTabItem();
               }

               ImGui.endTabBar();
            }

            if (ImGui.beginTable("##ColorAndPreviewTable", 2, 513)) {
               ImGui.tableNextColumn();
               if (showSizesOptions) {
                  if (ImGui.beginChild("##SizesChild", 0.0F, -2.0F)) {
                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.borders_category"))) {
                        ImGui.indent();
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.window_borders"),
                           currentStyle,
                           baseStyle,
                           0,
                           1,
                           ImGuiStyle::getWindowBorderSize,
                           ImGuiStyle::setWindowBorderSize
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.child_borders"),
                           currentStyle,
                           baseStyle,
                           0,
                           1,
                           ImGuiStyle::getChildBorderSize,
                           ImGuiStyle::setChildBorderSize
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.popup_borders"),
                           currentStyle,
                           baseStyle,
                           0,
                           1,
                           ImGuiStyle::getPopupBorderSize,
                           ImGuiStyle::setPopupBorderSize
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.frame_borders"),
                           currentStyle,
                           baseStyle,
                           0,
                           1,
                           ImGuiStyle::getFrameBorderSize,
                           ImGuiStyle::setFrameBorderSize
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.tab_borders"),
                           currentStyle,
                           baseStyle,
                           0,
                           1,
                           ImGuiStyle::getTabBorderSize,
                           ImGuiStyle::setTabBorderSize
                        );
                        ImGui.unindent();
                     }

                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.rounding_category"))) {
                        ImGui.indent();
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.window_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getWindowRounding,
                           ImGuiStyle::setWindowRounding
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.child_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getChildRounding,
                           ImGuiStyle::setChildRounding
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.popup_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getPopupRounding,
                           ImGuiStyle::setPopupRounding
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.frame_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getFrameRounding,
                           ImGuiStyle::setFrameRounding
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.tab_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getTabRounding,
                           ImGuiStyle::setTabRounding
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.scrollbar_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getScrollbarRounding,
                           ImGuiStyle::setScrollbarRounding
                        );
                        renderVarChanger1(
                           AxiomI18n.get("axiom.editorui.window.style_editor.slider_grab_rounding"),
                           currentStyle,
                           baseStyle,
                           0,
                           12,
                           ImGuiStyle::getGrabRounding,
                           ImGuiStyle::setGrabRounding
                        );
                        ImGui.unindent();
                     }
                  }

                  ImGui.endChild();
               } else if (showColorOptions) {
                  if (ImGui.beginChild("##ColorChild", 0.0F, -2.0F)) {
                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.text_category"))) {
                        ImGui.indent();
                        renderColorChanger(currentStyle, baseStyle, 0, AxiomI18n.get("axiom.editorui.window.style_editor.text"));
                        renderColorChanger(currentStyle, baseStyle, 1, AxiomI18n.get("axiom.editorui.window.style_editor.disabled_text"));
                        ImGui.unindent();
                     }

                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.window_category"))) {
                        ImGui.indent();
                        renderColorChanger(currentStyle, baseStyle, 2, AxiomI18n.get("axiom.editorui.window.style_editor.window_background"));
                        renderColorChanger(currentStyle, baseStyle, 4, AxiomI18n.get("axiom.editorui.window.style_editor.popup_background"));
                        renderColorChanger(currentStyle, baseStyle, 5, AxiomI18n.get("axiom.editorui.window.style_editor.border_color"));
                        renderColorChanger(currentStyle, baseStyle, 10, AxiomI18n.get("axiom.editorui.window.style_editor.title_background"));
                        renderColorChanger(currentStyle, baseStyle, 11, AxiomI18n.get("axiom.editorui.window.style_editor.title_background_active"));
                        renderColorChanger(currentStyle, baseStyle, 12, AxiomI18n.get("axiom.editorui.window.style_editor.title_background_collapsed"));
                        renderColorChanger(currentStyle, baseStyle, 30, AxiomI18n.get("axiom.editorui.window.style_editor.resize_grip"));
                        renderColorChanger(currentStyle, baseStyle, 31, AxiomI18n.get("axiom.editorui.window.style_editor.resize_grip_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 32, AxiomI18n.get("axiom.editorui.window.style_editor.resize_grip_active"));
                        ImGui.unindent();
                     }

                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.frame_category"))) {
                        ImGui.indent();
                        renderColorChanger(currentStyle, baseStyle, 7, AxiomI18n.get("axiom.editorui.window.style_editor.frame_background"));
                        renderColorChanger(currentStyle, baseStyle, 8, AxiomI18n.get("axiom.editorui.window.style_editor.frame_background_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 9, AxiomI18n.get("axiom.editorui.window.style_editor.frame_background_active"));
                        ImGui.unindent();
                     }

                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.widget_category"))) {
                        ImGui.indent();
                        renderColorChanger(currentStyle, baseStyle, 18, AxiomI18n.get("axiom.editorui.window.style_editor.checkmark"));
                        renderColorChanger(currentStyle, baseStyle, 21, AxiomI18n.get("axiom.editorui.window.style_editor.button"));
                        renderColorChanger(currentStyle, baseStyle, 22, AxiomI18n.get("axiom.editorui.window.style_editor.button_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 23, AxiomI18n.get("axiom.editorui.window.style_editor.button_active"));
                        renderColorChanger(currentStyle, baseStyle, 19, AxiomI18n.get("axiom.editorui.window.style_editor.slider_grab"));
                        renderColorChanger(currentStyle, baseStyle, 20, AxiomI18n.get("axiom.editorui.window.style_editor.slider_grab_active"));
                        renderColorChanger(currentStyle, baseStyle, 24, AxiomI18n.get("axiom.editorui.window.style_editor.header"));
                        renderColorChanger(currentStyle, baseStyle, 25, AxiomI18n.get("axiom.editorui.window.style_editor.header_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 26, AxiomI18n.get("axiom.editorui.window.style_editor.header_active"));
                        renderColorChanger(currentStyle, baseStyle, 27, AxiomI18n.get("axiom.editorui.window.style_editor.separator"));
                        renderColorChanger(currentStyle, baseStyle, 28, AxiomI18n.get("axiom.editorui.window.style_editor.separator_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 29, AxiomI18n.get("axiom.editorui.window.style_editor.separator_active"));
                        renderColorChanger(currentStyle, baseStyle, 35, AxiomI18n.get("axiom.editorui.window.style_editor.tab"));
                        renderColorChanger(currentStyle, baseStyle, 34, AxiomI18n.get("axiom.editorui.window.style_editor.tab_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 36, AxiomI18n.get("axiom.editorui.window.style_editor.tab_active"));
                        renderColorChanger(currentStyle, baseStyle, 38, AxiomI18n.get("axiom.editorui.window.style_editor.unfocused_tab"));
                        renderColorChanger(currentStyle, baseStyle, 39, AxiomI18n.get("axiom.editorui.window.style_editor.unfocused_tab_active"));
                        renderColorChanger(currentStyle, baseStyle, 49, AxiomI18n.get("axiom.editorui.window.style_editor.table_border_light"));
                        renderColorChanger(currentStyle, baseStyle, 48, AxiomI18n.get("axiom.editorui.window.style_editor.table_border_strong"));
                        ImGui.unindent();
                     }

                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.scrollbar_category"))) {
                        ImGui.indent();
                        renderColorChanger(currentStyle, baseStyle, 14, AxiomI18n.get("axiom.editorui.window.style_editor.scrollbar_background"));
                        renderColorChanger(currentStyle, baseStyle, 15, AxiomI18n.get("axiom.editorui.window.style_editor.scrollbar_grab"));
                        renderColorChanger(currentStyle, baseStyle, 16, AxiomI18n.get("axiom.editorui.window.style_editor.scrollbar_grab_hovered"));
                        renderColorChanger(currentStyle, baseStyle, 17, AxiomI18n.get("axiom.editorui.window.style_editor.scrollbar_grab_active"));
                        ImGui.unindent();
                     }

                     if (ImGui.collapsingHeader(AxiomI18n.get("axiom.editorui.window.style_editor.other_category"))) {
                        ImGui.indent();
                        renderColorChanger(currentStyle, baseStyle, 13, AxiomI18n.get("axiom.editorui.window.style_editor.menu_bar_background"));
                        renderColorChanger(currentStyle, baseStyle, 55, AxiomI18n.get("axiom.editorui.window.style_editor.drag_drop_target"));
                        renderColorChanger(currentStyle, baseStyle, 41, AxiomI18n.get("axiom.editorui.window.style_editor.docking_preview"));
                        renderColorChanger(currentStyle, baseStyle, 42, AxiomI18n.get("axiom.editorui.window.style_editor.docking_empty_background"));
                        ImGui.unindent();
                     }
                  }

                  ImGui.endChild();
               }

               ImGui.tableNextColumn();
               ImGui.text(AxiomI18n.get("axiom.hardcoded.text_lbl"));
               ImGui.textDisabled(AxiomI18n.get("axiom.hardcoded.demo_disabled_text"));
               ImGui.inputText("Text Input", demoTextInput);
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.demo_button"))) {
                  ImGui.openPopup("##DemoPopup");
               }

               if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.demo_checkbox"), demoCheckbox)) {
                  demoCheckbox = !demoCheckbox;
               }

               if (ImGui.beginPopup("##DemoPopup")) {
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_popup"));
                  ImGui.endPopup();
               }

               ImGui.sliderInt("Slider", demoIntSlider, 0, 100);
               if (ImGui.collapsingHeader(AxiomI18n.get("axiom.hardcoded.demo_collapsing_header"))) {
                  ImGui.indent();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_hello_there"));
                  ImGui.unindent();
               }

               ImGuiHelper.separatorWithText("Separator");
               if (ImGui.beginTabBar("##DemoTabBar")) {
                  if (ImGui.beginTabItem(AxiomI18n.get("axiom.hardcoded.demo_tab1"))) {
                     ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_contents1"));
                     ImGui.endTabItem();
                  }

                  if (ImGui.beginTabItem(AxiomI18n.get("axiom.hardcoded.demo_tab2"))) {
                     ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_contents2"));
                     ImGui.endTabItem();
                  }

                  if (ImGui.beginTabItem(AxiomI18n.get("axiom.hardcoded.demo_tab3"))) {
                     ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_contents3"));
                     ImGui.endTabItem();
                  }

                  ImGui.endTabBar();
               }

               if (ImGui.beginChild("##ScrollChild", 0.0F, 50.0F, true, 2048)) {
                  ImGui.text(
                     "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                  );
               }

               ImGui.endChild();
               if (ImGui.beginTable("##DemoTable", 2, 1920)) {
                  ImGui.tableNextColumn();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_table_00"));
                  ImGui.tableNextColumn();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_table_10"));
                  ImGui.tableNextColumn();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_table_01"));
                  ImGui.tableNextColumn();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.demo_table_11"));
                  ImGui.endTable();
               }

               ImGui.endTable();
            }
         }

         EditorWindowType.STYLE_EDITOR.end();
      }
   }

   private static void renderColorChanger(ImGuiStyle currentStyle, ImGuiStyle baseStyle, int colId, String name) {
      currentStyle.getColor(colId, colorBuf);
      colorBufArr[0] = colorBuf.x;
      colorBufArr[1] = colorBuf.y;
      colorBufArr[2] = colorBuf.z;
      colorBufArr[3] = colorBuf.w;
      ImGui.pushID(colId);
      int flags = 278560;
      if (ImGui.colorEdit4(name, colorBufArr, 278560)) {
         currentStyle.setColor(colId, colorBufArr[0], colorBufArr[1], colorBufArr[2], colorBufArr[3]);
      }

      baseStyle.getColor(colId, colorBuf);
      if (colorBufArr[0] != colorBuf.x || colorBufArr[1] != colorBuf.y || colorBufArr[2] != colorBuf.z || colorBufArr[3] != colorBuf.w) {
         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.editorui.window.style_editor.revert"))) {
            currentStyle.setColor(colId, colorBuf.x, colorBuf.y, colorBuf.z, colorBuf.w);
         }
      }

      ImGui.popID();
   }

   private static void renderVarChanger1(
      String name, ImGuiStyle currentStyle, ImGuiStyle baseStyle, int min, int max, StyleHelper.VarGetter1 getter, StyleHelper.VarSetter1 setter
   ) {
      float value = getter.get(currentStyle);
      if (min == 0 && max == 1) {
         if (ImGui.checkbox(name, value > 0.5)) {
            setter.set(currentStyle, value < 0.5 ? 1.0F : 0.0F);
         }
      } else {
         colorBufArr[0] = value;
         ImGui.setNextItemWidth(150.0F);
         if (ImGui.sliderFloat(name, colorBufArr, min, max, "%.0f")) {
            setter.set(currentStyle, colorBufArr[0]);
         }
      }

      float baseValue = getter.get(baseStyle);
      if (baseValue != value) {
         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.editorui.window.style_editor.revert") + "##Revert" + name)) {
            setter.set(currentStyle, baseValue);
         }
      }
   }
}
