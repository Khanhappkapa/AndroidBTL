package com.example.slime.entities;

/**
 * Enum representing playable characters in the SlimeJump game.
 */
public enum GameCharacter {
    SLIME("Slime", "A bouncy blue slime with incredible jumping abilities!");

    private final String displayName;
    private final String description;

    GameCharacter(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
