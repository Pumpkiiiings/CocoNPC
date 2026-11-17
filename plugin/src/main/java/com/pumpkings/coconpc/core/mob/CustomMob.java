package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.mob.model.ModelRig;
import com.pumpkings.coconpc.util.AttributeCompat;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class CustomMob {
    private static final float TICK_SECONDS = 0.05f;
    private static final int MISSING_DRIVER_LIMIT = 40;

    private final CocoNPC plugin;
    private MobDefinition definition;
    private final UUID instanceId;
    private final MobKeys keys;

    private LivingEntity driver;
    private ModelRig rig;
    private AnimationController controller;
    private Interaction interaction;
    private volatile List<String> instanceActions;

    private boolean attached;
    private boolean dying;
    private boolean cleaned;
    private boolean configuredOnce;
    private boolean migrating;
    private int deathGrace;
    private int flashTicks;
    private int ticks;
    private int missingTicks;
    private double lastX;
    private double lastZ;

    CustomMob(CocoNPC plugin, MobDefinition definition, LivingEntity driver,
              UUID instanceId, MobKeys keys) {
        this.plugin = plugin;
        this.definition = definition;
        this.driver = driver;
        this.instanceId = instanceId;
        this.keys = keys;
        this.lastX = driver.getLocation().getX();
        this.lastZ = driver.getLocation().getZ();
    }

    void attach() {
        if (cleaned || attached) return;
        configureDriver();
        attachVisuals();
    }

    void reattach(LivingEntity entity) {
        if (cleaned) return;

        UUID oldDriverId = driver != null ? driver.getUniqueId() : null;
        this.driver = entity;
        this.attached = false;
        configureDriver();
        attachVisuals();

        if (plugin.getMobRegistry() != null) {
            plugin.getMobRegistry().updateDriver(this, oldDriverId, entity);
        }
    }

    /**
     * Applies a reloaded definition. When the driver type changes, the old
     * entity is replaced by a new one of the requested type so AI, gravity and
     * collisions match the new configuration while keeping animations alive.
     */
    void updateDefinition(MobDefinition updated) {
        if (cleaned || updated == null) return;
        if (!updated.getId().equalsIgnoreCase(definition.getId())) return;

        if (driver != null && updated.getDriverType() != definition.getDriverType()) {
            migrateDriver(updated);
            return;
        }

        this.definition = updated;
        if (driver != null) {
            configureDriver();
        }
        if (attached) {
            detachVisuals();
            attachVisuals();
        }
    }

    /**
     * Points this instance at a definition whose mutable values (scale,
     * actions...) changed. Displays are not rebuilt, so the model keeps its
     * current textures and pose; {@link ModelRig} reads the new scale on the
     * next tick.
     */
    void syncDefinition(MobDefinition updated) {
        if (cleaned || updated == null) return;
        if (!updated.getId().equalsIgnoreCase(definition.getId())) return;
        this.definition = updated;
    }

    private void migrateDriver(MobDefinition updated) {
        EntityType type = updated.getDriverType();
        if (!type.isSpawnable() || !type.isAlive() || type.getEntityClass() == null) {
            plugin.getLogger().warning("[CocoNPC] Cannot migrate mob '" + definition.getId()
                    + "' to unsupported driver " + type + "; keeping the current driver.");
            this.definition = updated;
            configureDriver();
            return;
        }

        LivingEntity previous = driver;
        if (previous == null || !previous.isValid()) {
            this.definition = updated;
            return;
        }

        Location location = previous.getLocation().clone();
        UUID previousId = previous.getUniqueId();

        detachVisuals();
        migrating = true;
        // Drop the old mapping before removing the entity so a delayed
        // EntityRemoveEvent cannot clean up the new mob instance.
        if (plugin.getMobRegistry() != null) {
            plugin.getMobRegistry().updateDriver(this, previousId, null);
        }

        Entity spawned;
        try {
            spawned = location.getWorld().spawnEntity(location, type,
                    CreatureSpawnEvent.SpawnReason.CUSTOM);
        } catch (RuntimeException exception) {
            migrating = false;
            plugin.getLogger().log(Level.WARNING, "[CocoNPC] Could not migrate mob '"
                    + definition.getId() + "' to driver " + type, exception);
            if (plugin.getMobRegistry() != null) {
                plugin.getMobRegistry().updateDriver(this, null, previous);
            }
            attachVisuals();
            return;
        }

        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            migrating = false;
            if (plugin.getMobRegistry() != null) {
                plugin.getMobRegistry().updateDriver(this, null, previous);
            }
            attachVisuals();
            return;
        }

        previous.remove();
        migrating = false;

        this.driver = living;
        this.definition = updated;
        configureDriver();
        attachVisuals();

        if (plugin.getMobRegistry() != null) {
            plugin.getMobRegistry().updateDriver(this, null, living);
        }
    }

    /**
     * Recreates every display with the visuals currently stored in the
     * definition. Runs on the driver's region thread.
     */

    void refreshVisuals(MobDefinition updated) {
        if (cleaned || !attached || driver == null || !driver.isValid()) return;
        if (updated != null) {
            this.definition = updated;
        }
        detachVisuals();
        attachVisuals();
    }
    private void attachVisuals() {
        if (attached || driver == null) return;

        Location location = driver.getLocation();
        rig = new ModelRig(plugin, definition, keys, instanceId, location);
        rig.spawn();
        controller = new AnimationController(rig, definition);
        if (definition.getDefaultAnimation() != null) {
            controller.playCustomAnimation(definition.getDefaultAnimation(), false);
        }
        rig.apply(location, location.getYaw());

        if (definition.isInteractionEnabled()) {
            spawnInteraction(location);
        }

        lastX = location.getX();
        lastZ = location.getZ();
        flashTicks = 0;
        missingTicks = 0;
        dying = false;
        deathGrace = 0;
        attached = true;
    }

    void detachVisuals() {
        attached = false;
        if (rig != null) {
            rig.setFlash(false);
            rig.despawn();
            rig = null;
        }
        controller = null;
        removeInteraction();
        flashTicks = 0;
    }

    void tick() {
        if (cleaned) return;

        LivingEntity entity = driver;
        if (entity == null) {
            cleanup(false);
            return;
        }

        if (!entity.isValid() || entity.isDead()) {
            if (dying) {
                cleanup(true);
                return;
            }

            if (attached) {
                detachVisuals();
            }

            missingTicks++;
            if (missingTicks > MISSING_DRIVER_LIMIT) {
                cleanup(false);
            }
            return;
        }

        if (!attached) return;

        missingTicks = 0;
        Location location = entity.getLocation();

        float horizontalSpeed = 0f;
        if (!dying) {
            double dx = location.getX() - lastX;
            double dz = location.getZ() - lastZ;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < 4.0) {
                horizontalSpeed = (float) Math.sqrt(distanceSquared);
            }
            lastX = location.getX();
            lastZ = location.getZ();
        }

        controller.tick(TICK_SECONDS, horizontalSpeed);
        rig.apply(location, location.getYaw());
        updateInteraction(location);

        if (flashTicks > 0) {
            flashTicks--;
            if (flashTicks == 0 && rig != null) {
                rig.setFlash(false);
            }
        }

        ticks++;
        if ((ticks & 15) == 0) {
            maintainDriver(entity);
        }

        if (dying && controller.isDeathComplete()) {
            if (deathGrace > 0) {
                deathGrace--;
            } else {
                cleanup(true);
            }
        }
    }

    private void configureDriver() {
        if (driver == null) return;

        driver.setInvisible(true);
        driver.setSilent(true);
        driver.setCustomNameVisible(false);
        driver.setPersistent(true);
        driver.setRemoveWhenFarAway(false);
        driver.setCanPickupItems(false);
        driver.setAI(definition.isAiEnabled());
        driver.setInvulnerable(definition.isInvulnerable());
        driver.setGravity(definition.isGravity());
        driver.setCollidable(definition.isCollidable());
        driver.setFallDistance(0f);
        driver.clearActivePotionEffects();

        if (driver instanceof ArmorStand stand) {
            stand.setMarker(definition.isMarker());
            stand.setBasePlate(false);
            stand.setArms(false);
        }

        if (definition.getMaxHealth() > 0) {
            Attribute maxHealth = AttributeCompat.maxHealth();
            AttributeInstance attribute = maxHealth != null ? driver.getAttribute(maxHealth) : null;
            if (attribute != null) {
                attribute.setBaseValue(definition.getMaxHealth());
            }
            if (!configuredOnce || driver.getHealth() > definition.getMaxHealth()) {
                driver.setHealth(definition.getMaxHealth());
            }
        }

        Consumer<LivingEntity> initializer = definition.getDriverInitializer();
        if (initializer != null) {
            initializer.accept(driver);
        }

        if (driver.getEquipment() != null) {
            driver.getEquipment().clear();
        }

        driver.getPersistentDataContainer().set(keys.getInstance(),
                PersistentDataType.STRING, instanceId.toString());
        driver.getPersistentDataContainer().set(keys.getDefinition(),
                PersistentDataType.STRING, definition.getId());

        configuredOnce = true;
    }

    private void maintainDriver(LivingEntity entity) {
        if (!entity.isInvisible()) {
            entity.setInvisible(true);
        }
        if (!entity.isSilent()) {
            entity.setSilent(true);
        }
    }

    private void spawnInteraction(Location location) {
        try {
            interaction = driver.getWorld().spawn(location, Interaction.class,
                    CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    });
            interaction.setInteractionWidth(definition.getInteractionWidth());
            interaction.setInteractionHeight(definition.getInteractionHeight());
            interaction.setResponsive(true);
            interaction.setPersistent(false);
            interaction.setInvulnerable(true);
            interaction.setSilent(true);
            interaction.setGravity(false);
            interaction.getPersistentDataContainer().set(keys.getInstance(),
                    PersistentDataType.STRING, instanceId.toString());
            interaction.getPersistentDataContainer().set(keys.getDefinition(),
                    PersistentDataType.STRING, definition.getId());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "[CocoNPC] Could not spawn the interaction hitbox for mob " + definition.getId(),
                    exception);
            interaction = null;
        }
    }

    private void updateInteraction(Location location) {
        if (interaction == null) return;
        if (!interaction.isValid()) {
            interaction = null;
            return;
        }

        if (interaction.getWorld() != location.getWorld()
                || interaction.getLocation().distanceSquared(location) > 0.0025) {
            interaction.teleport(location);
        }
    }

    private void removeInteraction() {
        if (interaction != null) {
            if (interaction.isValid()) {
                interaction.remove();
            }
            interaction = null;
        }
    }

    void onDamage(EntityDamageEvent event) {
        if (cleaned || dying || controller == null) return;
        if (event.getFinalDamage() >= driver.getHealth()) return;

        controller.triggerHurt();
        int flashDuration = definition.getAnimationSet().getHurtFlashTicks();
        if (flashDuration > 0 && rig != null) {
            flashTicks = Math.max(flashTicks, flashDuration);
            rig.setFlash(true);
        }
    }

    void onDeath(EntityDeathEvent event) {
        if (cleaned || dying || event.isCancelled()) return;

        event.setCancelled(true);
        event.setReviveHealth(1.0);
        event.setDroppedExp(0);
        event.getDrops().clear();
        event.setShouldPlayDeathSound(false);

        dying = true;
        deathGrace = controller != null && controller.triggerDeath() ? 0 : 10;

        driver.setAI(false);
        driver.setInvulnerable(true);
        driver.setGravity(false);
        driver.setVelocity(new Vector());
        if (driver instanceof Mob mob) {
            mob.setTarget(null);
        }
        removeInteraction();

        flashTicks = 0;
        if (rig != null) {
            rig.setFlash(false);
        }
    }

    void onDriverRemoved(EntityRemoveEvent.Cause cause) {
        if (cleaned || migrating) return;
        if (cause == EntityRemoveEvent.Cause.UNLOAD) {
            detachVisuals();
            return;
        }
        cleanup(false);
    }

    void handleChunkUnload() {
        if (cleaned) return;
        if (dying) {
            cleanup(true);
            return;
        }
        detachVisuals();
    }

    public void cleanup(boolean removeDriver) {
        if (cleaned) return;
        cleaned = true;
        attached = false;

        if (rig != null) {
            rig.setFlash(false);
            rig.despawn();
            rig = null;
        }
        controller = null;
        removeInteraction();

        if (removeDriver && driver != null && driver.isValid()) {
            driver.remove();
        }

        if (plugin.getMobRegistry() != null) {
            plugin.getMobRegistry().unregister(this);
        }
    }

    void onAggro(LivingEntity target) {
        if (cleaned || dying || target == null) return;
        if (definition.getAnimationSet().isAttackOnAggro()) {
            triggerAttack();
        }
    }

    public boolean triggerAttack() {
        return !cleaned && !dying && controller != null && controller.triggerAttack();
    }

    /**
     * Replaces the actions of this instance. Passing {@code null} falls back to
     * the actions configured in the definition.
     */
    public void setActions(List<String> actions) {
        if (actions == null) {
            this.instanceActions = null;
            return;
        }
        List<String> copy = new ArrayList<>();
        for (String action : actions) {
            if (action != null && !action.isBlank()) {
                copy.add(action.trim());
            }
        }
        this.instanceActions = Collections.unmodifiableList(copy);
    }

    public List<String> getActions() {
        List<String> own = instanceActions;
        return own != null ? own : definition.getActions();
    }

    public boolean playCustomAnimation(String name, boolean loop) {
        if (cleaned || dying || controller == null) return false;
        return controller.playCustomAnimation(name, loop);
    }

    public boolean stopCustomAnimation() {
        if (cleaned || dying || controller == null) return false;
        return controller.stopCustomAnimation();
    }

    public Set<String> getCustomAnimationNames() {
        if (controller != null) {
            return controller.getCustomAnimationNames();
        }
        return Collections.unmodifiableSet(
                new java.util.LinkedHashSet<>(definition.getAnimationNames()));
    }

    public boolean hasCustomAnimation(String name) {
        if (controller != null) {
            return controller.hasCustomAnimation(name);
        }
        return definition.hasAnimation(name);
    }

    public String getCurrentAnimationName() {
        return controller != null ? controller.getCurrentAnimationName() : null;
    }

    public Location getLocation() {
        return driver != null ? driver.getLocation() : null;
    }

    /**
     * Moves the driver (and therefore the whole model) to the given location on
     * its owning region thread. Works for static ARMOR_STAND drivers too.
     */
    public boolean teleportTo(Location location) {
        if (cleaned || driver == null || location == null) return false;

        LivingEntity entity = driver;
        Runnable move = () -> {
            entity.teleport(location);
            lastX = location.getX();
            lastZ = location.getZ();
        };

        try {
            return entity.getScheduler().run(plugin, task -> move.run(), null) != null;
        } catch (RuntimeException exception) {
            move.run();
            return true;
        }
    }

    void onInteraction(Player player, EquipmentSlot hand) {
        if (cleaned || dying || player == null) return;
        // Clients fire the event once per hand; only the main hand should run
        // the actions to avoid executing them twice.
        if (hand == EquipmentSlot.OFF_HAND) return;

        List<String> actions = getActions();
        if (actions != null && !actions.isEmpty()) {
            double cooldown = definition.getInteractionCooldown();
            boolean allowed = true;
            if (cooldown > 0.0 && plugin.getCooldownManager() != null) {
                allowed = plugin.getCooldownManager().tryCooldown(player,
                        interactionCooldownKey(), formatCooldown(cooldown));
            }
            if (!allowed) return;

            if (plugin.getActionsManager() != null) {
                plugin.getActionsManager().runActions(player,
                        bindSelfActions(actions), actionCooldownKey());
            }
        }

        Consumer<Player> handler = definition.getInteractionHandler();
        if (handler != null) {
            handler.accept(player);
        }
    }

    private String interactionCooldownKey() {
        return "mob-interact:" + instanceId;
    }

    private String actionCooldownKey() {
        return "mob:" + instanceId;
    }

    private static String formatCooldown(double seconds) {
        return Math.max(1L, (long) Math.ceil(seconds)) + "s";
    }

    /**
     * Binds mob-scoped animate tags to this exact instance:
     * {@code [animate] greet} and {@code [mob_animate] greet true} become
     * {@code [mob_animate] #<instance> greet true}, so the ActionsManager can
     * dispatch them without knowing which mob triggered the click.
     */
    private List<String> bindSelfActions(List<String> actions) {
        List<String> bound = new ArrayList<>(actions.size());
        for (String raw : actions) {
            if (raw == null) continue;

            String trimmed = raw.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);

            if (lower.startsWith("[animate]")) {
                String rest = trimmed.substring("[animate]".length()).trim();
                bound.add(selfAnimate(rest));
                continue;
            }

            if (lower.startsWith("[mob_animate]")) {
                String rest = trimmed.substring("[mob_animate]".length()).trim();
                String[] tokens = rest.isEmpty() ? new String[0] : rest.split("\\s+");

                if (tokens.length > 0 && tokens[0].equalsIgnoreCase("self")) {
                    String tail = rest.substring(tokens[0].length()).trim();
                    bound.add(selfAnimate(tail));
                    continue;
                }
                if (tokens.length == 0 || tokens[0].startsWith("#")) {
                    bound.add(trimmed);
                    continue;
                }
                if (tokens.length == 1 || (tokens.length == 2 && isLoopToken(tokens[1]))) {
                    bound.add(selfAnimate(rest));
                    continue;
                }
                bound.add(trimmed);
                continue;
            }

            bound.add(raw);
        }
        return bound;
    }

    private String selfAnimate(String argument) {
        return "[mob_animate] #" + instanceId
                + (argument == null || argument.isBlank() ? "" : " " + argument.trim());
    }

    private static boolean isLoopToken(String token) {
        return token.equalsIgnoreCase("loop")
                || token.equalsIgnoreCase("true")
                || token.equalsIgnoreCase("false")
                || token.equals("1")
                || token.equals("0");
    }

    public UUID getInstanceId() {
        return instanceId;
    }

    public LivingEntity getDriver() {
        return driver;
    }

    public MobDefinition getDefinition() {
        return definition;
    }

    public ModelRig getRig() {
        return rig;
    }

    public AnimationController getController() {
        return controller;
    }

    public Interaction getInteraction() {
        return interaction;
    }

    public boolean isAttached() {
        return attached;
    }

    public boolean isDying() {
        return dying;
    }

    public boolean isCleaned() {
        return cleaned;
    }
}
