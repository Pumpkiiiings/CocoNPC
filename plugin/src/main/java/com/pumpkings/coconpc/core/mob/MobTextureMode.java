package com.pumpkings.coconpc.core.mob;

public enum MobTextureMode {
    /**
     * Generates one 64x64 player skin per cube and renders each cube through a
     * PLAYER_HEAD item. This is the mode that reproduces the .bbmodel textures.
     */
    SKINS,
    /**
     * Approximates each cube with the closest concrete block. No MineSkin
     * uploads are needed, so it works without an API key or internet access.
     */
    BLOCKS;

    public static MobTextureMode parse(String value, MobTextureMode fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "skin", "skins", "head", "heads", "texture", "textures" -> SKINS;
            case "block", "blocks", "concrete" -> BLOCKS;
            default -> fallback;
        };
    }
}
