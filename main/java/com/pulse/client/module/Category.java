package com.pulse.client.module;

public enum Category {
    MOVEMENT("Movement"),
    COMBAT("Combat"),
    RENDER("Render"),
    PLAYER("Player");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
