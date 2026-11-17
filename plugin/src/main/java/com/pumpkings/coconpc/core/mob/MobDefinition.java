package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.core.mob.model.BbAnimation;
import com.pumpkings.coconpc.core.mob.model.BbModel;
import com.pumpkings.coconpc.core.mob.model.BbModelParser;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class MobDefinition {
    private final String id;
    private final EntityType driverType;
    private final BbModel model;
    private volatile double scale;
    private final float yawOffset;
    private final MobAnimationSet animationSet;
    private final MobVisualPalette palette;
    private final Map<MobAnimationState, BbAnimation> clips;
    private final boolean interactionEnabled;
    private final float interactionWidth;
    private final float interactionHeight;
    private final boolean aiEnabled;
    private final double maxHealth;
    private final float viewRange;
    private final int interpolationDuration;
    private final int interpolationDelay;
    private final int teleportDuration;
    private final double reanchorDistance;
    private final ItemDisplay.ItemDisplayTransform itemDisplayTransform;
    private final boolean gravity;
    private final boolean collidable;
    private final boolean invulnerable;
    private final boolean marker;
    private final MobTextureMode textureMode;
    private final MobCubeVisuals cubeVisuals;
    private final Path modelPath;
    private final Consumer<LivingEntity> driverInitializer;
    private final Consumer<Player> interactionHandler;
    private volatile List<String> actions;
    private final double interactionCooldown;
    private final String defaultAnimation;

    private MobDefinition(Builder builder, Map<MobAnimationState, BbAnimation> clips) {
        this.id = builder.id;
        this.driverType = builder.driverType;
        this.model = builder.model;
        this.scale = builder.scale;
        this.yawOffset = builder.yawOffset;
        this.animationSet = builder.animationSet;
        this.palette = builder.palette;
        this.clips = Collections.unmodifiableMap(new EnumMap<>(clips));
        this.interactionEnabled = builder.interactionEnabled;
        this.interactionWidth = builder.interactionWidth;
        this.interactionHeight = builder.interactionHeight;
        this.aiEnabled = builder.aiEnabled;
        this.maxHealth = builder.maxHealth;
        this.viewRange = builder.viewRange;
        this.interpolationDuration = builder.interpolationDuration;
        this.interpolationDelay = builder.interpolationDelay;
        this.teleportDuration = builder.teleportDuration;
        this.reanchorDistance = builder.reanchorDistance;
        this.itemDisplayTransform = builder.itemDisplayTransform;
        this.gravity = builder.gravity;
        this.collidable = builder.collidable;
        this.invulnerable = builder.invulnerable;
        this.marker = builder.marker;
        this.textureMode = builder.textureMode;
        this.cubeVisuals = builder.cubeVisuals;
        this.modelPath = builder.modelPath;
        this.driverInitializer = builder.driverInitializer;
        this.interactionHandler = builder.interactionHandler;
        this.actions = Collections.unmodifiableList(new ArrayList<>(builder.actions));
        this.interactionCooldown = builder.interactionCooldown;
        this.defaultAnimation = builder.defaultAnimation;
    }

    public static Builder builder(String id, EntityType driverType, BbModel model) {
        return new Builder(id, driverType, model);
    }

    public static Builder builder(String id, EntityType driverType, Path modelPath, Logger logger) {
        BbModel model = BbModelParser.parse(modelPath, logger);
        if (model == null) {
            throw new IllegalArgumentException("Could not load .bbmodel: " + modelPath);
        }
        return builder(id, driverType, model);
    }

    public static Builder builder(String id, EntityType driverType, File modelFile, Logger logger) {
        return builder(id, driverType, modelFile != null ? modelFile.toPath() : null, logger);
    }

    public void validate(Logger logger) {
        if (logger == null) return;
        if (clips.get(MobAnimationState.IDLE) == null) {
            logger.warning("[CocoNPC] Mob '" + id + "' has no idle animation ("
                    + animationSet.getName(MobAnimationState.IDLE) + ").");
        }
        if (clips.get(MobAnimationState.WALK) == null) {
            logger.warning("[CocoNPC] Mob '" + id + "' has no walk animation ("
                    + animationSet.getName(MobAnimationState.WALK) + ").");
        }
        if (defaultAnimation != null && !defaultAnimation.isBlank()
                && model.findAnimation(defaultAnimation) == null) {
            logger.warning("[CocoNPC] Mob '" + id + "' sets animation '"
                    + defaultAnimation + "' but the model has no such animation.");
        }
    }

    public String getId() {
        return id;
    }

    public EntityType getDriverType() {
        return driverType;
    }

    public BbModel getModel() {
        return model;
    }

    public double getScale() {
        return scale;
    }

    /**
     * Updates the live scale. Every rig reads this value on each tick, so all
     * active mobs using this definition resize without rebuilding their
     * displays or textures.
     */
    public void setScale(double scale) {
        this.scale = Math.max(0.0001, Math.min(10.0, scale));
    }

    public float getYawOffset() {
        return yawOffset;
    }

    public MobAnimationSet getAnimationSet() {
        return animationSet;
    }

    public MobVisualPalette getPalette() {
        return palette;
    }

    public BbAnimation getAnimation(MobAnimationState state) {
        return clips.get(state);
    }

    public Map<MobAnimationState, BbAnimation> getClips() {
        return clips;
    }

    public boolean isInteractionEnabled() {
        return interactionEnabled;
    }

    public float getInteractionWidth() {
        return interactionWidth;
    }

    public float getInteractionHeight() {
        return interactionHeight;
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public float getViewRange() {
        return viewRange;
    }

    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    public int getInterpolationDelay() {
        return interpolationDelay;
    }

    public int getTeleportDuration() {
        return teleportDuration;
    }

    public double getReanchorDistance() {
        return reanchorDistance;
    }

    public ItemDisplay.ItemDisplayTransform getItemDisplayTransform() {
        return itemDisplayTransform;
    }

    public boolean isGravity() {
        return gravity;
    }

    public boolean isCollidable() {
        return collidable;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isMarker() {
        return marker;
    }

    public MobTextureMode getTextureMode() {
        return textureMode;
    }

    public MobCubeVisuals getCubeVisuals() {
        return cubeVisuals;
    }

    public Path getModelPath() {
        return modelPath;
    }

    public Consumer<LivingEntity> getDriverInitializer() {
        return driverInitializer;
    }

    public Consumer<Player> getInteractionHandler() {
        return interactionHandler;
    }

    public List<String> getActions() {
        return actions;
    }

    /**
     * Replaces the live action list. Instances without their own override run
     * these actions on right-click immediately.
     */
    public void setActions(List<String> actions) {
        List<String> copy = new ArrayList<>();
        if (actions != null) {
            for (String action : actions) {
                if (action != null && !action.isBlank()) {
                    copy.add(action.trim());
                }
            }
        }
        this.actions = Collections.unmodifiableList(copy);
    }

    public double getInteractionCooldown() {
        return interactionCooldown;
    }

    public String getDefaultAnimation() {
        return defaultAnimation;
    }

    public boolean hasAnimation(String name) {
        return model != null && model.findAnimation(name) != null;
    }

    public Set<String> getAnimationNames() {
        return model != null ? model.getAnimations().keySet() : Collections.emptySet();
    }

    public static final class Builder {
        private final String id;
        private final EntityType driverType;
        private final BbModel model;
        private double scale = 1.0;
        private float yawOffset;
        private MobAnimationSet animationSet = MobAnimationSet.defaults();
        private MobVisualPalette palette = MobVisualPalette.defaultPalette();
        private boolean interactionEnabled;
        private float interactionWidth = 0.9f;
        private float interactionHeight = 1.8f;
        private boolean aiEnabled = true;
        private double maxHealth = -1.0;
        private float viewRange = 2.0f;
        private int interpolationDuration = 1;
        private int interpolationDelay;
        private int teleportDuration = 1;
        private double reanchorDistance = 4.0;
        private ItemDisplay.ItemDisplayTransform itemDisplayTransform = ItemDisplay.ItemDisplayTransform.FIXED;
        private boolean gravity;
        private boolean collidable;
        private boolean invulnerable;
        private boolean marker;
        private MobTextureMode textureMode = MobTextureMode.SKINS;
        private MobCubeVisuals cubeVisuals = new MobCubeVisuals();
        private Path modelPath;
        private Consumer<LivingEntity> driverInitializer;
        private Consumer<Player> interactionHandler;
        private List<String> actions = new ArrayList<>();
        private double interactionCooldown;
        private String defaultAnimation;

        private Builder(String id, EntityType driverType, BbModel model) {
            this.id = Objects.requireNonNull(id, "id");
            this.driverType = Objects.requireNonNull(driverType, "driverType");
            this.model = Objects.requireNonNull(model, "model");

            boolean armorStandDriver = driverType == EntityType.ARMOR_STAND;
            this.aiEnabled = !armorStandDriver;
            this.gravity = !armorStandDriver;
            this.collidable = !armorStandDriver;
            this.invulnerable = armorStandDriver;
            this.marker = armorStandDriver;
        }

        public Builder scale(double scale) {
            this.scale = Math.max(0.0001, scale);
            return this;
        }

        public Builder yawOffset(float yawOffset) {
            this.yawOffset = yawOffset;
            return this;
        }

        public Builder animationSet(MobAnimationSet animationSet) {
            this.animationSet = Objects.requireNonNull(animationSet, "animationSet");
            return this;
        }

        public Builder palette(MobVisualPalette palette) {
            this.palette = Objects.requireNonNull(palette, "palette");
            return this;
        }

        public Builder interaction(boolean enabled, float width, float height) {
            this.interactionEnabled = enabled;
            this.interactionWidth = Math.max(0.05f, width);
            this.interactionHeight = Math.max(0.05f, height);
            return this;
        }

        public Builder ai(boolean enabled) {
            this.aiEnabled = enabled;
            return this;
        }

        public Builder gravity(boolean enabled) {
            this.gravity = enabled;
            return this;
        }

        public Builder collidable(boolean enabled) {
            this.collidable = enabled;
            return this;
        }

        public Builder invulnerable(boolean enabled) {
            this.invulnerable = enabled;
            return this;
        }

        public Builder marker(boolean enabled) {
            this.marker = enabled;
            return this;
        }

        public Builder textureMode(MobTextureMode mode) {
            this.textureMode = mode != null ? mode : MobTextureMode.SKINS;
            return this;
        }

        public Builder cubeVisuals(MobCubeVisuals visuals) {
            this.cubeVisuals = visuals != null ? visuals : new MobCubeVisuals();
            return this;
        }

        public Builder modelPath(Path modelPath) {
            this.modelPath = modelPath;
            return this;
        }

        public Builder maxHealth(double maxHealth) {
            this.maxHealth = maxHealth;
            return this;
        }

        public Builder viewRange(float viewRange) {
            this.viewRange = Math.max(0.0f, Math.min(4.0f, viewRange));
            return this;
        }

        public Builder interpolation(int duration, int delay) {
            this.interpolationDuration = Math.max(0, Math.min(59, duration));
            this.interpolationDelay = Math.max(0, Math.min(59, delay));
            return this;
        }

        public Builder teleportDuration(int duration) {
            this.teleportDuration = Math.max(0, Math.min(59, duration));
            return this;
        }

        public Builder reanchorDistance(double distance) {
            this.reanchorDistance = Math.max(0.5, distance);
            return this;
        }

        public Builder itemDisplayTransform(ItemDisplay.ItemDisplayTransform transform) {
            this.itemDisplayTransform = Objects.requireNonNull(transform, "transform");
            return this;
        }

        public Builder driverInitializer(Consumer<LivingEntity> initializer) {
            this.driverInitializer = initializer;
            return this;
        }

        public Builder interactionHandler(Consumer<Player> handler) {
            this.interactionHandler = handler;
            return this;
        }

        public Builder actions(List<String> actions) {
            this.actions = new ArrayList<>();
            if (actions != null) {
                for (String action : actions) {
                    if (action != null && !action.isBlank()) {
                        this.actions.add(action.trim());
                    }
                }
            }
            return this;
        }

        public Builder interactionCooldown(double seconds) {
            this.interactionCooldown = Math.max(0.0, seconds);
            return this;
        }

        public Builder defaultAnimation(String animationName) {
            this.defaultAnimation = animationName != null && !animationName.isBlank()
                    ? animationName.trim()
                    : null;
            return this;
        }

        public MobDefinition build() {
            Map<MobAnimationState, BbAnimation> clips = new EnumMap<>(MobAnimationState.class);
            for (MobAnimationState state : MobAnimationState.values()) {
                String animationName = animationSet.getName(state);
                BbAnimation animation = model.findAnimation(animationName);
                if (animation != null) {
                    clips.put(state, animation);
                }
            }
            return new MobDefinition(this, clips);
        }
    }
}
