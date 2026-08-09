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
    org.bukkit.inventory.ItemStack leftHandItem
) {
    public NpcData(UUID uuid, String id, List<String> actions, float size, List<String> hologramLines, String hologramBillboard, boolean hologramShadowed, String hologramBackground) {
        this(uuid, id, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
    }

    public NpcData(UUID uuid, String id, List<String> actions, float size, List<String> hologramLines) {
        this(uuid, id, actions, size, hologramLines, "CENTER", true, "transparent", new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
    }

    public static NpcData defaults(UUID uuid) {
        return new NpcData(uuid, "", List.of(), 1.0f, List.of("<yellow>CocoNPC"), "CENTER", true, "transparent", new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR), new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
    }

    public NpcData withId(String newId) {
        return new NpcData(uuid, newId, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem);
    }

    public NpcData withActions(List<String> newActions) {
        return new NpcData(uuid, id, newActions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem);
    }

    public NpcData withSize(float newSize) {
        return new NpcData(uuid, id, actions, newSize, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem);
    }

    public NpcData withHologramLines(List<String> newLines) {
        return new NpcData(uuid, id, actions, size, newLines, hologramBillboard, hologramShadowed, hologramBackground, rightHandItem, leftHandItem);
    }

    public NpcData withHologramStyle(String newBillboard, boolean newShadowed, String newBackground) {
        return new NpcData(uuid, id, actions, size, hologramLines, newBillboard, newShadowed, newBackground, rightHandItem, leftHandItem);
    }

    public NpcData withHandItems(org.bukkit.inventory.ItemStack newRight, org.bukkit.inventory.ItemStack newLeft) {
        return new NpcData(uuid, id, actions, size, hologramLines, hologramBillboard, hologramShadowed, hologramBackground, newRight, newLeft);
    }
}
