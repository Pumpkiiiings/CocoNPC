package com.pumpkings.coconpc.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public final class AttributeCompat {
    private AttributeCompat() {
    }

    public static Attribute maxHealth() {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
        if (attribute == null) {
            attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"));
        }
        return attribute;
    }
}
