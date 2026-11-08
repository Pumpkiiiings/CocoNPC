package com.pumpkings.coconpc.core.config;

import java.util.List;
import java.util.UUID;

public record NpcData(
    UUID uuid,
    String id,
    List<String> actions,
    float size,
    List<String> hologramLines,
    String hologramBillboard,
    boolean hologramShadowed,
    String hologramBackground,
    org.bukkit.inventory.ItemStack rightHandItem,
    org.bukkit.inventory.ItemStack leftHandItem,
    String animation
) {
    public NpcData(UUID uuid, String id, List<String> actions, float size, List<String> hologramLines, String hologramBillboard, boolean hologramShadowed, String hologramBackground, org.bukkit.inventory.ItemStack rightHandItem, org.bukkit.inventory.ItemStack leftHandItem) {
        this(uuid, id, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem, null);
    }

    public NpcData(UUID uuid, String id, List<String> actions, float size, List<String> hologramLines, String hologramBillboard, boolean hologramShadowed, String hologramBackground) {
        this(uuid, id, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), null);
    }

    public NpcData(UUID uuid, String id, List<String> actions, float size, List<String> hologramLines) {
        this(uuid, id, actions, size, hologramLines, "CENTER", true, "transparent", new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), null);
    }

    public static NpcData defaults(UUID uuid) {
        return new NpcData(uuid, "", List.of(), 1.0f, List.of("<yellow>CocoNPC"), "CENTER", true, "transparent", new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), null);
    }

    public NpcData withId(String newId) {
        return new NpcData(uuid, newId, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem, animation);
    }

    public NpcData withActions(List<String> newActions) {
        return new NpcData(uuid, id, newActions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem, animation);
    }

    public NpcData withSize(float newSize) {
        return new NpcData(uuid, id, actions, newSize, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem, animation);
    }

    public NpcData withHologramLines(List<String> newLines) {
        return new NpcData(uuid, id, actions, size, newLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem, animation);
    }

    public NpcData withHologramStyle(String newBillboard, boolean newShadowed, String newBackground) {
        return new NpcData(uuid, id, actions, size, hologramLines, newBillboard, newShadowed, newBackground, rightHandItem, leftHandItem, animation);
    }

    public NpcData withHandItems(org.bukkit.inventory.ItemStack newRight, org.bukkit.inventory.ItemStack newLeft) {
        return new NpcData(uuid, id, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, newRight, newLeft, animation);
    }

    public NpcData withAnimation(String newAnimation) {
        return new NpcData(uuid, id, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem, newAnimation);
    }
}
