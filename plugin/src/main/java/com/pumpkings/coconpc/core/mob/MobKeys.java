package com.pumpkings.coconpc.core.mob;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class MobKeys {
    private final NamespacedKey instance;
    private final NamespacedKey definition;
    private final NamespacedKey bone;

    public MobKeys(Plugin plugin) {
        this.instance = new NamespacedKey(plugin, "mob_instance");
        this.definition = new NamespacedKey(plugin, "mob_definition");
        this.bone = new NamespacedKey(plugin, "mob_bone");
    }

    public NamespacedKey getInstance() {
        return instance;
    }

    public NamespacedKey getDefinition() {
        return definition;
    }

    public NamespacedKey getBone() {
        return bone;
    }
}
