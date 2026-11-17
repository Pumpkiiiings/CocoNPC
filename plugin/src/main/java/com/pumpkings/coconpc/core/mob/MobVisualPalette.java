package com.pumpkings.coconpc.core.mob;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MobVisualPalette {
    private final Map<String, BoneVisual> boneVisuals;
    private final Map<String, BoneVisual> textureVisuals;
    private final BoneVisual fallback;

    private MobVisualPalette(Map<String, BoneVisual> boneVisuals,
                             Map<String, BoneVisual> textureVisuals,
                             BoneVisual fallback) {
        this.boneVisuals = Collections.unmodifiableMap(new HashMap<>(boneVisuals));
        this.textureVisuals = Collections.unmodifiableMap(new HashMap<>(textureVisuals));
        this.fallback = fallback;
    }

    public static MobVisualPalette defaultPalette() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public BoneVisual resolve(String boneName, String textureName) {
        if (boneName != null) {
            BoneVisual visual = boneVisuals.get(boneName);
            if (visual != null) return visual;
        }
        if (textureName != null) {
            BoneVisual visual = textureVisuals.get(textureName);
            if (visual != null) return visual;
        }
        return fallback;
    }

    public BoneVisual getFallback() {
        return fallback;
    }

    public static final class Builder {
        private final Map<String, BoneVisual> boneVisuals = new HashMap<>();
        private final Map<String, BoneVisual> textureVisuals = new HashMap<>();
        private BoneVisual fallback = BoneVisual.block(Material.WHITE_CONCRETE);

        public Builder fallback(BoneVisual visual) {
            if (visual != null) {
                this.fallback = visual;
            }
            return this;
        }

        public Builder fallbackBlock(Material material) {
            this.fallback = BoneVisual.block(material);
            return this;
        }

        public Builder fallbackBlock(BlockData blockData) {
            this.fallback = BoneVisual.block(blockData);
            return this;
        }

        public Builder fallbackItem(ItemStack itemStack) {
            this.fallback = BoneVisual.item(itemStack);
            return this;
        }

        public Builder bone(String boneName, BoneVisual visual) {
            if (boneName != null && visual != null) {
                boneVisuals.put(boneName, visual);
            }
            return this;
        }

        public Builder boneBlock(String boneName, Material material) {
            return bone(boneName, BoneVisual.block(material));
        }

        public Builder boneItem(String boneName, ItemStack itemStack) {
            return bone(boneName, BoneVisual.item(itemStack));
        }

        public Builder texture(String textureName, BoneVisual visual) {
            if (textureName != null && visual != null) {
                textureVisuals.put(textureName, visual);
            }
            return this;
        }

        public Builder textureBlock(String textureName, Material material) {
            return texture(textureName, BoneVisual.block(material));
        }

        public Builder textureItem(String textureName, ItemStack itemStack) {
            return texture(textureName, BoneVisual.item(itemStack));
        }

        public MobVisualPalette build() {
            return new MobVisualPalette(boneVisuals, textureVisuals, fallback);
        }
    }
}
