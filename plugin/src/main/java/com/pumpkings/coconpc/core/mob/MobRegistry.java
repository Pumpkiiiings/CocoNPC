package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class MobRegistry {
    private final CocoNPC plugin;
    private final MobKeys keys;
    private final MobTicker ticker;

    private final Map<String, MobDefinition> definitions = new HashMap<>();
    private final Map<UUID, CustomMob> mobsByDriver = new HashMap<>();
    private final Map<UUID, CustomMob> mobsByInstance = new HashMap<>();
    private final List<CustomMob> activeMobs = new ArrayList<>();

    public MobRegistry(CocoNPC plugin) {
        this.plugin = plugin;
        this.keys = new MobKeys(plugin);
        this.ticker = new MobTicker(plugin, this);
    }

    public void registerDefinition(MobDefinition definition) {
        if (definition == null) return;
        definitions.put(definition.getId(), definition);
        definition.validate(plugin.getLogger());
        plugin.getLogger().info("[CocoNPC] Registered custom mob definition '"
                + definition.getId() + "'.");
    }

    /**
     * Replaces every loaded definition and applies the change to the mobs that
     * are already spawned. Driver type changes migrate the live entity; model
     * or flag changes update it in place.
     */
    public void replaceDefinitions(List<MobDefinition> loaded) {
        Map<String, MobDefinition> next = new HashMap<>();
        if (loaded != null) {
            for (MobDefinition definition : loaded) {
                if (definition == null) continue;
                next.put(definition.getId(), definition);
                definition.validate(plugin.getLogger());
            }
        }

        definitions.clear();
        definitions.putAll(next);

        for (CustomMob mob : new ArrayList<>(activeMobs)) {
            MobDefinition updated = getDefinition(mob.getDefinition().getId());
            if (updated == null) {
                runForMob(mob, () -> mob.cleanup(true));
            } else {
                runForMob(mob, () -> mob.updateDefinition(updated));
            }
        }
    }

    private void runForMob(CustomMob mob, Runnable action) {
        LivingEntity driver = mob.getDriver();
        if (driver == null || !driver.isValid()) {
            action.run();
            return;
        }

        try {
            Bukkit.getRegionScheduler().run(plugin, driver.getLocation(), task -> {
                if (!mob.isCleaned()) {
                    action.run();
                }
            });
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.FINE,
                    "[CocoNPC] Could not schedule mob update; running inline.", exception);
            action.run();
        }
    }

    public int removeMobs(String definitionId) {
        MobDefinition definition = definitionId != null ? getDefinition(definitionId) : null;
        if (definitionId != null && definition == null) return 0;

        int removed = 0;
        for (CustomMob mob : new ArrayList<>(activeMobs)) {
            if (definition != null
                    && !mob.getDefinition().getId().equals(definition.getId())) {
                continue;
            }
            removed++;
            runForMob(mob, () -> mob.cleanup(true));
        }
        return removed;
    }

    public int countMobs(String definitionId) {
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) return 0;

        int count = 0;
        for (CustomMob mob : activeMobs) {
            if (mob.getDefinition().getId().equals(definition.getId())) {
                count++;
            }
        }
        return count;
    }

    public MobDefinition getDefinition(String id) {
        if (id == null) return null;

        MobDefinition exact = definitions.get(id);
        if (exact != null) return exact;

        for (Map.Entry<String, MobDefinition> entry : definitions.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(id)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<MobDefinition> getDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    /**
     * Rebuilds the displays of every live mob of the given definition. Called
     * by the texture service once cube skins or block approximations are ready.
     */
    public void refreshVisuals(String definitionId) {
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) return;

        for (CustomMob mob : new ArrayList<>(activeMobs)) {
            if (!mob.getDefinition().getId().equals(definition.getId())) continue;
            runForMob(mob, () -> mob.refreshVisuals(definition));
        }
    }

    /**
     * Persists the action list in {@code mobs.yml}, updates the live definition
     * and makes every active mob of that type pick it up. Displays are not
     * rebuilt, so textures are preserved.
     */
    public boolean saveActions(String definitionId, List<String> actions) {
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) return false;

        List<String> normalized = new ArrayList<>();
        if (actions != null) {
            for (String action : actions) {
                if (action != null && !action.isBlank()) {
                    normalized.add(action.trim());
                }
            }
        }

        if (plugin.getMobDefinitionLoader() == null
                || !plugin.getMobDefinitionLoader().saveActions(definition.getId(), normalized)) {
            return false;
        }

        definition.setActions(normalized);
        applyLiveDefinition(definition.getId());
        return true;
    }

    /**
     * Persists a new scale in {@code mobs.yml} and applies it to the live
     * definition. Rigs read the scale every tick, so no visuals are rebuilt.
     */
    public boolean saveScale(String definitionId, double scale) {
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) return false;

        double clamped = Math.max(0.1, Math.min(10.0,
                Math.round(scale * 100.0) / 100.0));

        if (plugin.getMobDefinitionLoader() == null
                || !plugin.getMobDefinitionLoader().saveScale(definition.getId(), clamped)) {
            return false;
        }

        definition.setScale(clamped);
        applyLiveDefinition(definition.getId());
        return true;
    }

    /**
     * Re-points every active mob of a definition at the registry's live
     * instance so mutable values (actions, scale) are visible immediately.
     */
    public void applyLiveDefinition(String definitionId) {
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) return;

        for (CustomMob mob : new ArrayList<>(activeMobs)) {
            if (!mob.getDefinition().getId().equals(definition.getId())) continue;
            runForMob(mob, () -> mob.syncDefinition(definition));
        }
    }

    /** Removes a single live mob instance on its region thread. */
    public void removeMob(CustomMob mob) {
        if (mob == null || !activeMobs.contains(mob)) return;
        runForMob(mob, () -> mob.cleanup(true));
    }

    public CustomMob spawn(String definitionId, Location location) {
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) {
            plugin.getLogger().warning("[CocoNPC] Unknown custom mob definition: " + definitionId);
            return null;
        }
        return spawn(definition, location);
    }

    public CustomMob spawn(MobDefinition definition, Location location) {
        if (definition == null || location == null || location.getWorld() == null) {
            return null;
        }
        if (!definition.getDriverType().isSpawnable()
                || !definition.getDriverType().isAlive()
                || definition.getDriverType().getEntityClass() == null) {
            throw new IllegalArgumentException("Unsupported driver type: "
                    + definition.getDriverType());
        }

        Entity spawned = location.getWorld().spawnEntity(location,
                definition.getDriverType(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            throw new IllegalArgumentException("Driver type is not a living entity: "
                    + definition.getDriverType());
        }

        CustomMob mob = new CustomMob(plugin, definition, living, UUID.randomUUID(), keys);
        try {
            mob.attach();
        } catch (RuntimeException exception) {
            living.remove();
            throw exception;
        }

        register(mob);
        ticker.attach(mob);
        return mob;
    }

    private void register(CustomMob mob) {
        if (mob == null || mob.getDriver() == null) return;
        mobsByDriver.put(mob.getDriver().getUniqueId(), mob);
        mobsByInstance.put(mob.getInstanceId(), mob);
        activeMobs.add(mob);
    }

    public void unregister(CustomMob mob) {
        if (mob == null) return;
        if (mob.getDriver() != null) {
            mobsByDriver.remove(mob.getDriver().getUniqueId());
        }
        mobsByInstance.remove(mob.getInstanceId());
        activeMobs.remove(mob);
        ticker.detach(mob);
    }

    void updateDriver(CustomMob mob, UUID oldDriverId, LivingEntity newDriver) {
        if (mob == null) return;
        if (oldDriverId != null) {
            mobsByDriver.remove(oldDriverId);
        }
        if (newDriver != null) {
            mobsByDriver.put(newDriver.getUniqueId(), mob);
            ticker.attach(mob);
        }
    }

    public CustomMob getByDriver(UUID driverId) {
        if (driverId == null) return null;

        CustomMob mob = mobsByDriver.get(driverId);
        if (mob != null) return mob;

        for (CustomMob candidate : activeMobs) {
            LivingEntity driver = candidate.getDriver();
            if (driver != null && driver.getUniqueId().equals(driverId)) {
                return candidate;
            }
        }
        return null;
    }

    public CustomMob getByInstance(UUID instanceId) {
        return instanceId != null ? mobsByInstance.get(instanceId) : null;
    }

    public CustomMob getByInteraction(Entity entity) {
        if (entity == null) return null;
        for (CustomMob mob : activeMobs) {
            if (mob.getInteraction() == entity) {
                return mob;
            }
        }
        return null;
    }

    public List<CustomMob> getMobs() {
        return Collections.unmodifiableList(activeMobs);
    }

    /**
     * Returns the closest attached custom mob to the given location, or
     * {@code null} when none of them live in that world.
     */
    public CustomMob getNearest(Location location) {
        if (location == null || location.getWorld() == null) return null;

        CustomMob nearest = null;
        double bestDistance = Double.MAX_VALUE;

        for (CustomMob mob : activeMobs) {
            if (mob == null || !mob.isAttached() || mob.isCleaned()) continue;

            LivingEntity driver = mob.getDriver();
            if (driver == null || !driver.isValid()) continue;

            Location driverLocation = driver.getLocation();
            if (driverLocation == null || driverLocation.getWorld() != location.getWorld()) {
                continue;
            }

            double distance = driverLocation.distanceSquared(location);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = mob;
            }
        }
        return nearest;
    }

    public MobTicker getTicker() {
        return ticker;
    }

    public void tickAll() {
        for (int index = activeMobs.size() - 1; index >= 0; index--) {
            CustomMob mob = activeMobs.get(index);
            mob.tick();
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        if (chunk == null) return;

        for (CustomMob mob : new ArrayList<>(activeMobs)) {
            LivingEntity driver = mob.getDriver();
            if (driver != null && isInChunk(driver.getLocation(), chunk)) {
                mob.handleChunkUnload();
            }
        }

        removeOrphansInChunk(chunk);
    }

    public void handleEntitiesLoad(Chunk chunk, List<Entity> entities) {
        if (chunk == null || entities == null) return;

        List<LivingEntity> drivers = new ArrayList<>();
        List<Entity> visuals = new ArrayList<>();

        for (Entity entity : entities) {
            if (!hasInstanceTag(entity)) continue;
            if (entity instanceof LivingEntity living) {
                drivers.add(living);
            } else if (entity instanceof Display || entity instanceof Interaction) {
                visuals.add(entity);
            }
        }

        for (LivingEntity driver : drivers) {
            handleLoadedDriver(driver);
        }

        for (Entity visual : visuals) {
            UUID instanceId = readInstanceId(visual);
            CustomMob mob = instanceId != null ? mobsByInstance.get(instanceId) : null;
            if (mob == null || !mob.isAttached()) {
                if (visual.isValid()) {
                    visual.remove();
                }
            }
        }
    }

    private void handleLoadedDriver(LivingEntity driver) {
        UUID instanceId = readInstanceId(driver);
        if (instanceId == null) {
            driver.remove();
            return;
        }

        CustomMob existing = mobsByInstance.get(instanceId);
        if (existing != null) {
            LivingEntity current = existing.getDriver();
            if (current != null && current.getUniqueId().equals(driver.getUniqueId())
                    && existing.isAttached()) {
                return;
            }
            existing.reattach(driver);
            return;
        }

        String definitionId = driver.getPersistentDataContainer()
                .get(keys.getDefinition(), PersistentDataType.STRING);
        MobDefinition definition = getDefinition(definitionId);
        if (definition == null) {
            driver.remove();
            return;
        }

        CustomMob mob = new CustomMob(plugin, definition, driver, instanceId, keys);
        try {
            mob.attach();
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "[CocoNPC] Could not restore custom mob " + definitionId, exception);
            driver.remove();
            return;
        }

        register(mob);
        ticker.attach(mob);
    }

    public void bootstrap() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                handleEntitiesLoad(chunk, Arrays.asList(chunk.getEntities()));
            }
        }
        sweepOrphans();
    }

    public void shutdown() {
        ticker.stop();

        List<CustomMob> snapshot = new ArrayList<>(activeMobs);
        for (CustomMob mob : snapshot) {
            mob.handleChunkUnload();
        }

        activeMobs.clear();
        mobsByDriver.clear();
        mobsByInstance.clear();

        sweepOrphans();
    }

    public void sweepOrphans() {
        for (World world : Bukkit.getWorlds()) {
            for (Display display : world.getEntitiesByClass(Display.class)) {
                removeIfOrphan(display);
            }
            for (Interaction interaction : world.getEntitiesByClass(Interaction.class)) {
                removeIfOrphan(interaction);
            }
        }
    }

    private void removeIfOrphan(Entity entity) {
        UUID instanceId = readInstanceId(entity);
        if (instanceId == null) return;

        CustomMob mob = mobsByInstance.get(instanceId);
        if (mob == null || !mob.isAttached()) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
    }

    private void removeOrphansInChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Display) && !(entity instanceof Interaction)) continue;

            UUID instanceId = readInstanceId(entity);
            if (instanceId == null) continue;

            CustomMob mob = mobsByInstance.get(instanceId);
            if (mob == null || !mob.isAttached()) {
                if (entity.isValid()) {
                    entity.remove();
                }
            }
        }
    }

    private boolean isInChunk(Location location, Chunk chunk) {
        return location.getWorld() == chunk.getWorld()
                && (location.getBlockX() >> 4) == chunk.getX()
                && (location.getBlockZ() >> 4) == chunk.getZ();
    }

    private boolean hasInstanceTag(Entity entity) {
        return entity.getPersistentDataContainer().has(keys.getInstance(),
                PersistentDataType.STRING);
    }

    private UUID readInstanceId(Entity entity) {
        String raw = entity.getPersistentDataContainer()
                .get(keys.getInstance(), PersistentDataType.STRING);
        if (raw == null) return null;

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public MobKeys getKeys() {
        return keys;
    }

    public int getDefinitionCount() {
        return definitions.size();
    }

    public int getMobCount() {
        return activeMobs.size();
    }

    public EntityType getDriverType(String definitionId) {
        MobDefinition definition = getDefinition(definitionId);
        return definition != null ? definition.getDriverType() : null;
    }
}
