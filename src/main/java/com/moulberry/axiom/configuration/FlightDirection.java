package com.moulberry.axiom.configuration;

import net.minecraft.client.resources.language.I18n;

public enum FlightDirection {
    HORIZONTAL("axiom.contextmenu.flight_direction.horizontal"),
    CAMERA("axiom.contextmenu.flight_direction.camera");

    private final String text;

    FlightDirection(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return I18n.get(this.text);
    }
}
