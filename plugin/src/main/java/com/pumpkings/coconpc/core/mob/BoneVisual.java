package com.pumpkings.coconpc.core.mob;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

/**
 * Visual representation of a single cube. A cube can be rendered as a block,
 * as an arbitrary item, or as a player head carrying a generated skin.
 */
public final class BoneVisual {
    private final ItemStack item;
    private final BlockData blockData;
    private final String skinUrl;
    private final float scale;
    private final ItemDisplay.ItemDisplayTransform transform;

    private BoneVisual(ItemStack item, BlockData blockData, String skinUrl,
                       float scale, ItemDisplay.ItemDisplayTransform transform) {
        this.item = item;
        this.blockData = blockData;
        this.skinUrl = skinUrl;
        this.scale = scale <= 0f ? 1f : scale;
        this.transform = transform;
    }

    public static BoneVisual item(ItemStack itemStack) {
        return new BoneVisual(itemStack, null, null, 1f, null);
    }

    public static BoneVisual item(ItemStack itemStack, float scale) {
        return new BoneVisual(itemStack, null, null, scale, null);
    }

    public static BoneVisual item(ItemStack itemStack, ItemDisplay.ItemDisplayTransform transform) {
        return new BoneVisual(itemStack, null, null, 1f, transform);
    }

    /**
     * Cube rendered through a PLAYER_HEAD item whose texture comes from a
     * generated skin URL. Heads use an 8-pixel base instead of a 16-pixel base.
     */
    public static BoneVisual head(String skinUrl) {
        return new BoneVisual(null, null, skinUrl, 1f, ItemDisplay.ItemDisplayTransform.HEAD);
    }

    public static BoneVisual block(Material material) {
        return block(material.createBlockData());
    }

    public static BoneVisual block(BlockData blockData) {
        return new BoneVisual(null, blockData, null, 1f, null);
    }

    public static BoneVisual none() {
        return new BoneVisual(null, null, null, 1f, null);
    }

    public ItemStack item() {
        return item;
    }

    public BlockData blockData() {
        return blockData;
    }

    public String skinUrl() {
        return skinUrl;
    }

    public float scale() {
        return scale;
    }

    public ItemDisplay.ItemDisplayTransform transform() {
        return transform;
    }

    public boolean isBlock() {
        return blockData != null;
    }

    public boolean isHead() {
        return skinUrl != null && !skinUrl.isBlank();
    }

    public boolean isNone() {
        return item == null && blockData == null && !isHead();
    }
}
