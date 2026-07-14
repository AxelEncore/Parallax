package com.moulberry.axiom.editor.windows.global_mask;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.AndMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.AngleMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BiomeMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockAboveMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockAdjacentMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockBelowMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockNearMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockNeighborMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.CanSeeSkyMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.CoordMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.InSelectionMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.MaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.MaskWidgetWithChildren;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.NotMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.OffsetMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.OrMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.SurfaceMaskWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.CmdStringConverter;
import com.moulberry.axiom.mask.LuaHelper;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.VisualCodeConverter;
import com.moulberry.axiom.utils.BooleanWrapper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.extension.texteditor.TextEditor;
import imgui.moulberry92.extension.texteditor.TextEditorLanguage;
import imgui.moulberry92.type.ImString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

public class ToolMaskWindow {
   private static final ImString stringMask = ImGuiHelper.createResizableString(256);
   private static CmdStringConverter.CmdStringParseException lastParseException = null;
   private static MaskWidget widget = null;
   private static final List<ToolMaskWindow.FloatingWidget> floatingWidgets = new ArrayList<>();
   private static final Set<MaskWidget> removeFloating = new HashSet<>();
   private static MaskWidget dragDroppingWidget = null;
   private static boolean visualCodeDirty = false;
   private static int switchMaskTab = 0;
   private static boolean maskChangedForPresetWidget = false;
   private static final PresetWidget presetWidgetMask = new PresetWidget(ToolMaskWindow::loadSettings, ToolMaskWindow::writeSettings, "tool_masks");
   private static boolean scriptChangedForPresetWidget = false;
   private static final PresetWidget presetWidgetScript = new PresetWidget(
      ToolMaskWindow::loadSettingsScript, ToolMaskWindow::writeSettingsScript, "tool_masks_lua"
   );
   private static final TextEditor LUA_EDITOR = new TextEditor();
   private static long lastLuaCompile = 0L;
   private static boolean luaDirty = false;
   private static String lastLuaText = null;
   public static String luaExecutionError = null;
   private static final String LUA_DESCRIPTION = "Create a mask using a Lua script\nFunction must return true/false\n\n"
      + LuaHelper.getAvailableLuaFunctions(false);

   public static void render() {
      if (EditorWindowType.TOOL_MASKS.isOpen()) {
         ImGuiHelper.pushStyleVar(12, 0.0F);
         ImGui.setNextWindowSizeConstraints(510.0F, 350.0F, 5000.0F, 3000.0F);
         if (EditorWindowType.TOOL_MASKS.begin("###ToolMasks", false)) {
            if (Axiom.configuration.internal.showToolMaskOpenWarning) {
               ImGui.textColored(-11184641, AxiomI18n.get("axiom.hardcoded.warning_prefix"));
               ImGui.sameLine();
               ImGui.textWrapped(
                  "Masks only apply to tools while this window is open, docked or minimised. Closing this window will result in the mask deactivating"
               );
               if (ImGui.button(AxiomI18n.get("axiom.editorui.warning_confirmation"))) {
                  Axiom.configuration.internal.showToolMaskOpenWarning = false;
               }

               ImGui.separator();
            }

            if (ImGui.beginTabBar("##TabBar")) {
               int switchMaskTabTo = switchMaskTab;
               switchMaskTab = 0;
               if (ImGui.beginTabItem(AxiomI18n.get("axiom.hardcoded.mask"), switchMaskTabTo == 1 ? 2 : 0)) {
                  presetWidgetMask.displayImgui(maskChangedForPresetWidget);
                  maskChangedForPresetWidget = false;
                  ImGui.sameLine();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.presets"));
                  MaskManager.useLuaScript = false;
                  renderMaskSection();
                  ImGui.endTabItem();
               }

               if (ImGui.beginTabItem(AxiomI18n.get("axiom.hardcoded.scripting"), switchMaskTabTo == 2 ? 2 : 0)) {
                  presetWidgetScript.displayImgui(scriptChangedForPresetWidget);
                  scriptChangedForPresetWidget = false;
                  ImGui.sameLine();
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.presets"));
                  MaskManager.useLuaScript = true;
                  renderScriptingSection();
                  ImGui.endTabItem();
               }

               if (ImGui.beginTabItem(AxiomI18n.get("axiom.hardcoded.examples"))) {
                  ImGuiHelper.separatorWithText("Mask");
                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.single_block") + "##SingleBlockMask")) {
                     MaskElement maskElement = CmdStringConverter.fromCmdString("block = stone");
                     MaskManager.setConfiguredMask(maskElement, true, true);
                     switchMaskTab = 1;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.multiple_blocks"))) {
                     MaskElement maskElement = CmdStringConverter.fromCmdString("block = [stone,granite]");
                     MaskManager.setConfiguredMask(maskElement, true, true);
                     switchMaskTab = 1;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.grass_surface"))) {
                     MaskElement maskElement = CmdStringConverter.fromCmdString("sky & block = air & below = grass_block & angle >= 70");
                     MaskManager.setConfiguredMask(maskElement, true, true);
                     switchMaskTab = 1;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.lower_edge"))) {
                     MaskElement maskElement = CmdStringConverter.fromCmdString("offset(0,1,0){ adjacent = #axiom:solid & block = air }");
                     MaskManager.setConfiguredMask(maskElement, true, true);
                     switchMaskTab = 1;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.upper_edge"))) {
                     MaskElement maskElement = CmdStringConverter.fromCmdString("adjacent = air & above = air");
                     MaskManager.setConfiguredMask(maskElement, true, true);
                     switchMaskTab = 1;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.shoreline"))) {
                     MaskElement maskElement = CmdStringConverter.fromCmdString("near(4) = water & above = air");
                     MaskManager.setConfiguredMask(maskElement, true, true);
                     switchMaskTab = 1;
                  }

                  ImGuiHelper.separatorWithText("Scripting");
                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.single_block") + "##SingleBlockScript")) {
                     LUA_EDITOR.setText("return getBlock(x, y, z) == blocks.stone");
                     luaDirty = true;
                     switchMaskTab = 2;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.top_3_4_blocks"))) {
                     LUA_EDITOR.setText(
                        "local depth = getHighestBlockYAt(x, z) - y\n\nif depth == 3 then\n    return math.random() < 0.25\nend\n\nreturn depth >= 0 and depth < 3"
                     );
                     luaDirty = true;
                     switchMaskTab = 2;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.simplex_noise_lbl"))) {
                     LUA_EDITOR.setText(
                        "local scale = 8\nlocal threshold = 0.5\nlocal noise = getSimplexNoise(x/scale, y/scale, z/scale)\nreturn noise < threshold"
                     );
                     luaDirty = true;
                     switchMaskTab = 2;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.voronoi_edges_noise_lbl"))) {
                     LUA_EDITOR.setText("local scale = 8\nlocal noise = getVoronoiEdgeNoise(x/scale, y/scale, z/scale)\nreturn noise < 0.5/scale");
                     luaDirty = true;
                     switchMaskTab = 2;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.invalid_soil_blocks"))) {
                     LUA_EDITOR.setText(
                        "local soil = {\n    [blocks.grass_block] = true,\n    [blocks.dirt] = true\n}\n\nlocal block = getBlock(x, y, z)\nif not soil[block] then\n    return false\nend\n\nlocal i = y+1\nwhile true do\n    local above = getBlock(x, i, z)\n    if not isSolid(above) then\n       return false\n    end\n    if soil[above] then\n        i = i + 1\n    else\n        return true\n    end\nend"
                     );
                     luaDirty = true;
                     switchMaskTab = 2;
                  }

                  ImGui.endTabItem();
               }

               ImGui.endTabBar();
            }
         }

         EditorWindowType.TOOL_MASKS.end();
         if (!(ImGui.getDragDropPayload("MaskWidget") instanceof MaskWidget maskWidget && maskWidget == dragDroppingWidget)) {
            dragDroppingWidget = null;
         } else if (ImGui.beginDragDropSource(16)) {
            maskWidget.render(false, new BooleanWrapper(false));
            ImGui.endDragDropSource();
         }

         if (visualCodeDirty) {
            if (widget != null) {
               MaskElement maskElement = VisualCodeConverter.fromVisualCode(widget);
               MaskManager.setConfiguredMask(maskElement, true, false);
            } else {
               MaskManager.setConfiguredMask(null, true, false);
            }

            visualCodeDirty = false;
         }

         ImGuiHelper.popStyleVar();
      }
   }

   private static void renderMaskSection() {
      String clearText = AxiomI18n.get("axiom.editorui.mainmenu.toolmasks.clear");
      float clearWidth = ImGuiHelper.calcTextWidth(clearText);
      ImGui.setNextItemWidth(-clearWidth - ImGui.getStyle().getFramePaddingX() * 2.0F - ImGui.getStyle().getItemSpacingX());
      if (ImGui.inputText("##StringMask", stringMask)) {
         String string = ImGuiHelper.getString(stringMask).trim();
         if (string.isEmpty()) {
            MaskManager.setConfiguredMask(null, false, true);
         } else {
            try {
               MaskElement maskElement = CmdStringConverter.fromCmdString(string);
               MaskManager.setConfiguredMask(maskElement, false, true);
               lastParseException = null;
            } catch (CmdStringConverter.CmdStringParseException var11) {
               lastParseException = var11;
            }
         }
      }

      ImGui.sameLine();
      if (ImGui.button(clearText)) {
         MaskManager.setConfiguredMask(null, true, true);
         maskChangedForPresetWidget = false;
         presetWidgetMask.setDefault();
      }

      if (lastParseException != null) {
         renderParseException();
      }

      ImGui.separator();
      BooleanWrapper allowDragDropTarget = new BooleanWrapper(true);
      if (ImGui.beginTable("##Table", 2, 33554944)) {
         ImGui.tableSetupColumn("##MaskColumn", 8);
         ImGui.tableSetupColumn("##ToolboxColumn", 16);
         ImVec2 startPos = ImGui.getCursorScreenPos();
         ImGui.tableNextColumn();
         if (ImGui.beginChild("##MaskChild", -1.0F, -2.0F)) {
            if (widget != null) {
               widget.render(true, allowDragDropTarget);
            }

            ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());

            for (ToolMaskWindow.FloatingWidget floatingWidget : floatingWidgets) {
               ImGui.setCursorPos(floatingWidget.x, floatingWidget.y);
               ImGui.beginGroup();
               floatingWidget.widget.render(true, allowDragDropTarget);
               ImGui.endGroup();
            }

            ImGuiHelper.popStyleVar();
            floatingWidgets.removeIf(floatingWidget -> removeFloating.contains(floatingWidget.widget));
            removeFloating.clear();
         }

         ImGui.endChild();
         ImVec2 mousePos = ImGui.getMousePos();
         CustomBlockState dragDropped = ImGuiHelper.blockStateDragDropTarget();
         if (dragDropped != null) {
            widget = new BlockMaskWidget(dragDropped.getCustomBlock());
            markDirty(widget);
         }

         if (ImGui.beginDragDropTarget()) {
            EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class);
            List<CustomBlock> customBlocks = new ArrayList<>();
            if (droppedPalette != null) {
               for (CustomBlockStateOrTombstone block : droppedPalette.getBlocks()) {
                  if (block instanceof CustomBlockState customBlockState) {
                     customBlocks.add(customBlockState.getCustomBlock());
                  }
               }
            }

            if (!customBlocks.isEmpty()) {
               if (customBlocks.size() == 1) {
                  widget = new BlockMaskWidget(customBlocks.get(0));
                  markDirty(widget);
               } else {
                  MaskWidgetWithChildren orMask = new OrMaskWidget();

                  for (CustomBlock customBlock : customBlocks) {
                     orMask.addChild(-1, new BlockMaskWidget(customBlock));
                  }

                  widget = orMask;
                  markDirty(widget);
               }
            }

            ImGui.endDragDropTarget();
         }

         if (ImGui.beginDragDropTarget()) {
            MaskWidget maskWidget = (MaskWidget)ImGui.acceptDragDropPayload("MaskWidget", 3072);
            if (maskWidget != null && maskWidget.isDragging()) {
               boolean snap = widget == null && mousePos.y < startPos.y + 100.0F;
               if (snap) {
                  ImGui.getWindowDrawList()
                     .addRect(startPos.x, startPos.y + 2.0F, startPos.x + ImGui.getContentRegionAvailX(), startPos.y + 3.0F, ImGui.getColorU32(55));
               }

               if (ImGui.isMouseReleased(0)) {
                  if (snap) {
                     widget = maskWidget;
                     markDirty(widget);
                  } else {
                     ToolMaskWindow.FloatingWidget floatingWidget = new ToolMaskWindow.FloatingWidget(
                        mousePos.x - startPos.x, mousePos.y - startPos.y, maskWidget
                     );
                     floatingWidgets.add(floatingWidget);
                  }

                  maskWidget.setDragging(false);
               }
            }

            ImGui.endDragDropTarget();
         }

         ImGui.tableNextColumn();
         renderDefaultMaskWidgets();
         ImGui.endTable();
      }
   }

   private static void renderDefaultMaskWidgets() {
      boolean canDragDrop = ImGui.getDragDropPayload("MaskWidget") == null;
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.tool_masks.logic"));
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_or") + " (|)");
      ImGuiHelper.tooltip("If any input is true, then the output is true");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new OrMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_and") + " (&)");
      ImGuiHelper.tooltip("All inputs must be true for the output to be true");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new AndMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_not") + " (!)");
      ImGuiHelper.tooltip("Inverts the input; the input must be false for the output to be true");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new NotMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_offset"));
      ImGuiHelper.tooltip("Offsets the target position, affecting all masks inside");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new OffsetMaskWidget(0, 0, 0));
         ImGui.endDragDropSource();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.editorui.window.tool_masks.masks"));
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_block") + " =");
      ImGuiHelper.tooltip("Matches the block at the target position");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BlockMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_above") + " =");
      ImGuiHelper.tooltip("Matches the block above the target position");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BlockAboveMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_below") + " =");
      ImGuiHelper.tooltip("Matches the block below the target position");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BlockBelowMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_near") + " =");
      ImGuiHelper.tooltip("Checks if any block in a radius from the target position matches");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BlockNearMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_neighbor") + " =");
      ImGuiHelper.tooltip("Checks if any block touching the target position matches");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BlockNeighborMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_adjacent") + " =");
      ImGuiHelper.tooltip("Checks if any block horizontally touching the target position matches");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BlockAdjacentMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.separator();
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_biome") + " =");
      ImGuiHelper.tooltip("Matches the biome at the target position");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new BiomeMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.separator();
      ImGui.button("Y =");
      ImGuiHelper.tooltip("Compares a constant value to the target y position");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new CoordMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_angle") + " =");
      ImGuiHelper.tooltip("Compares a constant value to the vertical angle of the surface");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new AngleMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.separator();
      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_inselection"));
      ImGuiHelper.tooltip("Checks if the target position is inside the selection");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new InSelectionMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_can_see_sky"));
      ImGuiHelper.tooltip("Checks if the target position has direct access to the sky");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new CanSeeSkyMaskWidget());
         ImGui.endDragDropSource();
      }

      ImGui.button(AxiomI18n.get("axiom.editorui.window.tool_masks.mask_surface"));
      ImGuiHelper.tooltip("Checks if the target position is on the surface");
      if (ImGui.isItemHovered()) {
         ImGui.setMouseCursor(7);
      }

      if (canDragDrop && ImGui.beginDragDropSource()) {
         setDragDroppingWidget(new SurfaceMaskWidget());
         ImGui.endDragDropSource();
      }
   }

   private static void renderParseException() {
      float framePaddingX = ImGui.getStyle().getFramePaddingX();
      String string = ImGuiHelper.getString(stringMask);
      if (!string.isBlank()) {
         if (ImGuiHelper.calcTextWidth(string) > ImGui.getContentRegionAvailX() - framePaddingX * 2.0F) {
            ImGui.textWrapped(lastParseException.message);
         } else {
            int startIndex = lastParseException.start - 1;
            int endIndex = lastParseException.end - 1;
            boolean trimming = true;

            for (int i = 0; i < string.length(); i++) {
               char c = string.charAt(i);
               if (trimming) {
                  if (c <= ' ') {
                     startIndex++;
                     endIndex++;
                     continue;
                  }

                  trimming = false;
               }

               if (endIndex <= i) {
                  break;
               }

               if (c == ':') {
                  if (startIndex > i) {
                     startIndex = Math.max(i, startIndex - 8);
                  }

                  endIndex = Math.max(i, endIndex - 8);
               } else if (c == '#') {
                  if (startIndex > i) {
                     startIndex = Math.max(i, startIndex - 10);
                  }

                  endIndex = Math.max(i, endIndex - 10);
               }
            }

            if (startIndex < 0) {
               startIndex = 0;
            }

            if (startIndex == endIndex) {
               endIndex++;
            }

            if (endIndex > string.length()) {
               endIndex = string.length();
            }

            if (startIndex > 0 && startIndex == endIndex) {
               startIndex--;
            }

            String startString = string.substring(0, startIndex);
            String endString = string.substring(0, endIndex);
            float startWidth = ImGuiHelper.calcTextWidth(startString);
            float endWidth = ImGuiHelper.calcTextWidth(endString);
            StringBuilder current = new StringBuilder();
            float lastWidth = 0.0F;

            while (true) {
               current.append(' ');
               float width = ImGuiHelper.calcTextWidth(current.toString());
               if (width > startWidth) {
                  float factor = (startWidth - lastWidth) / (width - lastWidth);
                  if (factor < 0.75) {
                     current.setLength(current.length() - 1);
                  } else {
                     lastWidth = width;
                  }

                  while (true) {
                     current.append('^');
                     width = ImGuiHelper.calcTextWidth(current.toString());
                     if (width > endWidth) {
                        factor = (endWidth - lastWidth) / (width - lastWidth);
                        if (factor < 0.25 && current.length() > 0) {
                           current.setLength(current.length() - 1);
                        }

                        ImGui.setCursorPosX(ImGui.getCursorPosX() + framePaddingX);
                        ImGui.text(current.toString());
                        ImGui.textWrapped(lastParseException.message);
                        return;
                     }

                     lastWidth = width;
                  }
               }

               lastWidth = width;
            }
         }
      }
   }

   private static void renderScriptingSection() {
      if (luaExecutionError != null) {
         ImGui.textColored(-12566273, AxiomI18n.get("axiom.hardcoded.script_error"));
         ImGui.textWrapped(luaExecutionError);
         if (ImGui.button(AxiomI18n.get("axiom.generic.close"))) {
            luaExecutionError = null;
         }

         ImGui.separator();
      }

      ImGuiHelper.separatorWithText("Lua Script Mask (Hover for help)");
      ImGuiHelper.tooltip(LUA_DESCRIPTION);
      ImGui.pushFont(EditorUI.monospace, EditorUI.monospace.getLegacySize());
      LUA_EDITOR.render("TextEditor");
      ImGui.popFont();
      String text = LUA_EDITOR.getText();
      if (!Objects.equals(lastLuaText, text)) {
         lastLuaText = text;
         luaDirty = true;
      }

      if (luaDirty) {
         long time = System.currentTimeMillis();
         if (time > lastLuaCompile + 250L) {
            lastLuaCompile = time;
            luaDirty = false;
            Globals globals = LuaHelper.createSandboxed();
            LUA_EDITOR.clearMarkers();

            try {
               String script = LUA_EDITOR.getText();
               LuaValue luaValue = globals.load(script);
               if (!luaValue.isfunction()) {
                  LUA_EDITOR.addMarker(1, -2145378176, -2145378176, "not a function");
                  MaskManager.luaScript = null;
               } else {
                  MaskManager.luaScript = script;
                  scriptChangedForPresetWidget = true;
               }
            } catch (LuaError var11) {
               String message = var11.getMessage();
               Pattern pattern = Pattern.compile(":(\\d+):");
               Matcher matcher = pattern.matcher(message);
               int line = 1;
               if (matcher.find()) {
                  line = Integer.parseInt(matcher.group(1));
               }

               int totalLines = LUA_EDITOR.getLineCount();
               if (line > totalLines) {
                  line = totalLines;
               }

               String[] splitMessage = message.split(":");
               message = splitMessage[splitMessage.length - 1];
               LUA_EDITOR.addMarker(line, -2145378176, -2145378176, message);
               MaskManager.luaScript = null;
            }
         }
      }
   }

   public static void loadSettings(CompoundTag compoundTag) {
      String mask = VersionUtilsNbt.helperCompoundTagGetStringOr(compoundTag, "mask", "").trim();
      if (mask.isEmpty()) {
         MaskManager.setConfiguredMask(null, true, true);
      } else {
         try {
            MaskElement maskElement = CmdStringConverter.fromCmdString(mask);
            MaskManager.setConfiguredMask(maskElement, true, true);
            lastParseException = null;
         } catch (CmdStringConverter.CmdStringParseException var3) {
            lastParseException = var3;
         }
      }
   }

   public static void writeSettings(CompoundTag compoundTag) {
      compoundTag.put("mask", StringTag.valueOf(ImGuiHelper.getString(stringMask)));
   }

   public static void loadSettingsScript(CompoundTag compoundTag) {
      String lua = VersionUtilsNbt.helperCompoundTagGetStringOr(compoundTag, "lua", "");
      MaskManager.luaScript = lua;
      LUA_EDITOR.setText(lua);
      lastLuaText = lua;
   }

   public static void writeSettingsScript(CompoundTag compoundTag) {
      if (MaskManager.luaScript != null) {
         compoundTag.put("lua", StringTag.valueOf(MaskManager.luaScript));
      }
   }

   public static void setDragDroppingWidget(MaskWidget maskWidget) {
      if (dragDroppingWidget == null || !dragDroppingWidget.isDragging()) {
         if (EditorUI.isCtrlOrCmdDown()) {
            maskWidget = maskWidget.copy();
         } else {
            markDirty(maskWidget);
         }

         ImGui.setDragDropPayload("MaskWidget", maskWidget);
         maskWidget.setDragging(true);
         dragDroppingWidget = maskWidget;
         if (maskWidget.parent() != null) {
            maskWidget.parent().removeChild(maskWidget);
         } else if (widget == maskWidget) {
            widget = null;
         } else {
            removeFloating.add(maskWidget);
         }
      }
   }

   public static void markDirty(MaskWidget maskWidget) {
      while (maskWidget.parent() != null) {
         maskWidget = maskWidget.parent();
      }

      if (maskWidget == widget) {
         visualCodeDirty = true;
      }
   }

   public static void onMaskUpdated(MaskElement maskElement, boolean updateCmdString, boolean updateVisualCode) {
      maskChangedForPresetWidget = true;
      if (maskElement == null) {
         if (updateCmdString) {
            stringMask.set("");
         }

         if (updateVisualCode) {
            widget = null;
         }
      } else {
         if (updateCmdString) {
            stringMask.set(CmdStringConverter.toCmdString(maskElement));
            lastParseException = null;
         }

         if (updateVisualCode) {
            widget = VisualCodeConverter.toVisualCode(maskElement);
         }
      }
   }

   static {
      TextEditorLanguage lang = TextEditorLanguage.Lua();
      LUA_EDITOR.setLanguage(lang);
      LUA_EDITOR.setShowWhitespacesEnabled(false);
      LUA_EDITOR.setTabSize(4);
      LUA_EDITOR.setAutoIndentEnabled(true);
   }

   private record FloatingWidget(float x, float y, MaskWidget widget) {
   }
}
