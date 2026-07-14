package com.moulberry.axiom.editor.keybinds;

import java.util.List;

public record KeybindCategory(String name, boolean preventPassToGame, List<Keybind> keybinds) {
}
