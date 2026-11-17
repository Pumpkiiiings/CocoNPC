package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.mob.model.BbModel;
import com.pumpkings.coconpc.core.mob.model.BbModelParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Scans the {@code plugins/CocoNPC/mobs} folder for {@code .bbmodel} files and
 * builds a {@link MobDefinition} for each one. Settings live in
 * {@code plugins/CocoNPC/mobs.yml}; new models get a documented default block
 * automatically.
 */
public final class MobDefinitionLoader {
    private static final String DEFAULT_DRIVER = "ARMOR_STAND";

    private final CocoNPC plugin;
    private final File folder;
    private final File configFile;

    public MobDefinitionLoader(CocoNPC plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "mobs");
        this.configFile = new File(plugin.getDataFolder(), "mobs.yml");
    }

    public File getFolder() {
        return folder;
    }

    public File getConfigFile() {
        return configFile;
    }

    public void prepare() {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            plugin.getLogger().warning("[CocoNPC] Could not create the mobs folder: " + folder);
        }
        if (!configFile.isFile()) {
            try {
                plugin.saveResource("mobs.yml", false);
            } catch (IllegalArgumentException exception) {
                try {
                    configFile.getParentFile().mkdirs();
                    Files.writeString(configFile.toPath(), "mobs: {}\n");
                } catch (IOException ioException) {
                    plugin.getLogger().log(Level.WARNING,
                            "[CocoNPC] Could not create mobs.yml", ioException);
                }
            }
        }
    }

    public List<MobDefinition> load() {
        prepare();

        List<MobDefinition> definitions = new ArrayList<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        applyHeader(config);

        List<Path> models = findModels();
        boolean dirty = false;
        java.util.Set<String> usedIds = new java.util.HashSet<>();

        for (Path path : models) {
            String fileName = relativeName(path);
            String id = sanitizeId(stripExtension(path.getFileName().toString()));

            if (!usedIds.add(id)) {
                plugin.getLogger().warning("[CocoNPC] Duplicate mob id '" + id
                        + "' for " + fileName + "; rename one of the .bbmodel files.");
                continue;
            }

            ConfigurationSection section = config.getConfigurationSection("mobs." + id);
            if (section == null) {
                section = config.createSection("mobs." + id);
                writeDefaults(section, fileName);
                dirty = true;
            } else if (!section.isSet("model")) {
                section.set("model", fileName);
                dirty = true;
            }

            MobDefinition definition = build(id, path, section);
            if (definition != null) {
                definitions.add(definition);
            }
        }

        if (dirty) {
            try {
                config.save(configFile);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "[CocoNPC] Could not save mobs.yml", exception);
            }
        }

        plugin.getLogger().info("[CocoNPC] Loaded " + definitions.size()
                + " custom mob definition(s) from " + folder.getName() + "/.");
        return definitions;
    }

    /**
     * Persists the action list of a definition in {@code mobs.yml}.
     *
     * @return {@code true} when the file was written successfully
     */
    public boolean saveActions(String definitionId, List<String> actions) {
        List<String> copy = new ArrayList<>();
        if (actions != null) {
            for (String action : actions) {
                if (action != null && !action.isBlank()) {
                    copy.add(action.trim());
                }
            }
        }
        return saveValue(definitionId, "actions", copy);
    }

    /**
     * Persists the scale of a definition in {@code mobs.yml}.
     *
     * @return {@code true} when the file was written successfully
     */
    public boolean saveScale(String definitionId, double scale) {
        return saveValue(definitionId, "scale",
                Math.round(scale * 1000.0) / 1000.0);
    }

    private synchronized boolean saveValue(String definitionId, String key, Object value) {
        if (definitionId == null || definitionId.isBlank()) return false;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            applyHeader(config);
            config.set("mobs." + definitionId + "." + key, value);
            config.save(configFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "[CocoNPC] Could not save " + key + " for mob '"
                            + definitionId + "' in mobs.yml", exception);
            return false;
        }
    }

    private static void applyHeader(YamlConfiguration config) {
        config.options().setHeader(List.of(
                "CocoNPC custom .bbmodel mobs.",
                "Drop .bbmodel files in plugins/CocoNPC/mobs and run /coconpc mob reload.",
                "Every file gets a section below the first time it is found.",
                "Optional per-mob settings:",
                "  actions: list of NPC-style actions run on right-click.",
                "  cooldown: per-player interaction cooldown in seconds.",
                "  animation: animation played once when the mob spawns."));
    }

    private List<Path> findModels() {
        if (!folder.isDirectory()) return List.of();

        try (var stream = Files.walk(folder.toPath(), 4)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)
                            .endsWith(".bbmodel"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "[CocoNPC] Could not list the mobs folder", exception);
            return List.of();
        }
    }

    private MobDefinition build(String id, Path path, ConfigurationSection section) {
        try {
            BbModel model = BbModelParser.parse(path, plugin.getLogger());
            if (model == null) {
                plugin.getLogger().warning("[CocoNPC] Skipping invalid model: " + path.getFileName());
                return null;
            }

            EntityType driverType = readDriver(section.getString("driver", DEFAULT_DRIVER));
            boolean armorStand = driverType == EntityType.ARMOR_STAND;
            boolean staticMode = section.isSet("static")
                    ? section.getBoolean("static")
                    : armorStand;

            boolean ai = section.isSet("ai")
                    ? section.getBoolean("ai")
                    : !staticMode;
            boolean gravity = section.isSet("gravity")
                    ? section.getBoolean("gravity")
                    : !staticMode;
            boolean collidable = section.isSet("collidable")
                    ? section.getBoolean("collidable")
                    : !staticMode;
            boolean invulnerable = section.isSet("invulnerable")
                    ? section.getBoolean("invulnerable")
                    : staticMode;
            boolean marker = section.isSet("marker")
                    ? section.getBoolean("marker")
                    : armorStand;

            MobTextureMode textureMode = MobTextureMode.parse(
                    section.getString("texture-mode"), MobTextureMode.SKINS);

            double scale = section.getDouble("scale", 1.0);
            float yawOffset = (float) section.getDouble("yaw-offset", 180.0);
            float viewRange = (float) section.getDouble("view-range", 2.0);
            int interpolation = section.getInt("interpolation", 1);
            int interpolationDelay = section.getInt("interpolation-delay", 0);
            int teleportDuration = section.getInt("teleport-duration", 1);
            double reanchor = section.getDouble("reanchor-distance", 4.0);
            double maxHealth = section.getDouble("max-health", -1.0);

            boolean hitbox = section.getBoolean("hitbox.enabled", true);
            float hitboxWidth = (float) section.getDouble("hitbox.width", 0.9);
            float hitboxHeight = (float) section.getDouble("hitbox.height", 1.8);

            MobAnimationSet animationSet = readAnimations(section);

            List<String> actions = section.getStringList("actions");
            double interactionCooldown = section.getDouble("cooldown", 0.0);
            String defaultAnimation = section.getString("animation", null);

            MobDefinition.Builder builder = MobDefinition.builder(id, driverType, model)
                    .scale(scale)
                    .yawOffset(yawOffset)
                    .interaction(hitbox, hitboxWidth, hitboxHeight)
                    .ai(ai)
                    .gravity(gravity)
                    .collidable(collidable)
                    .invulnerable(invulnerable)
                    .marker(marker)
                    .textureMode(textureMode)
                    .viewRange(viewRange)
                    .interpolation(interpolation, interpolationDelay)
                    .teleportDuration(teleportDuration)
                    .reanchorDistance(reanchor)
                    .animationSet(animationSet)
                    .actions(actions)
                    .interactionCooldown(interactionCooldown)
                    .defaultAnimation(defaultAnimation)
                    .modelPath(path.toAbsolutePath().normalize());

            if (maxHealth > 0.0) {
                builder.maxHealth(maxHealth);
            }

            return builder.build();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING,
                    "[CocoNPC] Could not build mob definition '" + id + "'", exception);
            return null;
        }
    }

    private MobAnimationSet readAnimations(ConfigurationSection section) {        MobAnimationSet.Builder builder = MobAnimationSet.builder();
        builder.idle(section.getString("animations.idle", "idle"));
        builder.walk(section.getString("animations.walk", "walk"));
        builder.attack(section.getString("animations.attack", "attack"));
        builder.hurt(section.getString("animations.hurt", "hurt"));
        builder.death(section.getString("animations.death", "death"));
        builder.idleThreshold((float) section.getDouble("animations.idle-threshold", 0.02));
        builder.walkReferenceSpeed((float) section.getDouble("animations.walk-reference-speed", 0.2));
        builder.walkPlaybackRange(
                (float) section.getDouble("animations.walk-playback-min", 0.35),
                (float) section.getDouble("animations.walk-playback-max", 2.5));
        builder.hurtFlashTicks(section.getInt("animations.hurt-flash-ticks", 6));
        builder.additiveRotation(section.getBoolean("animations.additive-rotation", false));
        builder.attackOnAggro(section.getBoolean("animations.attack-on-aggro", true));
        return builder.build();
    }

    private EntityType readDriver(String raw) {
        if (raw == null || raw.isBlank()) return EntityType.ARMOR_STAND;

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            EntityType type = EntityType.valueOf(normalized);
            if (!type.isSpawnable() || !type.isAlive() || type.getEntityClass() == null) {
                plugin.getLogger().warning("[CocoNPC] Driver '" + raw
                        + "' is not a living entity; using ARMOR_STAND.");
                return EntityType.ARMOR_STAND;
            }
            return type;
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[CocoNPC] Unknown driver '" + raw
                    + "'; using ARMOR_STAND.");
            return EntityType.ARMOR_STAND;
        }
    }

    private void writeDefaults(ConfigurationSection section, String fileName) {
        section.set("model", fileName);
        section.set("driver", DEFAULT_DRIVER);
        // Static / ai / gravity / collidable / invulnerable / marker are not
        // written on purpose: their defaults follow the selected driver, so
        // changing only "driver" is enough to enable AI for living drivers.
        section.set("texture-mode", "skins");
        section.set("scale", 1.0);
        section.set("yaw-offset", 180.0);
        section.set("view-range", 2.0);
        section.set("interpolation", 1);
        section.set("interpolation-delay", 0);
        section.set("teleport-duration", 1);
        section.set("reanchor-distance", 4.0);
        section.set("hitbox.enabled", true);
        section.set("hitbox.width", 0.9);
        section.set("hitbox.height", 1.8);
        section.set("animations.idle", "idle");
        section.set("animations.walk", "walk");
        section.set("animations.attack", "attack");
        section.set("animations.hurt", "hurt");
        section.set("animations.death", "death");
        section.set("animations.idle-threshold", 0.02);
        section.set("animations.walk-reference-speed", 0.2);
        section.set("animations.walk-playback-min", 0.35);
        section.set("animations.walk-playback-max", 2.5);
        section.set("animations.hurt-flash-ticks", 6);
        section.set("animations.additive-rotation", false);
        section.set("animations.attack-on-aggro", true);
        section.set("actions", new ArrayList<String>());
        section.set("cooldown", 0.0);
    }

    private String relativeName(Path path) {
        Path root = folder.toPath().toAbsolutePath().normalize();
        Path absolute = path.toAbsolutePath().normalize();
        try {
            return root.relativize(absolute).toString().replace('\\', '/');
        } catch (IllegalArgumentException exception) {
            return path.getFileName().toString();
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String sanitizeId(String raw) {
        String id = raw.toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9_\\-]", "_")
                .replaceAll("_+", "_");
        if (id.isBlank()) id = "mob";
        return id;
    }
}
