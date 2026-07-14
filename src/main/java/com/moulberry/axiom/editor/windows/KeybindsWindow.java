package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.keybinds.KeybindCategory;
import com.moulberry.axiom.editor.keybinds.KeybindDefaults;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeybindsWindow {
   private static String backedUpKeybind = null;
   private static int backupKey;
   private static boolean backupShiftMod;
   private static boolean backupCtrlMod;
   private static boolean backupAltMod;
   private static boolean backupSuperMod;

   public static void render() {
      if (EditorWindowType.KEYBINDS.isOpen()) {
         String title = EditorWindowType.KEYBINDS.getName() + "###Keybinds";
         if (!ImGui.isPopupOpen("###Keybinds")) {
            ImGui.openPopup("###Keybinds");
         }

         ImVec2 center = ImGui.getMainViewport().getCenter();
         ImGui.setNextWindowPos(center.x, center.y, 8, 0.5F, 0.5F);
         ImGui.setNextWindowSize(720.0F, 500.0F, 4);
         ImGui.setNextWindowSizeConstraints(510.0F, 350.0F, 5000.0F, 3000.0F);
         if (ImGuiHelper.beginPopupModalCloseable(title)) {
            renderInner();
            ImGuiHelper.endPopupModalCloseable();
         }

         if (!ImGui.isPopupOpen("###Keybinds")) {
            EditorWindowType.KEYBINDS.setOpen(false);
         }
      }
   }

   private static void renderInner() {
      float warningSymbolWidth = ImGuiHelper.calcTextWidth("⚠");
      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.keybinds.load_default"))) {
         ImGui.openPopup("##DefaultSelector");
      }

      if (ImGui.beginPopup("##DefaultSelector")) {
         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.keybinds.load_axiom_defaults"))) {
            KeybindDefaults.loadAxiom();
            ImGui.closeCurrentPopup();
         }

         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.keybinds.load_blender_defaults"))) {
            KeybindDefaults.loadBlender();
            ImGui.closeCurrentPopup();
         }

         ImGui.endPopup();
      }

      ImGui.separator();

      for (KeybindCategory category : Keybinds.categories) {
         String translated = AxiomI18n.get(category.name());
         if (ImGui.treeNode(translated + "##" + category.name())) {
            if (category.name().equals("axiom.keybind_categories.movement")) {
               String useEnhancedFlightLabel = AxiomI18n.get("axiom.keybind_options.use_enhanced_flight");
               if (ImGui.checkbox(useEnhancedFlightLabel + "##UseEnhancedFlight", !Axiom.configuration.movement.syncIngameMovementWithEditorUI)) {
                  Axiom.configuration.movement.syncIngameMovementWithEditorUI = !Axiom.configuration.movement.syncIngameMovementWithEditorUI;
               }

               String useVanillaMovementLabel = AxiomI18n.get("axiom.keybind_options.use_vanilla_movement");
               if (ImGui.checkbox(useVanillaMovementLabel + "##UseVanillaMovement", Keybinds.useVanillaMovement)) {
                  Keybinds.useVanillaMovement = !Keybinds.useVanillaMovement;
               }

               if (Keybinds.useVanillaMovement) {
                  ImGui.treePop();
                  continue;
               }
            }

            for (Keybind keybind : category.keybinds()) {
               String descriptionRaw = keybind.getDescriptionRaw();
               String description = keybind.getDescription();
               if (ImGui.selectable(description + "##" + descriptionRaw, false, 1)) {
                  ImGui.openPopup(descriptionRaw + "##EditKeybind");
               }

               ImGui.sameLine();
               String longKeyIdent = keybind.longKeyIdentifier();
               StringBuilder conflicts = null;
               if (keybind.getKey() != 0) {
                  boolean isIngameKeybind = Keybinds.INGAME_KEYBINDS.contains(keybind);

                  for (Keybind other : Keybinds.keybindsForKey.get(keybind.getKey())) {
                     if (other != keybind
                        && keybind != Keybinds.USE_TOOL
                        && other != Keybinds.USE_TOOL
                        && (keybind != Keybinds.QUICK_REPLACE || other != Keybinds.ROTATE_PLACEMENT)
                        && (keybind != Keybinds.ROTATE_PLACEMENT || other != Keybinds.QUICK_REPLACE)
                        && (keybind != Keybinds.QUICK_FILL || other != Keybinds.FLIP_PLACEMENT)
                        && (keybind != Keybinds.FLIP_PLACEMENT || other != Keybinds.QUICK_FILL)
                        && (keybind != Keybinds.EXTRUDE_POINT || !other.getDescriptionRaw().equals("elevation"))
                        && (!keybind.getDescriptionRaw().equals("elevation") || other != Keybinds.EXTRUDE_POINT)
                        && isIngameKeybind == Keybinds.INGAME_KEYBINDS.contains(other)
                        && keybind.hasSameMods(other)) {
                        if (conflicts == null) {
                           conflicts = new StringBuilder(AxiomI18n.get("axiom.editorui.window.keybinds.conflicts_with"));
                        }

                        conflicts.append('\n').append(other.getDescription());
                     }
                  }
               }

               float availX = ImGui.getContentRegionAvailX();
               float width = ImGuiHelper.calcTextWidth(longKeyIdent) + ImGui.getStyle().getFramePaddingX() * 2.0F;
               if (conflicts != null) {
                  width += warningSymbolWidth + ImGui.getStyle().getItemSpacingX();
               }

               ImGui.sameLine(0.0F, availX - width);
               if (conflicts != null) {
                  ImGui.text("⚠");
                  ImGuiHelper.tooltip(conflicts.toString());
                  ImGui.sameLine();
               }

               ImGui.smallButton(longKeyIdent);
               if (descriptionRaw.equals("rotate_camera")) {
                  String invertCameraRotateLabel = AxiomI18n.get("axiom.keybind_options.invert_camera_rotate");
                  if (ImGui.checkbox(invertCameraRotateLabel + "##InvertCameraRotate", Axiom.configuration.internal.invertCameraRotate)) {
                     Axiom.configuration.internal.invertCameraRotate = !Axiom.configuration.internal.invertCameraRotate;
                  }
               } else if (descriptionRaw.equals("arcball_camera")) {
                  String useCenterOfScreenForArcballLabel = "Use Viewport Center for Arcball";
                  if (ImGui.checkbox(
                     useCenterOfScreenForArcballLabel + "##UseCenterOfScreenForArcball", Axiom.configuration.internal.useCenterOfScreenForArcball
                  )) {
                     Axiom.configuration.internal.useCenterOfScreenForArcball = !Axiom.configuration.internal.useCenterOfScreenForArcball;
                  }
               } else if (descriptionRaw.equals("pick_block")) {
                  if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.pick_block_drag"), Axiom.configuration.internal.pickBlockDrag)) {
                     Axiom.configuration.internal.pickBlockDrag = !Axiom.configuration.internal.pickBlockDrag;
                  }
               } else if (descriptionRaw.equals("cut") && ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.cut_copy_to_clipboard"), Axiom.configuration.internal.cutAlsoCopiesToClipboard)) {
                  Axiom.configuration.internal.cutAlsoCopiesToClipboard = !Axiom.configuration.internal.cutAlsoCopiesToClipboard;
               }

               if (ImGuiHelper.beginPopup(descriptionRaw + "##EditKeybind")) {
                  ImGuiHelper.setEditingKeybind(keybind);
                  if (backedUpKeybind == null || !backedUpKeybind.equals(keybind.getDescriptionRaw())) {
                     backedUpKeybind = keybind.getDescriptionRaw();
                     backupKey = keybind.getKey();
                     backupShiftMod = keybind.isShiftMod();
                     backupCtrlMod = keybind.isCtrlMod();
                     backupAltMod = keybind.isAltMod();
                     backupSuperMod = keybind.isSuperMod();
                  }

                  ImGui.text(description);
                  ImGui.button(longKeyIdent + "##KeybindPreview", -1.0F, 0.0F);
                  if (ImGui.isItemHovered()) {
                     for (int i = 0; i < 5; i++) {
                        if (ImGui.isMouseClicked(i)) {
                           long window = ImGui.getWindowViewport().getPlatformHandle();
                           boolean shiftDown = GLFW.glfwGetKey(window, 340) != 0 || GLFW.glfwGetKey(window, 344) != 0;
                           boolean ctrlDown = GLFW.glfwGetKey(window, 341) != 0 || GLFW.glfwGetKey(window, 345) != 0;
                           boolean altDown = GLFW.glfwGetKey(window, 342) != 0 || GLFW.glfwGetKey(window, 346) != 0;
                           boolean superDown = GLFW.glfwGetKey(window, 343) != 0 || GLFW.glfwGetKey(window, 347) != 0;
                           keybind.set(-i - 1, shiftDown, Minecraft.ON_OSX ? superDown : ctrlDown, altDown, Minecraft.ON_OSX ? ctrlDown : superDown);
                        }
                     }
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.widget.ok"))) {
                     backedUpKeybind = null;
                     ImGui.closeCurrentPopup();
                  }

                  ImGui.sameLine();
                  if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
                     keybind.set(backupKey, backupShiftMod, backupCtrlMod, backupAltMod, backupSuperMod);
                     backedUpKeybind = null;
                     ImGui.closeCurrentPopup();
                  }

                  ImGui.sameLine();
                  if (ImGui.button(AxiomI18n.get("axiom.editorui.window.keybinds.unbind"))) {
                     keybind.clear();
                     backedUpKeybind = null;
                     ImGui.closeCurrentPopup();
                  }

                  ImGui.endPopup();
               }
            }

            ImGui.treePop();
         }
      }
   }
}
