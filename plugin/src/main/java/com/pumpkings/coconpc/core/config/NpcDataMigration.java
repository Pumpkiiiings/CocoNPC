package com.pumpkings.coconpc.core.config;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

class NpcDataMigration {

    private static final String OLD_DATA = "CocoNPC.data";
    private static final String OLD_ACTIONS = "npcs.yml";

    private final CocoNPC plugin;
    private final File npcFolder;

    NpcDataMigration(CocoNPC plugin, File npcFolder) {
        this.plugin = plugin;
        this.npcFolder = npcFolder;
    }

    void runIfNeeded() {
        File oldData = new File(plugin.getDataFolder(), OLD_DATA);
        if (!oldData.exists()) return;

        File[] alreadyConverted = npcFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (alreadyConverted != null && alreadyConverted.length > 0) {
            plugin.getLogger().warning(OLD_DATA + " is still present but npcs/ already has data. "
                    + "The old file is being ignored — delete it once you are happy.");
            return;
        }

        plugin.getLogger().info("Migrating NPC storage to one file per NPC...");

        YamlConfiguration data = YamlConfiguration.loadConfiguration(oldData);
        File oldActionsFile = new File(plugin.getDataFolder(), OLD_ACTIONS);
        YamlConfiguration actions = YamlConfiguration.loadConfiguration(oldActionsFile);

        ConfigurationSection root = data.getConfigurationSection("data");
        if (root == null) {
            plugin.getLogger().info("Nothing to migrate — " + OLD_DATA + " has no NPC data.");
            backup(oldData, oldActionsFile);
            return;
        }

        int migrated = 0;
        int skipped = 0;

        for (String uuidStr : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid uuid during migration: " + uuidStr);
                skipped++;
                continue;
            }

            ConfigurationSection old = root.getConfigurationSection(uuidStr);
            if (old == null) {
                skipped++;
                continue;
            }

            int id = old.getInt("id", -1);
            if (id <= 0) {
                plugin.getLogger().warning("Skipping NPC " + uuidStr + " during migration: no valid id.");
                skipped++;
                continue;
            }

            File target = new File(npcFolder, id + ".yml");
            if (target.exists()) {
                plugin.getLogger().warning("Two NPCs claim id " + id + "; keeping the first and "
                        + "skipping " + uuidStr + ". Re-add it manually if you need it.");
                skipped++;
                continue;
            }

            if (writeConverted(target, uuid, id, old, actions)) {
                migrated++;
            } else {
                skipped++;
            }
        }

        plugin.getLogger().info("Migrated " + migrated + " NPC(s)"
                + (skipped > 0 ? ", skipped " + skipped : "") + ".");
        backup(oldData, oldActionsFile);
    }

    private boolean writeConverted(File target, UUID uuid, int id,
                                   ConfigurationSection old, YamlConfiguration actions) {
        YamlConfiguration out = new YamlConfiguration();

        out.set("uuid", uuid.toString());
        out.set("id", id);
        out.set("size", old.getDouble("size", 1.0));

        copySection(old, "location", out, "location");
        copySection(old, "skin", out, "skin");
        copySection(old, "parts", out, "parts");
        out.set("hologram.lines", old.contains("hologram")
                ? old.getStringList("hologram")
                : java.util.List.of("<yellow>CocoNPC"));
        out.set("hologram.billboard", old.getString("hologram_billboard", "CENTER"));
        out.set("hologram.shadow", old.getBoolean("hologram_shadow", true));
        out.set("hologram.background", old.getString("hologram_bg", "transparent"));

        if (old.contains("right_hand")) out.set("hands.right", old.getItemStack("right_hand"));
        if (old.contains("left_hand")) out.set("hands.left", old.getItemStack("left_hand"));
        out.set("actions", actions.getStringList("npc." + id + ".actions"));

        try {
            out.save(target);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not write npcs/" + id + ".yml during migration: "
                    + ex.getMessage());
            return false;
        }
    }

    private void copySection(ConfigurationSection from, String fromPath,
                             YamlConfiguration to, String toPath) {
        ConfigurationSection section = from.getConfigurationSection(fromPath);
        if (section == null) return;
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) continue;
            to.set(toPath + "." + key, section.get(key));
        }
    }

    private void backup(File oldData, File oldActions) {
        renameToBackup(oldData);
        if (oldActions.exists()) {
            renameToBackup(oldActions);
        }
        plugin.getLogger().info("Old storage kept as .bak — delete it once you are happy "
                + "the migration worked.");
    }

    private void renameToBackup(File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        if (backup.exists() && !backup.delete()) {
            plugin.getLogger().warning("Could not replace the existing " + backup.getName());
        }
        if (!file.renameTo(backup)) {
            plugin.getLogger().warning("Could not rename " + file.getName()
                    + " — it will be ignored on the next start, but you may want to remove it.");
        }
    }
}


