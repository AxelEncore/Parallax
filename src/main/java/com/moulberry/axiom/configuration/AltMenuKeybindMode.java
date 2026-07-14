package com.moulberry.axiom.configuration;

import net.minecraft.client.resources.language.I18n;

public enum AltMenuKeybindMode {
    HOLD("axiom.config.context_menu.keybind_mode.hold"),
    TOGGLE("axiom.config.context_menu.keybind_mode.toggle");

    private final String text;

    AltMenuKeybindMode(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return I18n.get(this.text);
    }
}
