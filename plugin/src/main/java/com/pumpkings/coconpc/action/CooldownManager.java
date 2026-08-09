package com.pumpkings.coconpc.action;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {
    private static final long SAVE_DEBOUNCE_TICKS = 100L;

    private final CocoNPC plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean savePending = new java.util.concurrent.atomic.AtomicBoolean(false);

    public CooldownManager(CocoNPC plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create cooldowns.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        
        long now = System.currentTimeMillis();
        for (String uuidStr : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid UUID in cooldowns.yml: " + uuidStr);
                continue;
            }

            Map<String, Long> playerCooldowns = new ConcurrentHashMap<>();

            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(uuidStr);
            if (section != null) {
                for (String npcIdStr : section.getKeys(false)) {
                    long expiresAt = section.getLong(npcIdStr);
                    if (expiresAt <= now) continue;
                    playerCooldowns.put(npcIdStr, expiresAt);
                }
            }
            if (!playerCooldowns.isEmpty()) {
                cooldowns.put(uuid, playerCooldowns);
            }
        }
    }

    public void saveAsync() {
        if (!savePending.compareAndSet(false, true)) return;

        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            savePending.set(false);
            writeToDisk();
        }, SAVE_DEBOUNCE_TICKS);
    }

    public void flush() {
        savePending.set(false);
        writeToDisk();
    }

    private void writeToDisk() {
        long now = System.currentTimeMillis();
        YamlConfiguration toSave = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
            for (Map.Entry<String, Long> cd : entry.getValue().entrySet()) {
                if (cd.getValue() > now) {
                    toSave.set(entry.getKey().toString() + "." + cd.getKey(), cd.getValue());
                }
            }
        }
        try {
            toSave.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cooldowns.yml");
        }
    }

    public boolean tryCooldown(Player player, String npcId, String durationStr) {
        long durationMs = parseDuration(durationStr);
        if (durationMs <= 0) return true;
        
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        
        Long expiresAt = playerCooldowns.get(npcId);
        if (expiresAt != null && expiresAt > now) {
            long remaining = (expiresAt - now) / 1000;
            Message.COOLDOWN_WAIT.send(plugin, player, "{time}", String.valueOf(remaining));
            return false;
        }
        
        playerCooldowns.put(npcId, now + durationMs);
        saveAsync();
        return true;
    }

    static long parseDuration(String durationStr) {
        durationStr = durationStr.toLowerCase().trim();
        try {
            if (durationStr.endsWith("s")) {
                return Long.parseLong(durationStr.replace("s", "")) * 1000;
            } else if (durationStr.endsWith("m")) {
                return Long.parseLong(durationStr.replace("m", "")) * 60 * 1000;
            } else if (durationStr.endsWith("h")) {
                return Long.parseLong(durationStr.replace("h", "")) * 60 * 60 * 1000;
            } else if (durationStr.endsWith("d")) {
                return Long.parseLong(durationStr.replace("d", "")) * 24 * 60 * 60 * 1000;
            }
            return Long.parseLong(durationStr) * 1000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}



