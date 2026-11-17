package com.pumpkings.coconpc.core.mob.model;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.mob.BoneVisual;
import com.pumpkings.coconpc.core.mob.MobDefinition;
import com.pumpkings.coconpc.core.mob.MobKeys;
import com.pumpkings.coconpc.util.SkinUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ModelRig {
    private static final float PIXEL_SCALE = 1f / 16f;
    private static final Color FLASH_COLOR = Color.fromRGB(255, 40, 40);

    private static final float HEAD_BASE_PIXELS = 8f;
    private static final float BLOCK_BASE_PIXELS = 16f;

    private final CocoNPC plugin;
    private final MobDefinition definition;
    private final MobKeys keys;
    private final UUID instanceId;
    private final BbModel model;

    private final List<ModelBone> roots = new ArrayList<>();
    private final List<ModelBone> bones = new ArrayList<>();
    private final Map<String, ModelBone> bonesByName = new HashMap<>();
    private final Map<String, ModelBone> bonesByUuid = new HashMap<>();

    private final Matrix4f rootMatrix = new Matrix4f();
    private final float reanchorDistanceSquared;

    private Location anchor;
    private World world;
    private boolean spawned;

    public ModelRig(CocoNPC plugin, MobDefinition definition, MobKeys keys,
                    UUID instanceId, Location anchor) {
        this.plugin = plugin;
        this.definition = definition;
        this.keys = keys;
        this.instanceId = instanceId;
        this.model = definition.getModel();
        this.anchor = cleanLocation(anchor);
        this.world = anchor != null ? anchor.getWorld() : null;
        this.reanchorDistanceSquared = (float) (definition.getReanchorDistance()
                * definition.getReanchorDistance());

        for (BbModel.Bone root : model.getRoots()) {
            roots.add(buildBone(root, null, new Vector3f()));
        }
    }

    public static String cubeKey(BbModel.Bone bone, BbModel.Cube cube, int index) {
        if (cube != null && cube.getUuid() != null && !cube.getUuid().isBlank()) {
            return "cube:" + cube.getUuid();
        }
        String boneName = bone != null ? bone.getName() : "bone";
        return "cube:" + boneName + "#" + index;
    }

    private ModelBone buildBone(BbModel.Bone source, ModelBone parent, Vector3f parentPivotPixels) {
        Vector3f pivotPixels = source.getOrigin();
        Vector3f localOffset = new Vector3f(pivotPixels)
                .sub(parentPivotPixels)
                .mul(PIXEL_SCALE);

        Quaternionf restRotation = euler(source.getRotation());
        Vector3f restScale = new Vector3f(1f, 1f, 1f);

        ModelBone bone = new ModelBone(source.getName(), source.getUuid(), source.getName(), parent,
                localOffset, restRotation, restScale, new Matrix4f(), null);

        bones.add(bone);
        bonesByName.putIfAbsent(source.getName(), bone);
        if (source.getUuid() != null) {
            bonesByUuid.put(source.getUuid(), bone);
        }

        int index = 0;
        for (BbModel.Cube cube : source.getCubes()) {
            String cubeKey = cubeKey(source, cube, index);
            BoneVisual visual = resolveVisual(cubeKey, source.getName(), cube);

            Vector3f cubePivot = cube.getPivot();
            Vector3f cubeOffset = new Vector3f(cubePivot)
                    .sub(pivotPixels)
                    .mul(PIXEL_SCALE);
            Quaternionf cubeRotation = euler(cube.getRotation());
            Matrix4f cubeVisual = cubeMatrix(cube, visual);

            ModelBone cubeBone = new ModelBone(cubeKey, cube.getUuid(), source.getName(), bone,
                    cubeOffset, cubeRotation, new Vector3f(1f, 1f, 1f), cubeVisual, visual);
            bone.addChild(cubeBone);
            bones.add(cubeBone);
            index++;
        }

        for (BbModel.Bone child : source.getChildren()) {
            bone.addChild(buildBone(child, bone, pivotPixels));
        }

        return bone;
    }

    private BoneVisual resolveVisual(String cubeKey, String boneName, BbModel.Cube cube) {
        BoneVisual visual = definition.getCubeVisuals().get(cubeKey);
        if (visual != null && !visual.isNone()) {
            return visual;
        }
        return definition.getPalette().resolve(boneName, cube.getTexture());
    }

    private Matrix4f cubeMatrix(BbModel.Cube cube, BoneVisual visual) {
        Vector3f pivot = cube.getPivot();
        Vector3f center = cube.getCenter();
        Vector3f size = cube.getSize();

        boolean head = visual != null && visual.isHead();
        boolean block = visual != null && visual.isBlock();
        float base = head ? HEAD_BASE_PIXELS : BLOCK_BASE_PIXELS;
        float extraScale = visual != null ? visual.scale() : 1f;

        float scaleX = Math.max(0.001f, size.x) / base * extraScale;
        float scaleY = Math.max(0.001f, size.y) / base * extraScale;
        float scaleZ = Math.max(0.001f, size.z) / base * extraScale;

        Matrix4f matrix = new Matrix4f()
                .translation(
                        (center.x - pivot.x) * PIXEL_SCALE,
                        (center.y - pivot.y) * PIXEL_SCALE,
                        (center.z - pivot.z) * PIXEL_SCALE)
                .scale(scaleX, scaleY, scaleZ);

        if (head) {
            // In ItemDisplayTransform.HEAD, a PLAYER_HEAD spans X/Z: -0.25..0.25 (center 0),
            // Y: -0.5..0.0 (center -0.25). Shift +0.25 in Y to center it at (0, 0, 0).
            matrix.translate(0f, 0.25f, 0f);
        } else if (block) {
            // Block models span 0..1 with their corner at the display origin,
            // so shift them by half a block before scaling to center them.
            matrix.translate(-0.5f, -0.5f, -0.5f);
        }
        return matrix;
    }

    private static Quaternionf euler(Vector3f degrees) {
        return new Quaternionf().rotationZYX(
                (float) Math.toRadians(degrees.z),
                (float) Math.toRadians(degrees.y),
                (float) Math.toRadians(degrees.x));
    }

    public void spawn() {
        if (spawned) return;
        if (world == null || anchor == null) return;

        for (ModelBone bone : bones) {
            if (!bone.isRenderable()) continue;

            try {
                Display display = createDisplay(bone.getVisualType());
                if (display == null) continue;
                configureDisplay(display, bone);
                bone.setDisplay(display);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING,
                        "[CocoNPC] Could not spawn display for cube " + bone.getName(), exception);
            }
        }

        spawned = true;
    }

    private Display createDisplay(BoneVisual visual) {
        if (visual.isHead()) {
            ItemDisplay display = world.spawn(anchor, ItemDisplay.class,
                    CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    });
            display.setItemStack(SkinUtils.getHeadUrl(visual.skinUrl()));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            return display;
        }

        if (visual.isBlock()) {
            BlockDisplay display = world.spawn(anchor, BlockDisplay.class,
                    CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    });
            display.setBlock(visual.blockData().clone());
            return display;
        }

        if (visual.item() != null) {
            ItemDisplay display = world.spawn(anchor, ItemDisplay.class,
                    CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    });
            display.setItemStack(visual.item().clone());
            display.setItemDisplayTransform(visual.transform() != null
                    ? visual.transform()
                    : definition.getItemDisplayTransform());
            return display;
        }

        return null;
    }

    private void configureDisplay(Display display, ModelBone bone) {
        display.setPersistent(false);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setGravity(false);
        display.setBillboard(Display.Billboard.FIXED);
        display.setInterpolationDuration(definition.getInterpolationDuration());
        display.setInterpolationDelay(definition.getInterpolationDelay());
        display.setTeleportDuration(definition.getTeleportDuration());
        display.setViewRange(definition.getViewRange());
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        display.setVisibleByDefault(true);
        display.setGlowing(false);

        display.getPersistentDataContainer().set(keys.getInstance(),
                PersistentDataType.STRING, instanceId.toString());
        display.getPersistentDataContainer().set(keys.getDefinition(),
                PersistentDataType.STRING, definition.getId());
        display.getPersistentDataContainer().set(keys.getBone(),
                PersistentDataType.STRING, bone.getName());
    }

    public void apply(Location driverLocation, float yaw) {
        if (!spawned || driverLocation == null) return;

        World driverWorld = driverLocation.getWorld();
        if (driverWorld == null) return;

        if (world != driverWorld) {
            rebind(driverLocation);
        }
        if (!spawned) return;

        if (needsReanchor(driverLocation)) {
            reanchor(driverLocation);
        }

        rootMatrix.identity()
                .translate(
                        (float) (driverLocation.getX() - anchor.getX()),
                        (float) (driverLocation.getY() - anchor.getY()),
                        (float) (driverLocation.getZ() - anchor.getZ()))
                .rotateY((float) Math.toRadians(-yaw + definition.getYawOffset()))
                .scale((float) definition.getScale());

        for (ModelBone root : roots) {
            root.update(rootMatrix);
        }
    }

    /**
     * Recreates every display after the texture service has resolved a new
     * set of cube visuals. Must run on the owning region thread.
     */
    public void refreshVisuals() {
        if (!spawned) return;
        for (ModelBone bone : bones) {
            Display display = bone.getDisplay();
            if (display != null && display.isValid()) {
                display.remove();
            }
            bone.setDisplay(null);
        }
        spawned = false;
        spawn();
    }

    private boolean needsReanchor(Location location) {
        double dx = location.getX() - anchor.getX();
        double dy = location.getY() - anchor.getY();
        double dz = location.getZ() - anchor.getZ();
        if (dx * dx + dy * dy + dz * dz > reanchorDistanceSquared) {
            return true;
        }

        return (location.getBlockX() >> 4) != (anchor.getBlockX() >> 4)
                || (location.getBlockZ() >> 4) != (anchor.getBlockZ() >> 4);
    }

    private void reanchor(Location location) {
        anchor = cleanLocation(location);
        for (ModelBone bone : bones) {
            Display display = bone.getDisplay();
            if (display == null) continue;
            if (display.isValid()) {
                display.teleport(anchor);
            } else {
                bone.setDisplay(null);
            }
        }
    }

    private void rebind(Location location) {
        despawn();
        anchor = cleanLocation(location);
        world = anchor != null ? anchor.getWorld() : null;
        spawn();
    }

    public void despawn() {
        for (ModelBone bone : bones) {
            Display display = bone.getDisplay();
            if (display != null && display.isValid()) {
                display.remove();
            }
            bone.setDisplay(null);
        }
        spawned = false;
    }

    public void setFlash(boolean flashing) {
        for (ModelBone bone : bones) {
            Display display = bone.getDisplay();
            if (display == null || !display.isValid()) continue;
            display.setGlowing(flashing);
            display.setGlowColorOverride(flashing ? FLASH_COLOR : null);
        }
    }

    public void resetPose() {
        for (ModelBone bone : bones) {
            bone.resetPose();
        }
    }

    public RigAnimation createAnimation(BbAnimation animation, boolean additiveRotation) {
        return new RigAnimation(this, animation, additiveRotation);
    }

    ModelBone findBone(String uuid, String name) {
        if (uuid != null) {
            ModelBone bone = bonesByUuid.get(uuid);
            if (bone != null) return bone;
        }

        if (name == null) return null;
        ModelBone bone = bonesByName.get(name);
        if (bone != null) return bone;

        for (Map.Entry<String, ModelBone> entry : bonesByName.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Location cleanLocation(Location location) {
        if (location == null) return null;
        Location clean = location.clone();
        clean.setYaw(0f);
        clean.setPitch(0f);
        return clean;
    }

    public List<ModelBone> getRoots() {
        return Collections.unmodifiableList(roots);
    }

    public List<ModelBone> getBones() {
        return Collections.unmodifiableList(bones);
    }

    public Location getAnchor() {
        return anchor != null ? anchor.clone() : null;
    }

    public World getWorld() {
        return world;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public MobDefinition getDefinition() {
        return definition;
    }
}