package com.pumpkings.coconpc.core.config;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {

    private final CocoNPC plugin;
    public PluginConfig config;
    public PluginConfig skins;
    public PluginConfig messages;
    public PluginConfig npcEditorMenu;
    public PluginConfig hologramEditorMenu;

    private NpcDataRepository repository;

    public ConfigManager(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public void loadMainConfig() {
        config = new PluginConfig("config.yml", plugin);
        int added = config.mergeDefaults("config.yml");
        if (added > 0) {
            plugin.getLogger().info("Added " + added + " new setting(s) to config.yml.");
        }
    }

    public void loadSkins() {
        skins = new PluginConfig("skins.yml", plugin);
    }

    public void loadNpc() {
    }

    public void loadNpcData() {
        messages = new PluginConfig("messages.yml", plugin);
        boolean dirty = false;
        for (Message msg : Message.values()) {
            if (!messages.contains(msg.name())) {
                messages.set(msg.name(), msg.getDefault());
                dirty = true;
            }
        }
        if (dirty) {
            messages.saveConfig();
        }

        npcEditorMenu = new PluginConfig("menus/npc_editor.yml", plugin);
        hologramEditorMenu = new PluginConfig("menus/hologram_editor.yml", plugin);
        if (repository == null) {
            repository = new NpcDataRepository(plugin);
        } else {
            repository.invalidateCache();
        }
        Map<UUID, NpcData> dataMap = repository.loadAll();
        dataMap.forEach((uuid, data) -> {
            plugin.getRegistry().getNpcsId().put(uuid, data.id());
            if (plugin.getRegistry().getNpcs().containsKey(uuid)) {
                plugin.getRegistry().getNpcs().get(uuid).setId(data.id());
            }
        });
    }

    public com.pumpkings.coconpc.core.config.NpcData spawn(NpcEntity npc, String customId) {
        NpcData saved = repository.save(npc, customId);
        plugin.getRegistry().getNpcsId().put(npc.getUUID(), saved.id());
        return saved;
    }

    public List<String> getActions(NpcEntity npc) {
        return repository.getActions(npc);
    }

    public void saveActions(NpcEntity npc, List<String> actions) {
        repository.saveActions(npc, actions);
    }

    public void saveSize(NpcEntity npc, float size) {
        repository.saveSize(npc, size);
    }

    public void delete(NpcEntity npc) {
        repository.delete(npc);
    }

    public void saveHandItems(NpcEntity npc, org.bukkit.inventory.ItemStack rightHand, org.bukkit.inventory.ItemStack leftHand) {
        repository.saveHandItems(npc, rightHand, leftHand);
    }

    public org.bukkit.inventory.ItemStack getRightHandItem(NpcEntity npc) {
        return repository.getRightHandItem(npc);
    }

    public org.bukkit.inventory.ItemStack getLeftHandItem(NpcEntity npc) {
        return repository.getLeftHandItem(npc);
    }

    public NpcDataRepository getRepository() {
        return repository;
    }

    public String getMineskinUserAgent() {
        String value = config == null ? null : config.getString("mineskin.user-agent", "CocoNPC");
        return value == null || value.isBlank() ? "CocoNPC" : value;
    }

    public String getMineskinApiKey() {
        String value = config == null ? null : config.getString("mineskin.api-key", "");
        return value == null || value.isBlank() ? null : value;
    }

    public void setMineskinApiKey(String apiKey) {
        if (config == null) return;
        config.set("mineskin.api-key", apiKey == null ? "" : apiKey);
        config.saveConfig();
    }

    public int getViewDistance() {
        int value = config == null ? 48 : config.getInt("npc.view-distance", 48);
        return Math.max(8, value);
    }

    public void setViewDistance(int blocks) {
        if (config == null) return;
        config.set("npc.view-distance", Math.max(8, blocks));
        config.saveConfig();
    }

    public boolean isBedrockBlocked() {
        return config == null || config.getBoolean("bedrock.block-interactions", true);
    }

    public boolean isBedrockNotified() {
        return config == null || config.getBoolean("bedrock.notify-blocked", true);
    }

    public boolean isRequireMoneyStrict() {
        return config == null || config.getBoolean("actions.require-money-strict", true);
    }

    public int getTitleDurationSeconds() {
        return config == null ? 3 : config.getInt("actions.title-duration-seconds", 3);
    }

    public float getEditorRotationStep(boolean precise) {
        double fallback = precise ? 1.0 : 5.0;
        double value = config == null ? fallback : config.getDouble(
                precise ? "editor.precision-rotation-step" : "editor.rotation-step", fallback);
        return (float) Math.max(0.1, Math.min(45.0, value));
    }

    public float getEditorTranslationStep(boolean precise) {
        double fallback = precise ? 0.01 : 0.05;
        double value = config == null ? fallback : config.getDouble(
                precise ? "editor.precision-translation-step" : "editor.translation-step", fallback);
        return (float) Math.max(0.001, Math.min(1.0, value));
    }

    public List<String> getConsoleBlockedCharacters() {
        if (config == null) return List.of(";", "|", "&");
        return config.getStringList("actions.console-blocked-characters",
                List.of(";", "|", "&"));
    }
}


