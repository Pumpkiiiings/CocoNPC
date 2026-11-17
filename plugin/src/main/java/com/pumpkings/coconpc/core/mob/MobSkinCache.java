package com.pumpkings.coconpc.core.mob;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent content-addressed cache for generated cube skins. The key is the
 * SHA-256 hash of the composed 64x64 PNG, so two cubes that resolve to the
 * same pixels only ever trigger one MineSkin upload.
 */
public final class MobSkinCache {
    private final File file;
    private final Map<String, String> urls = new LinkedHashMap<>();

    public MobSkinCache(File file) {
        this.file = file;
    }

    public synchronized void load() {
        urls.clear();
        if (file == null || !file.isFile()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("skins");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String url = section.getString(key);
            if (key != null && url != null && !url.isBlank()) {
                urls.put(key, url);
            }
        }
    }

    public synchronized String get(String hash) {
        return hash == null ? null : urls.get(hash);
    }

    public synchronized boolean contains(String hash) {
        return hash != null && urls.containsKey(hash);
    }

    public synchronized void put(String hash, String url) {
        if (hash == null || url == null || url.isBlank()) return;
        urls.put(hash, url);
    }

    public synchronized void save() {
        if (file == null) return;

        YamlConfiguration config = new YamlConfiguration();
        config.options().setHeader(java.util.List.of(
                "CocoNPC generated cube skins.",
                "Keys are SHA-256 hashes of the composed 64x64 PNG images;",
                "values are the MineSkin texture URLs. Do not edit by hand."));
        for (Map.Entry<String, String> entry : urls.entrySet()) {
            config.set("skins." + entry.getKey(), entry.getValue());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                return;
            }
            config.save(file);
        } catch (IOException exception) {
            // The caller logs; avoid a Bukkit dependency here.
        }
    }

    public synchronized int size() {
        return urls.size();
    }
}
