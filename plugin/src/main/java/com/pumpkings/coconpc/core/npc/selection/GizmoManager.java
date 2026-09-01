package com.pumpkings.coconpc.core.npc.selection;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.npc.part.ItemPart;
import com.pumpkings.coconpc.core.packet.entity.VirtualItemDisplay;
import com.pumpkings.coconpc.menu.EditorTarget;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GizmoManager {
    private static final int RING_SEGMENTS = 16;
    private static final float AXIS_LENGTH_ROT = 1.0f;
    private static final float AXIS_LENGTH_TRANS = 2.0f;
    private static final float THICKNESS = 0.05f;
    private static final float GRAB_THRESHOLD = 0.24f;
    private static final ItemStack RED = new ItemStack(Material.RED_CONCRETE);
    private static final ItemStack GREEN = new ItemStack(Material.LIME_CONCRETE);
    private static final ItemStack BLUE = new ItemStack(Material.BLUE_CONCRETE);
    private static final ItemStack ACTIVE = new ItemStack(Material.WHITE_CONCRETE);

    private final CocoNPC plugin;
    private final Map<UUID, EditorAxis> grabbedAxes = new ConcurrentHashMap<>();
    private final Map<UUID, GrabMode> grabbedModes = new ConcurrentHashMap<>();
    private final Map<UUID, GizmoMode> playerModes = new ConcurrentHashMap<>();
    private final Map<UUID, List<VirtualItemDisplay>> playerGizmos = new ConcurrentHashMap<>();

    public GizmoManager(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public void activate(Player player, NpcEntity npc, EditorTarget target) {
        if (player != null && npc != null && target != null) updateGizmoFor(player, npc, target);
    }

    public void refresh(Player player) {
        NpcEntity npc = plugin.getSelectionManager().getSelected(player);
        EditorTarget target = plugin.getSelectionManager().getEditorTarget(player);
        if (npc == null || target == null) {
            destroyGizmoFor(player);
        } else {
            updateGizmoFor(player, npc, target);
        }
    }

    private void updateGizmoFor(Player player, NpcEntity npc, EditorTarget target) {
        Location center = getCenter(npc, target);
        if (center == null || center.getWorld() == null) {
            destroyGizmoFor(player);
            return;
        }

        GizmoMode mode = getMode(player);
        int requiredSize = mode == GizmoMode.TRANSLATION ? 3 : RING_SEGMENTS * 3;
        List<VirtualItemDisplay> gizmos = playerGizmos.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());
        if (gizmos.size() != requiredSize) {
            destroyVisuals(player);
            gizmos = new ArrayList<>(requiredSize);
            for (int i = 0; i < requiredSize; i++) {
                ItemStack item = i < requiredSize / 3 ? RED : i < (requiredSize * 2) / 3 ? GREEN : BLUE;
                VirtualItemDisplay display = createDisplay(center, item);
                gizmos.add(display);
                display.addViewer(player);
            }
            playerGizmos.put(player.getUniqueId(), gizmos);
        } else {
            for (VirtualItemDisplay display : gizmos) display.teleport(center);
        }

        Quaternionf rotation = getLocalTargetRotation(npc, target);
        EditorAxis grabbed = getGrabbedAxis(player);
        if (mode == GizmoMode.TRANSLATION) {
            float length = AXIS_LENGTH_TRANS * 2.0f;
            float width = 0.15f;
            updateDisplayTransform(gizmos.get(0), rotation, new Vector3f(length, width, width),
                    new Vector3f(AXIS_LENGTH_TRANS / 2f, 0, 0), grabbed == EditorAxis.X, RED);
            updateDisplayTransform(gizmos.get(1), rotation, new Vector3f(width, length, width),
                    new Vector3f(0, AXIS_LENGTH_TRANS / 2f, 0), grabbed == EditorAxis.Y, GREEN);
            updateDisplayTransform(gizmos.get(2), rotation, new Vector3f(width, width, length),
                    new Vector3f(0, 0, AXIS_LENGTH_TRANS / 2f), grabbed == EditorAxis.Z, BLUE);
        } else {
            for (int i = 0; i < RING_SEGMENTS; i++) {
                updateRingSegmentTransform(gizmos.get(i), rotation, i, EditorAxis.X, grabbed == EditorAxis.X, RED);
                updateRingSegmentTransform(gizmos.get(RING_SEGMENTS + i), rotation, i, EditorAxis.Y, grabbed == EditorAxis.Y, GREEN);
                updateRingSegmentTransform(gizmos.get(RING_SEGMENTS * 2 + i), rotation, i, EditorAxis.Z, grabbed == EditorAxis.Z, BLUE);
            }
        }
        for (VirtualItemDisplay display : gizmos) display.updateMetadata(player);
    }

    private VirtualItemDisplay createDisplay(Location location, ItemStack item) {
        VirtualItemDisplay display = new VirtualItemDisplay(location, item);
        display.setTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        return display;
    }

    private void updateDisplayTransform(VirtualItemDisplay display, Quaternionf rotation, Vector3f scale,
                                        Vector3f offset, boolean active, ItemStack baseItem) {
        display.setTransformation(new Vector3f(offset).rotate(rotation), rotation, scale, new Quaternionf());
        display.setItem(active ? ACTIVE : baseItem);
    }

    private void updateRingSegmentTransform(VirtualItemDisplay display, Quaternionf baseRotation, int index,
                                            EditorAxis axis, boolean active, ItemStack baseItem) {
        float angle = (float) (index * 2.0 * Math.PI / RING_SEGMENTS);
        float segmentLength = (float) (2.0 * Math.PI * AXIS_LENGTH_ROT / RING_SEGMENTS) * 2.15f;
        Vector3f position = new Vector3f();
        Vector3f tangent = new Vector3f();
        switch (axis) {
            case X -> {
                position.set(0, (float) Math.cos(angle), (float) Math.sin(angle));
                tangent.set(0, (float) -Math.sin(angle), (float) Math.cos(angle));
            }
            case Y -> {
                position.set((float) Math.cos(angle), 0, (float) Math.sin(angle));
                tangent.set((float) -Math.sin(angle), 0, (float) Math.cos(angle));
            }
            case Z -> {
                position.set((float) Math.cos(angle), (float) Math.sin(angle), 0);
                tangent.set((float) -Math.sin(angle), (float) Math.cos(angle), 0);
            }
            default -> throw new IllegalArgumentException("A ring requires a concrete axis");
        }
        Quaternionf localRotation = new Quaternionf().rotationTo(new Vector3f(1, 0, 0), tangent.normalize());
        display.setTransformation(
                position.mul(AXIS_LENGTH_ROT).rotate(baseRotation),
                new Quaternionf(baseRotation).mul(localRotation),
                new Vector3f(segmentLength, THICKNESS, THICKNESS),
                new Quaternionf());
        display.setItem(active ? ACTIVE : baseItem);
    }

    public EditorAxis getLookedAxis(Player player, NpcEntity npc, EditorTarget target) {
        Location center = getCenter(npc, target);
        if (center == null) return EditorAxis.NONE;
        Location eye = player.getEyeLocation();
        Vector3f rayStart = new Vector3f((float) eye.getX(), (float) eye.getY(), (float) eye.getZ());
        org.bukkit.util.Vector eyeDirection = eye.getDirection();
        Vector3f rayDirection = new Vector3f((float) eyeDirection.getX(), (float) eyeDirection.getY(),
                (float) eyeDirection.getZ()).normalize();
        Vector3f origin = new Vector3f((float) center.getX(), (float) center.getY(), (float) center.getZ());
        Quaternionf rotation = getWorldTargetRotation(npc, target);
        Vector3f xAxis = new Vector3f(1, 0, 0).rotate(rotation);
        Vector3f yAxis = new Vector3f(0, 1, 0).rotate(rotation);
        Vector3f zAxis = new Vector3f(0, 0, 1).rotate(rotation);

        double xDistance;
        double yDistance;
        double zDistance;
        if (getMode(player) == GizmoMode.TRANSLATION) {
            xDistance = distanceToAxis(rayStart, rayDirection, origin, xAxis, AXIS_LENGTH_TRANS);
            yDistance = distanceToAxis(rayStart, rayDirection, origin, yAxis, AXIS_LENGTH_TRANS);
            zDistance = distanceToAxis(rayStart, rayDirection, origin, zAxis, AXIS_LENGTH_TRANS);
        } else {
            xDistance = GizmoMath.distanceRayToRing(rayStart, rayDirection, origin, xAxis, AXIS_LENGTH_ROT, RING_SEGMENTS);
            yDistance = GizmoMath.distanceRayToRing(rayStart, rayDirection, origin, yAxis, AXIS_LENGTH_ROT, RING_SEGMENTS);
            zDistance = GizmoMath.distanceRayToRing(rayStart, rayDirection, origin, zAxis, AXIS_LENGTH_ROT, RING_SEGMENTS);
        }
        double nearest = GRAB_THRESHOLD;
        EditorAxis result = EditorAxis.NONE;
        if (xDistance < nearest) { nearest = xDistance; result = EditorAxis.X; }
        if (yDistance < nearest) { nearest = yDistance; result = EditorAxis.Y; }
        if (zDistance < nearest) result = EditorAxis.Z;
        return result;
    }

    private double distanceToAxis(Vector3f rayStart, Vector3f rayDirection, Vector3f origin,
                                  Vector3f axis, float length) {
        return GizmoMath.distanceRayToSegment(rayStart, rayDirection, origin,
                new Vector3f(origin).add(new Vector3f(axis).mul(length)));
    }

    public Vector3f getAxisDirection(NpcEntity npc, EditorTarget target, EditorAxis axis) {
        Vector3f direction = switch (axis) {
            case X -> new Vector3f(1, 0, 0);
            case Y -> new Vector3f(0, 1, 0);
            case Z -> new Vector3f(0, 0, 1);
            default -> new Vector3f();
        };
        Quaternionf rotation;
        if (target == EditorTarget.GLOBAL) {
            // Global translation changes the Bukkit location, so its delta is world-space.
            rotation = getWorldTargetRotation(npc, target);
        } else {
            // Part offsets are persisted in their parent space. The parent/global transform
            // is applied later by NpcEntity, so only the part's local rotation belongs here.
            ItemPart part = getPrimaryPart(npc, target);
            rotation = part == null ? new Quaternionf() : part.getLocalRotation();
        }
        return direction.rotate(rotation);
    }

    private Location getCenter(NpcEntity npc, EditorTarget target) {
        Location location = npc.getLocation();
        if (location == null) return null;
        if (target == EditorTarget.GLOBAL) return location.clone().add(0, 0.9f * npc.getSize(), 0);
        ItemPart part = getPrimaryPart(npc, target);
        if (part == null) return null;
        Vector3f position = new Vector3f(part.getTranslation()).rotate(getBaseYaw(npc));
        return location.clone().add(position.x, position.y, position.z);
    }

    private Quaternionf getLocalTargetRotation(NpcEntity npc, EditorTarget target) {
        if (target == EditorTarget.GLOBAL) return npc.getGlobalRotation();
        ItemPart part = getPrimaryPart(npc, target);
        return part == null ? new Quaternionf() : new Quaternionf(part.getLeftRotation());
    }

    private Quaternionf getWorldTargetRotation(NpcEntity npc, EditorTarget target) {
        return getBaseYaw(npc).mul(getLocalTargetRotation(npc, target));
    }

    private Quaternionf getBaseYaw(NpcEntity npc) {
        Location location = npc.getLocation();
        float yaw = location == null ? 0f : location.getYaw();
        return new Quaternionf().rotationY((float) Math.toRadians(-yaw));
    }

    private ItemPart getPrimaryPart(NpcEntity npc, EditorTarget target) {
        String key = switch (target) {
            case HEAD -> "head";
            case BODY -> "torsoUpper";
            case RIGHT_ARM -> "rightArmUpper";
            case LEFT_ARM -> "leftArmUpper";
            case RIGHT_LEG -> "rightLegUpper";
            case LEFT_LEG -> "leftLegUpper";
            case RIGHT_ITEM -> "right_item";
            case LEFT_ITEM -> "left_item";
            default -> null;
        };
        return key == null ? null : npc.getPart(key);
    }

    public GizmoMode getMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), GizmoMode.ROTATION);
    }

    public void toggleMode(Player player) {
        GizmoMode next = getMode(player) == GizmoMode.ROTATION ? GizmoMode.TRANSLATION : GizmoMode.ROTATION;
        playerModes.put(player.getUniqueId(), next);
        clearGrab(player);
        destroyVisuals(player);
        refresh(player);
        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<green>Modo cambiado a: " + next.name()));
    }

    public void grabAxis(Player player, EditorAxis axis, GrabMode mode) {
        if (axis == EditorAxis.NONE) clearGrab(player);
        else {
            grabbedAxes.put(player.getUniqueId(), axis);
            grabbedModes.put(player.getUniqueId(), mode);
        }
        refresh(player);
    }

    public EditorAxis getGrabbedAxis(Player player) {
        return grabbedAxes.getOrDefault(player.getUniqueId(), EditorAxis.NONE);
    }

    public GrabMode getGrabbedMode(Player player) {
        return grabbedModes.getOrDefault(player.getUniqueId(), GrabMode.NONE);
    }

    public void releaseAxis(Player player) {
        clearGrab(player);
        if (plugin.getSelectionManager().inModify(player)) refresh(player);
    }

    private void clearGrab(Player player) {
        grabbedAxes.remove(player.getUniqueId());
        grabbedModes.remove(player.getUniqueId());
    }

    private void destroyVisuals(Player player) {
        List<VirtualItemDisplay> gizmos = playerGizmos.remove(player.getUniqueId());
        if (gizmos != null) for (VirtualItemDisplay display : gizmos) display.removeViewer(player);
    }

    public void destroyGizmoFor(Player player) {
        destroyVisuals(player);
        UUID id = player.getUniqueId();
        grabbedAxes.remove(id);
        grabbedModes.remove(id);
        playerModes.remove(id);
    }

    public void clearAll() {
        for (UUID id : List.copyOf(playerGizmos.keySet())) {
            Player player = org.bukkit.Bukkit.getPlayer(id);
            if (player != null) destroyGizmoFor(player);
        }
        playerGizmos.clear();
        grabbedAxes.clear();
        grabbedModes.clear();
        playerModes.clear();
    }
}
