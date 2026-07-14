package com.moulberry.axiom.editor.keybinds;

import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.configuration.legacy.KeybindConfigurationLegacy;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.utils.StringUtils;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Keybinds {
   public static Map<Integer, Set<Keybind>> keybindsForKey = new HashMap<>();
   public static final Keybind ROTATE_CAMERA = new Keybind("rotate_camera", -1, false, false, false, false);
   public static final Keybind PICK_BLOCK = new Keybind("pick_block", -3, false, false, false, false);
   public static final Keybind USE_TOOL = new Keybind("use_tool", -2, false, false, false, false);
   public static final Keybind ARCBALL_CAMERA = new Keybind("arcball_camera", -1, false, true, false, false);
   public static final Keybind ARCBALL_CARDINAL_SNAP = new Keybind("arcball_cardinal_snap", 0, false, false, false, false);
   public static final Keybind PAN_CAMERA = new Keybind("pan_camera", -2, false, true, false, false);
   public static final Keybind CROSSHAIR_CAMERA = new Keybind("crosshair_camera", -2, false, false, false, false);
   public static final Keybind ADJUST_RADIUS = new Keybind("adjust_radius", -3, false, true, false, false);
   public static final Keybind ADJUST_SPEED = new Keybind("adjust_speed", 0, false, false, false, false);
   public static final Keybind CONFIRM = new Keybind("confirm", 257, false, false, false, false);
   public static final Keybind UNDO = new Keybind("undo", 90, false, true, false, false);
   public static final Keybind REDO = new Keybind("redo", 89, false, true, false, false);
   public static final Keybind COPY = new Keybind("copy", 67, false, true, false, false);
   public static final Keybind COPY_WITH_AIR = new Keybind("copy_with_air", 0, false, false, false, false);
   public static final Keybind PASTE = new Keybind("paste", 86, false, true, false, false);
   public static final Keybind DELETE = new Keybind("delete", 261, false, false, false, false);
   public static final Keybind CUT = new Keybind("cut", 88, false, true, false, false);
   public static final Keybind DUPLICATE = new Keybind("duplicate", 74, false, true, false, false);
   public static final Keybind EXTRUDE_POINT = new Keybind("extrude_point", 69, false, false, false, false);
   public static final Keybind SAVE_BLUEPRINT = new Keybind("save_blueprint", 80, false, true, false, false);
   public static final Keybind SHOW_SELECTION = new Keybind("show_selection", 0, false, false, false, false);
   public static final Keybind SHOW_BIOMES = new Keybind("show_biomes", 66, false, true, false, false);
   public static final Keybind SHOW_ANNOTATIONS = new Keybind("show_annotations", 0, false, true, false, false);
   public static final Keybind SHOW_DISPLAY_ENTITY_GIZMOS = new Keybind("show_display_entity_gizmos", 0, false, false, false, false);
   public static final Keybind SHOW_MARKER_ENTITY_GIZMOS = new Keybind("show_marker_entity_gizmos", 0, false, false, false, false);
   public static final Keybind SHOW_COLLISION_MESH = new Keybind("show_collision_mesh", 0, false, false, false, false);
   public static final Keybind SHOW_LIGHT_BLOCKS = new Keybind("show_light_blocks", 0, false, false, false, false);
   public static final Keybind SHOW_STRUCTURE_VOID_BLOCKS = new Keybind("show_structure_void_blocks", 0, false, false, false, false);
   public static final Keybind QUICK_FILL = new Keybind("quick_fill", 70, false, true, false, false);
   public static final Keybind QUICK_REPLACE = new Keybind("quick_replace", 82, false, true, false, false);
   public static final Keybind ROTATE_PLACEMENT = new Keybind("rotate_placement", 82, false, true, false, false);
   public static final Keybind FLIP_PLACEMENT = new Keybind("flip_placement", 70, false, true, false, false);
   public static final Keybind PASTE_AND_SELECT = new Keybind("paste_and_select", 0, false, false, false, false);
   public static boolean useVanillaMovement = true;
   public static final Keybind MOVE_QUICK = new Keybind("move_quick", 341, false, false, false, false);
   public static final Keybind MOVE_FORWARD = new Keybind("move_forward", 87, false, false, false, false);
   public static final Keybind MOVE_LEFT = new Keybind("move_left", 65, false, false, false, false);
   public static final Keybind MOVE_BACKWARD = new Keybind("move_backward", 83, false, false, false, false);
   public static final Keybind MOVE_RIGHT = new Keybind("move_right", 68, false, false, false, false);
   public static final Keybind MOVE_UP = new Keybind("move_up", 32, false, false, false, false);
   public static final Keybind MOVE_DOWN = new Keybind("move_down", 340, false, false, false, false);
   public static final Keybind SWAP_TO_LAST_TOOL = new Keybind("swap_to_last_tool", 0, false, false, false, false);
   public static final Keybind COPY_INGAME = new Keybind("copy_ingame", 67, false, true, false, false);
   public static final Keybind BUILDER_TOOL_DELETE = new Keybind("builder_tool_delete", 261, false, false, false, false);
   public static final Keybind NUDGE_FORWARDS = new Keybind("nudge_forwards", 265, false, false, false, false);
   public static final Keybind NUDGE_BACKWARDS = new Keybind("nudge_backwards", 264, false, false, false, false);
   public static final Keybind NUDGE_RIGHT = new Keybind("nudge_right", 262, false, false, false, false);
   public static final Keybind NUDGE_LEFT = new Keybind("nudge_left", 263, false, false, false, false);
   public static final Keybind NUDGE_PLUS_Y = new Keybind("nudge_plus_y", 266, false, false, false, false);
   public static final Keybind NUDGE_MINUS_Y = new Keybind("nudge_minus_y", 267, false, false, false, false);
   public static final Keybind SELECT_VIEW_1 = new Keybind("select_view_1", 49, false, true, false, false);
   public static final Keybind SELECT_VIEW_2 = new Keybind("select_view_2", 50, false, true, false, false);
   public static final Keybind SELECT_VIEW_3 = new Keybind("select_view_3", 51, false, true, false, false);
   public static final Keybind SELECT_VIEW_4 = new Keybind("select_view_4", 52, false, true, false, false);
   public static final Keybind SELECT_VIEW_5 = new Keybind("select_view_5", 53, false, true, false, false);
   public static final Keybind SELECT_VIEW_6 = new Keybind("select_view_6", 54, false, true, false, false);
   public static final Keybind SELECT_VIEW_7 = new Keybind("select_view_7", 55, false, true, false, false);
   public static final Keybind SELECT_VIEW_8 = new Keybind("select_view_8", 56, false, true, false, false);
   public static final Keybind SELECT_VIEW_9 = new Keybind("select_view_9", 57, false, true, false, false);
   public static final List<Keybind> SELECT_VIEW_KEYBINDS = List.of(
      SELECT_VIEW_1, SELECT_VIEW_2, SELECT_VIEW_3, SELECT_VIEW_4, SELECT_VIEW_5, SELECT_VIEW_6, SELECT_VIEW_7, SELECT_VIEW_8, SELECT_VIEW_9
   );
   private static final KeybindCategory TOOL_KEYBINDS = ToolManager.createKeybindCategory();
   public static final List<Keybind> INGAME_KEYBINDS = List.of(COPY_INGAME, BUILDER_TOOL_DELETE);
   public static final List<KeybindCategory> categories = List.of(
      new KeybindCategory(
         "axiom.keybind_categories.basic",
         true,
         List.of(ROTATE_CAMERA, PICK_BLOCK, USE_TOOL, ARCBALL_CAMERA, ARCBALL_CARDINAL_SNAP, PAN_CAMERA, CROSSHAIR_CAMERA, ADJUST_RADIUS, ADJUST_SPEED)
      ),
      new KeybindCategory(
         "axiom.keybind_categories.edit", true, List.of(CONFIRM, UNDO, REDO, COPY, COPY_WITH_AIR, PASTE, DELETE, CUT, DUPLICATE, EXTRUDE_POINT, SAVE_BLUEPRINT)
      ),
      new KeybindCategory(
         "axiom.keybind_categories.view",
         true,
         List.of(
            SHOW_SELECTION,
            SHOW_BIOMES,
            SHOW_ANNOTATIONS,
            SHOW_DISPLAY_ENTITY_GIZMOS,
            SHOW_MARKER_ENTITY_GIZMOS,
            SHOW_COLLISION_MESH,
            SHOW_LIGHT_BLOCKS,
            SHOW_STRUCTURE_VOID_BLOCKS
         )
      ),
      new KeybindCategory("axiom.keybind_categories.operations", true, List.of(QUICK_FILL, QUICK_REPLACE)),
      new KeybindCategory("axiom.keybind_categories.placement", true, List.of(ROTATE_PLACEMENT, FLIP_PLACEMENT, PASTE_AND_SELECT)),
      new KeybindCategory(
         "axiom.keybind_categories.movement", false, List.of(MOVE_QUICK, MOVE_FORWARD, MOVE_LEFT, MOVE_BACKWARD, MOVE_RIGHT, MOVE_UP, MOVE_DOWN)
      ),
      TOOL_KEYBINDS,
      new KeybindCategory("axiom.keybind_categories.ingame", false, INGAME_KEYBINDS),
      new KeybindCategory(
         "axiom.keybind_categories.gizmo", false, List.of(NUDGE_FORWARDS, NUDGE_BACKWARDS, NUDGE_RIGHT, NUDGE_LEFT, NUDGE_PLUS_Y, NUDGE_MINUS_Y)
      )
   );
   public static boolean hasShiftCtrl = false;
   public static boolean hasShiftAlt = false;

   public static void updateMapping(Keybind keybind, int old) {
      if (keybind.isShiftMod() && keybind.isCtrlMod()) {
         hasShiftCtrl = true;
      } else if (categories != null && hasShiftCtrl) {
         hasShiftCtrl = false;

         label80:
         for (KeybindCategory category : categories) {
            for (Keybind other : category.keybinds()) {
               if (other != null && other.isShiftMod() && other.isCtrlMod()) {
                  hasShiftCtrl = true;
                  break label80;
               }
            }
         }
      }

      if (keybind.isShiftMod() && keybind.isAltMod()) {
         hasShiftAlt = true;
      } else if (categories != null && hasShiftAlt) {
         hasShiftAlt = false;

         label60:
         for (KeybindCategory category : categories) {
            for (Keybind otherx : category.keybinds()) {
               if (otherx != null && otherx.isShiftMod() && otherx.isAltMod()) {
                  hasShiftAlt = true;
                  break label60;
               }
            }
         }
      }

      if (keybind.getKey() != old) {
         if (old != 0 && keybindsForKey.containsKey(old)) {
            Set<Keybind> set = keybindsForKey.get(old);
            set.remove(keybind);
         }

         if (keybind.getKey() != 0) {
            keybindsForKey.computeIfAbsent(keybind.getKey(), k -> new HashSet<>()).add(keybind);
         }
      }
   }

   public static void loadLegacy(KeybindConfigurationLegacy keybindConfiguration) {
      for (Field field : Keybinds.class.getDeclaredFields()) {
         if ((field.getModifiers() & 8) != 0 && field.getType() == Keybind.class) {
            try {
               Keybind keybind = (Keybind)field.get(null);
               if (keybind != SWAP_TO_LAST_TOOL) {
                  String description = keybind.getDescriptionRaw();
                  description = StringUtils.convertSnakeToCamel(description);
                  String defaultValue = keybind.toConfigValue();
                  String keybindValue = keybindConfiguration.load(String.class, description, defaultValue);
                  keybindConfiguration.regularKeybinds.put(description, keybindValue);
               }
            } catch (Exception var9) {
               throw new RuntimeException(var9);
            }
         }
      }
   }

   public static void load(AxiomConfig axiomConfig) {
      for (Field field : Keybinds.class.getDeclaredFields()) {
         if ((field.getModifiers() & 8) != 0 && field.getType() == Keybind.class) {
            try {
               Keybind keybind = (Keybind)field.get(null);
               if (keybind != SWAP_TO_LAST_TOOL) {
                  String description = keybind.getDescriptionRaw();
                  description = StringUtils.convertSnakeToCamel(description);
                  String keybindValue = axiomConfig.keybinds.regularKeybinds.get(description);
                  if (keybindValue != null) {
                     keybind.loadFromConfigValue(keybindValue);
                  }
               }
            } catch (Exception var8) {
               throw new RuntimeException(var8);
            }
         }
      }

      for (Entry<String, String> entry : axiomConfig.keybinds.toolKeybinds.entrySet()) {
         for (Keybind keybind : TOOL_KEYBINDS.keybinds()) {
            if (keybind.getDescriptionRaw().equals(entry.getKey())) {
               keybind.loadFromConfigValue(entry.getValue());
            }
         }
      }
   }

   public static void save(AxiomConfig axiomConfig) {
      for (Field field : Keybinds.class.getDeclaredFields()) {
         if ((field.getModifiers() & 8) != 0 && field.getType() == Keybind.class) {
            try {
               Keybind keybind = (Keybind)field.get(null);
               if (keybind != SWAP_TO_LAST_TOOL) {
                  String description = keybind.getDescriptionRaw();
                  description = StringUtils.convertSnakeToCamel(description);
                  String keybindValue = keybind.toConfigValue();
                  axiomConfig.keybinds.regularKeybinds.put(description, keybindValue);
               }
            } catch (Exception var8) {
            }
         }
      }

      for (Keybind keybind : TOOL_KEYBINDS.keybinds()) {
         axiomConfig.keybinds.toolKeybinds.put(keybind.getDescriptionRaw(), keybind.toConfigValue());
      }
   }
}
