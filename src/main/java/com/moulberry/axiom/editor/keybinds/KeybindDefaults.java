package com.moulberry.axiom.editor.keybinds;

public class KeybindDefaults {
   public static void loadAxiom() {
      Keybinds.ROTATE_CAMERA.set(-1, false, false, false, false);
      Keybinds.PICK_BLOCK.set(-3, false, false, false, false);
      Keybinds.USE_TOOL.set(-2, false, false, false, false);
      Keybinds.ARCBALL_CAMERA.set(-1, false, true, false, false);
      Keybinds.PAN_CAMERA.set(-2, false, true, false, false);
      Keybinds.CROSSHAIR_CAMERA.set(-2, false, false, false, false);
      Keybinds.ADJUST_RADIUS.set(-3, false, true, false, false);
      Keybinds.UNDO.set(90, false, true, false, false);
      Keybinds.REDO.set(89, false, true, false, false);
      Keybinds.CUT.set(88, false, true, false, false);
      Keybinds.COPY.set(67, false, true, false, false);
      Keybinds.PASTE.set(86, false, true, false, false);
      Keybinds.SAVE_BLUEPRINT.set(80, false, true, false, false);
      Keybinds.SHOW_SELECTION.set(0, false, false, false, false);
      Keybinds.SHOW_BIOMES.set(66, false, true, false, false);
      Keybinds.QUICK_FILL.set(70, false, true, false, false);
      Keybinds.QUICK_REPLACE.set(82, false, true, false, false);
      Keybinds.ROTATE_PLACEMENT.set(82, false, true, false, false);
      Keybinds.FLIP_PLACEMENT.set(70, false, true, false, false);
      Keybinds.useVanillaMovement = true;
      Keybinds.MOVE_QUICK.set(341, false, false, false, false);
      Keybinds.MOVE_FORWARD.set(87, false, false, false, false);
      Keybinds.MOVE_LEFT.set(65, false, false, false, false);
      Keybinds.MOVE_BACKWARD.set(83, false, false, false, false);
      Keybinds.MOVE_RIGHT.set(68, false, false, false, false);
      Keybinds.MOVE_UP.set(32, false, false, false, false);
      Keybinds.MOVE_DOWN.set(340, false, false, false, false);
   }

   public static void loadBlender() {
      loadAxiom();
      Keybinds.ROTATE_CAMERA.set(-2, false, false, false, false);
      Keybinds.PICK_BLOCK.set(-1, false, true, false, false);
      Keybinds.USE_TOOL.set(-1, false, false, false, false);
      Keybinds.ARCBALL_CAMERA.set(-3, false, false, false, false);
      Keybinds.PAN_CAMERA.set(-3, false, true, false, false);
      Keybinds.CROSSHAIR_CAMERA.set(-1, false, false, false, false);
      Keybinds.ADJUST_RADIUS.set(-2, false, true, false, false);
      Keybinds.UNDO.set(90, false, true, false, false);
      Keybinds.REDO.set(90, true, true, false, false);
   }
}
