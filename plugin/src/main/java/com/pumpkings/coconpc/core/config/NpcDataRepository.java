package com.pumpkings.coconpc.core.config;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.BodyParts;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.npc.NpcSkin;
import com.pumpkings.coconpc.core.npc.part.ItemPart;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NpcDataRepository {

    private static final String FOLDER = "npcs";
    private static final String UUID_KEY = "uuid";
    private static final String ID_KEY = "id";
    private static final String SIZE_KEY = "size";
    private static final String LOCATION = "location";
    private static final String SKIN = "skin";
    private static final String PARTS = "parts";
    private static final String HOLOGRAM = "hologram";
    private static final String HOLO_LINES = HOLOGRAM + ".lines";
    private static final String HOLO_BILLBOARD = HOLOGRAM + ".billboard";
    private static final String HOLO_SHADOW = HOLOGRAM + ".shadow";
    private static final String HOLO_BACKGROUND = HOLOGRAM + ".background";
    private static final String HAND_RIGHT = "hands.right";
    private static final String HAND_LEFT = "hands.left";
    private static final String ACTIONS = "actions";

    static final java.util.List<String> PART_NAMES = BodyParts.ALL;

    private static final List<String> DEFAULT_HOLOGRAM = List.of("<yellow>CocoNPC");

    private final CocoNPC plugin;
    private final File folder;

    private final Map<String, YamlConfiguration> cache = new HashMap<>();
    private final Map<UUID, String> uuidToId = new HashMap<>();

    public NpcDataRepository(CocoNPC plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), FOLDER);
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().severe("Could not create the npcs folder — NPCs will not persist.");
        }
        new NpcDataMigration(plugin, folder).runIfNeeded();
        indexExistingFiles();
    }

    private File fileFor(String id) {
        return new File(folder, id + ".yml");
    }

    private List<String> storedIds() {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();

        List<String> ids = new ArrayList<>();
        for (File file : files) {
            String base = file.getName().substring(0, file.getName().length() - 4);
            ids.add(base);
        }
        Collections.sort(ids);
        return ids;
    }

    private YamlConfiguration configFor(String id) {
        return cache.computeIfAbsent(id, key -> YamlConfiguration.loadConfiguration(fileFor(key)));
    }

    private void write(String id) {
        YamlConfiguration config = cache.get(id);
        if (config == null) return;
        try {
            config.save(fileFor(id));
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save npcs/" + id + ".yml: " + ex.getMessage());
        }
    }

    private String idOf(NpcEntity npc) {
        if (npc.getId() != null && !npc.getId().isEmpty()) return npc.getId();
        return uuidToId.get(npc.getUUID());
    }

    private void indexExistingFiles() {
        uuidToId.clear();
        for (String id : storedIds()) {
            String uuidStr = configFor(id).getString(UUID_KEY);
            if (uuidStr == null) continue;
            try {
                uuidToId.put(UUID.fromString(uuidStr), id);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("npcs/" + id + ".yml has an invalid uuid; skipping.");
            }
        }
    }

    public Map<UUID, NpcData> loadAll() {
        Map<UUID, NpcData> result = new LinkedHashMap<>();

        for (String id : storedIds()) {
            YamlConfiguration config = configFor(id);
            String uuidStr = config.getString(UUID_KEY);
            if (uuidStr == null) {
                plugin.getLogger().warning("npcs/" + id + ".yml has no uuid; skipping.");
                continue;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("npcs/" + id + ".yml has an invalid uuid; skipping.");
                continue;
            }

            result.put(uuid, new NpcData(
                    uuid,
                    config.getString(ID_KEY, id),
                    config.getStringList(ACTIONS),
                    (float) config.getDouble(SIZE_KEY, 1.0),
                    config.contains(HOLO_LINES) ? config.getStringList(HOLO_LINES) : DEFAULT_HOLOGRAM,
                    config.getString(HOLO_BILLBOARD, "CENTER"),
                    config.getBoolean(HOLO_SHADOW, true),
                    config.getString(HOLO_BACKGROUND, "transparent"),
                    config.getItemStack(HAND_RIGHT, new ItemStack(Material.AIR)),
                    config.getItemStack(HAND_LEFT, new ItemStack(Material.AIR))
            ));
            uuidToId.put(uuid, id);
        }
        return result;
    }

    public Location loadLocation(UUID uuid) {
        String id = uuidToId.get(uuid);
        if (id == null) return null;

        YamlConfiguration config = configFor(id);
        String worldName = config.getString(LOCATION + ".world");
        if (worldName == null) return null;

        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("NPC #" + id + " refers to a world that is not loaded: " + worldName);
            return null;
        }

        return new Location(world,
                config.getDouble(LOCATION + ".x"),
                config.getDouble(LOCATION + ".y"),
                config.getDouble(LOCATION + ".z"),
                (float) config.getDouble(LOCATION + ".yaw"),
                (float) config.getDouble(LOCATION + ".pitch"));
    }

    public NpcSkin loadSkin(UUID uuid) {
        String id = uuidToId.get(uuid);
        if (id == null) return null;

        YamlConfiguration config = configFor(id);
        if (!config.contains(SKIN + ".head")) return null;

        NpcSkin skin = new NpcSkin(plugin, uuid.toString());
        for (String partName : BodyParts.SKIN_PARTS) {
            skin.set(partName, config.getString(SKIN + "." + partName));
        }
        return skin;
    }

    public List<String> getActions(NpcEntity npc) {
        String id = idOf(npc);
        return id == null ? List.of() : configFor(id).getStringList(ACTIONS);
    }

    public List<String> getHologramLines(NpcEntity npc) {
        String id = idOf(npc);
        if (id == null) return DEFAULT_HOLOGRAM;
        YamlConfiguration config = configFor(id);
        return config.contains(HOLO_LINES) ? config.getStringList(HOLO_LINES) : DEFAULT_HOLOGRAM;
    }

    public String getHologramBillboard(NpcEntity npc) {
        String id = idOf(npc);
        return id == null ? "CENTER" : configFor(id).getString(HOLO_BILLBOARD, "CENTER");
    }

    public boolean getHologramShadowed(NpcEntity npc) {
        String id = idOf(npc);
        return id == null || configFor(id).getBoolean(HOLO_SHADOW, true);
    }

    public String getHologramBackground(NpcEntity npc) {
        String id = idOf(npc);
        return id == null ? "transparent" : configFor(id).getString(HOLO_BACKGROUND, "transparent");
    }

    public ItemStack getRightHandItem(NpcEntity npc) {
        String id = idOf(npc);
        return id == null ? new ItemStack(Material.AIR)
                : configFor(id).getItemStack(HAND_RIGHT, new ItemStack(Material.AIR));
    }

    public ItemStack getLeftHandItem(NpcEntity npc) {
        String id = idOf(npc);
        return id == null ? new ItemStack(Material.AIR)
                : configFor(id).getItemStack(HAND_LEFT, new ItemStack(Material.AIR));
    }

    public NpcData save(NpcEntity npc, String customId) {
        String newId = customId != null && !customId.isEmpty() ? customId : nextFreeId();

        npc.setId(newId);
        uuidToId.put(npc.getUUID(), newId);

        YamlConfiguration config = configFor(newId);
        config.set(UUID_KEY, npc.getUUID().toString());
        config.set(ID_KEY, newId);
        config.set(SIZE_KEY, npc.getSize());
        config.set(HOLO_LINES, DEFAULT_HOLOGRAM);
        config.set(HOLO_BILLBOARD, "CENTER");
        config.set(HOLO_SHADOW, true);
        config.set(HOLO_BACKGROUND, "transparent");
        config.set(ACTIONS, List.of());
        writeLocationAndSkin(config, npc);
        write(newId);

        return new NpcData(npc.getUUID(), newId, List.of(), npc.getSize(),
                DEFAULT_HOLOGRAM, "CENTER", true, "transparent");
    }

    private String nextFreeId() {
        List<String> ids = storedIds();
        int max = 0;
        for (String id : ids) {
            try {
                int numericId = Integer.parseInt(id);
                if (numericId > max) max = numericId;
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf(max + 1);
    }

    public void saveLocationAndSkin(NpcEntity npc) {
        String id = idOf(npc);
        if (id == null) return;
        YamlConfiguration config = configFor(id);
        writeLocationAndSkin(config, npc);
        write(id);
    }

    private void writeLocationAndSkin(YamlConfiguration config, NpcEntity npc) {
        Location loc = npc.getLocation();
        if (loc != null && loc.getWorld() != null) {
            config.set(LOCATION + ".world", loc.getWorld().getName());
            config.set(LOCATION + ".x", loc.getX());
            config.set(LOCATION + ".y", loc.getY());
            config.set(LOCATION + ".z", loc.getZ());
            config.set(LOCATION + ".yaw", loc.getYaw());
            config.set(LOCATION + ".pitch", loc.getPitch());
        }

        NpcSkin skin = npc.getSkin();
        if (skin != null) {
            for (String partName : BodyParts.SKIN_PARTS) {
                config.set(SKIN + "." + partName, skin.get(partName));
            }
        }
    }

    public void saveActions(NpcEntity npc, List<String> actions) {
        String id = idOf(npc);
        if (id == null) return;
        configFor(id).set(ACTIONS, actions);
        write(id);
    }

    public void saveSize(NpcEntity npc, float size) {
        String id = idOf(npc);
        if (id == null) return;
        configFor(id).set(SIZE_KEY, size);
        write(id);
    }

    public void saveHologram(NpcEntity npc, List<String> lines) {
        String id = idOf(npc);
        if (id == null) return;
        configFor(id).set(HOLO_LINES, lines);
        write(id);
    }

    public void saveHologramStyle(NpcEntity npc, String billboard, boolean shadowed, String background) {
        String id = idOf(npc);
        if (id == null) return;
        YamlConfiguration config = configFor(id);
        if (billboard != null) config.set(HOLO_BILLBOARD, billboard);
        config.set(HOLO_SHADOW, shadowed);
        if (background != null) config.set(HOLO_BACKGROUND, background);
        write(id);
    }

    public void saveHandItems(NpcEntity npc, ItemStack rightHand, ItemStack leftHand) {
        String id = idOf(npc);
        if (id == null) return;
        YamlConfiguration config = configFor(id);
        config.set(HAND_RIGHT, rightHand);
        config.set(HAND_LEFT, leftHand);
        write(id);
    }

    public void saveParts(NpcEntity npc) {
        String id = idOf(npc);
        if (id == null) return;

        YamlConfiguration config = configFor(id);
        for (String name : PART_NAMES) {
            ItemPart part = npc.getPart(name);
            if (part == null) continue;

            String base = PARTS + "." + name;
            config.set(base + ".pitch", part.getPitch());
            config.set(base + ".yaw", part.getYaw());
            config.set(base + ".roll", part.getRoll());
            config.set(base + ".offset_x", part.getCustomOffset().x);
            config.set(base + ".offset_y", part.getCustomOffset().y);
            config.set(base + ".offset_z", part.getCustomOffset().z);
            config.set(base + ".hidden", part.isHidden());
        }
        write(id);
    }

    public void loadParts(NpcEntity npc) {
        String id = idOf(npc);
        if (id == null) return;

        YamlConfiguration config = configFor(id);
        ConfigurationSection parts = config.getConfigurationSection(PARTS);
        if (parts == null) return;

        for (String name : PART_NAMES) {
            ItemPart part = npc.getPart(name);
            if (part == null || !parts.contains(name)) continue;

            ConfigurationSection section = parts.getConfigurationSection(name);
            if (section == null) continue;

            part.setRotation(
                    (float) section.getDouble("pitch", 0.0),
                    (float) section.getDouble("yaw", 0.0),
                    (float) section.getDouble("roll", 0.0));
            part.setCustomOffset(
                    (float) section.getDouble("offset_x", 0.0),
                    (float) section.getDouble("offset_y", 0.0),
                    (float) section.getDouble("offset_z", 0.0));
            npc.setPartHidden(name, section.getBoolean("hidden", false));
        }
        npc.updateTransforms();
    }

    public void delete(NpcEntity npc) {
        String id = idOf(npc);
        if (id == null) return;

        cache.remove(id);
        uuidToId.remove(npc.getUUID());

        File file = fileFor(id);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete npcs/" + id + ".yml");
        }
    }

    public void invalidateCache() {
        cache.clear();
        indexExistingFiles();
    }
}


