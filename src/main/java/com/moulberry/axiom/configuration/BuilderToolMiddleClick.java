package com.moulberry.axiom.configuration;

import net.minecraft.client.resources.language.I18n;

public enum BuilderToolMiddleClick {
    EXTEND_SELECT("axiom.config.builder_tools.middle_click.extend_select"),
    MAGIC_SELECT("axiom.config.builder_tools.middle_click.magic_select");

    private final String text;

    BuilderToolMiddleClick(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return I18n.get(this.text);
    }
}
