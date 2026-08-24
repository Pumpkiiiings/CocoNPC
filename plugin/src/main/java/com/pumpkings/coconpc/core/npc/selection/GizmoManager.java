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
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GizmoManager {
    private final CocoNPC plugin;
    private final Map<UUID, EditorAxis> grabbedAxes = new ConcurrentHashMap<>();
    private final Map<UUID, GrabMode> grabbedModes = new ConcurrentHashMap<>();
    private final Map<UUID, GizmoMode> playerModes = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.List<VirtualItemDisplay>> playerGizmos = new ConcurrentHashMap<>();
    private static final float AXIS_LENGTH_ROT = 1.0f;
    private static final float AXIS_LENGTH_TRANS = 2.0f;
    private static final float THICKNESS = 0.05f;
    private static final float GRAB_THRESHOLD = 0.45f;

    public GizmoManager(CocoNPC plugin) {
        this.plugin = plugin;
        startGizmoTask();
    }

    private void startGizmoTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (plugin.getSelectionManager().inModify(player)) {
                        EditorTarget target = plugin.getSelectionManager().getEditorTarget(player);
                        NpcEntity npc = plugin.getSelectionManager().getSelected(player);
                        if (npc != null && target != null) {
                            updateGizmoFor(player, npc, target);
                        } else {
                            destroyGizmoFor(player);
                        }
                    } else {
                        destroyGizmoFor(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    private void updateGizmoFor(Player player, NpcEntity npc, EditorTarget target) {
        ItemPart part = getPrimaryPart(npc, target);
        if (part == null) return;

        Location npcLoc = npc.getLocation();
        if (npcLoc == null || npcLoc.getWorld() == null) return;

        Vector3f pos = new Vector3f(part.getTranslation());
        Location partLoc = npcLoc.clone().add(pos.x, pos.y, pos.z);

        GizmoMode mode = getMode(player);
        int requiredSize = mode == GizmoMode.TRANSLATION ? 3 : 36;
        
        java.util.List<VirtualItemDisplay> gizmos = playerGizmos.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayList<>());
        
        if (gizmos.size() != requiredSize) {
            for (VirtualItemDisplay v : gizmos) v.destroy(player);
            gizmos.clear();
            if (mode == GizmoMode.TRANSLATION) {
                gizmos.add(createDisplay(partLoc, Material.RED_CONCRETE));
                gizmos.add(createDisplay(partLoc, Material.LIME_CONCRETE));
                gizmos.add(createDisplay(partLoc, Material.BLUE_CONCRETE));
            } else {
                for (int i = 0; i < 12; i++) gizmos.add(createDisplay(partLoc, Material.RED_CONCRETE));
                for (int i = 0; i < 12; i++) gizmos.add(createDisplay(partLoc, Material.LIME_CONCRETE));
                for (int i = 0; i < 12; i++) gizmos.add(createDisplay(partLoc, Material.BLUE_CONCRETE));
            }
            for (VirtualItemDisplay v : gizmos) v.spawn(player);
        }

        for (VirtualItemDisplay v : gizmos) {
            v.setLocation(partLoc);
        }

        Quaternionf rot = part.getLeftRotation();
        EditorAxis grabbed = getGrabbedAxis(player);

        if (mode == GizmoMode.TRANSLATION) {
            float transScaleL = AXIS_LENGTH_TRANS * 2.0f; // Multiplied by 2.0 to counteract the 0.5x FIXED scale and visually reach 1.0 blocks
            float transScaleT = 0.15f; // Thicker lines
            Quaternionf noRot = new Quaternionf();
            updateDisplayTransform(gizmos.get(0), noRot, new Vector3f(transScaleL, transScaleT, transScaleT), new Vector3f(AXIS_LENGTH_TRANS / 2f, 0, 0), grabbed == EditorAxis.X, Material.RED_CONCRETE);
            updateDisplayTransform(gizmos.get(1), noRot, new Vector3f(transScaleT, transScaleL, transScaleT), new Vector3f(0, AXIS_LENGTH_TRANS / 2f, 0), grabbed == EditorAxis.Y, Material.LIME_CONCRETE);
            updateDisplayTransform(gizmos.get(2), noRot, new Vector3f(transScaleT, transScaleT, transScaleL), new Vector3f(0, 0, AXIS_LENGTH_TRANS / 2f), grabbed == EditorAxis.Z, Material.BLUE_CONCRETE);
        } else {
            float radius = AXIS_LENGTH_ROT;
            for(int i = 0; i < 12; i++) {
                updateRingSegmentTransform(gizmos.get(i), rot, i, 12, radius, EditorAxis.X, grabbed == EditorAxis.X, Material.RED_CONCRETE);
            }
            for(int i = 0; i < 12; i++) {
                updateRingSegmentTransform(gizmos.get(12 + i), rot, i, 12, radius, EditorAxis.Y, grabbed == EditorAxis.Y, Material.LIME_CONCRETE);
            }
            for(int i = 0; i < 12; i++) {
                updateRingSegmentTransform(gizmos.get(24 + i), rot, i, 12, radius, EditorAxis.Z, grabbed == EditorAxis.Z, Material.BLUE_CONCRETE);
            }
        }

        for (VirtualItemDisplay v : gizmos) {
            v.updateMetadata(player);
        }
    }

    private VirtualItemDisplay createDisplay(Location loc, Material material) {
        VirtualItemDisplay display = new VirtualItemDisplay(loc, new ItemStack(material));
        display.setTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        return display;
    }

    private void updateDisplayTransform(VirtualItemDisplay display, Quaternionf baseRot, Vector3f scale, Vector3f offset, boolean glowing, Material mat) {
        Vector3f rotatedOffset = new Vector3f(offset).rotate(baseRot);
        display.setTransformation(rotatedOffset, baseRot, scale, new Quaternionf());
        
        if (glowing) {
            display.setItem(new ItemStack(Material.WHITE_CONCRETE));
        } else {
            display.setItem(new ItemStack(mat));
        }
    }

    private void updateRingSegmentTransform(VirtualItemDisplay display, Quaternionf baseRot, int i, int total, float radius, EditorAxis axis, boolean glowing, Material mat) {
        float angle = (float) (i * 2.0 * Math.PI / total);
        // Multiplied by 2.15f because FIXED ItemDisplay has an inherent 0.5x scale factor for blocks
        float segmentLength = (float) (2.0 * Math.PI * radius / total) * 2.15f;
        
        Vector3f localPos = new Vector3f();
        Vector3f tangent = new Vector3f();
        
        if (axis == EditorAxis.X) {
            localPos.set(0, (float)Math.cos(angle) * radius, (float)Math.sin(angle) * radius);
            tangent.set(0, (float)-Math.sin(angle), (float)Math.cos(angle)).normalize();
        } else if (axis == EditorAxis.Y) {
            localPos.set((float)Math.cos(angle) * radius, 0, (float)Math.sin(angle) * radius);
            tangent.set((float)-Math.sin(angle), 0, (float)Math.cos(angle)).normalize();
        } else if (axis == EditorAxis.Z) {
            localPos.set((float)Math.cos(angle) * radius, (float)Math.sin(angle) * radius, 0);
            tangent.set((float)-Math.sin(angle), (float)Math.cos(angle), 0).normalize();
        }

        Quaternionf localRot = new Quaternionf().rotationTo(new Vector3f(1,0,0), tangent);
        
        Vector3f rotatedPos = new Vector3f(localPos).rotate(baseRot);
        Quaternionf finalRot = new Quaternionf(baseRot).mul(localRot);
        Vector3f scale = new Vector3f(segmentLength, THICKNESS, THICKNESS);

        display.setTransformation(rotatedPos, finalRot, scale, new Quaternionf());
        
        if (glowing) {
            display.setItem(new ItemStack(Material.WHITE_CONCRETE));
        } else {
            display.setItem(new ItemStack(mat));
        }
    }

    public void destroyGizmoFor(Player player) {
        java.util.List<VirtualItemDisplay> gizmos = playerGizmos.remove(player.getUniqueId());
        if (gizmos != null) {
            for (VirtualItemDisplay v : gizmos) {
                v.destroy(player);
            }
        }
        grabbedAxes.remove(player.getUniqueId());
        grabbedModes.remove(player.getUniqueId());
    }

    public void clearAll() {
        for (UUID uuid : playerGizmos.keySet()) {
            Player p = org.bukkit.Bukkit.getPlayer(uuid);
            if (p != null) destroyGizmoFor(p);
        }
    }

    public EditorAxis getLookedAxis(Player player, NpcEntity npc, EditorTarget target) {
        ItemPart part = getPrimaryPart(npc, target);
        if (part == null || npc.getLocation() == null) return EditorAxis.NONE;

        Location eyeLoc = player.getEyeLocation();
        Vector3f rayStart = new Vector3f((float) eyeLoc.getX(), (float) eyeLoc.getY(), (float) eyeLoc.getZ());
        org.bukkit.util.Vector eyeDir = eyeLoc.getDirection();
        Vector3f rayDir = new Vector3f((float) eyeDir.getX(), (float) eyeDir.getY(), (float) eyeDir.getZ()).normalize();

        Vector3f pos = new Vector3f(part.getTranslation());
        Location partLoc = npc.getLocation().clone().add(pos.x, pos.y, pos.z);
        Vector3f segStart = new Vector3f((float) partLoc.getX(), (float) partLoc.getY(), (float) partLoc.getZ());

        GizmoMode mode = getMode(player);
        Quaternionf hitRot = mode == GizmoMode.TRANSLATION ? new Quaternionf() : part.getLeftRotation();
        Vector3f xAxis = new Vector3f(1, 0, 0).rotate(hitRot);
        Vector3f yAxis = new Vector3f(0, 1, 0).rotate(hitRot);
        Vector3f zAxis = new Vector3f(0, 0, 1).rotate(hitRot);

        double minDistance = GRAB_THRESHOLD;
        EditorAxis looked = EditorAxis.NONE;
        
        if (mode == GizmoMode.TRANSLATION) {
            Vector3f xEnd = new Vector3f(segStart).add(new Vector3f(xAxis).mul(AXIS_LENGTH_TRANS));
            Vector3f yEnd = new Vector3f(segStart).add(new Vector3f(yAxis).mul(AXIS_LENGTH_TRANS));
            Vector3f zEnd = new Vector3f(segStart).add(new Vector3f(zAxis).mul(AXIS_LENGTH_TRANS));

            double dX = GizmoMath.distanceRayToSegment(rayStart, rayDir, segStart, xEnd);
            double dY = GizmoMath.distanceRayToSegment(rayStart, rayDir, segStart, yEnd);
            double dZ = GizmoMath.distanceRayToSegment(rayStart, rayDir, segStart, zEnd);

            if (dX < minDistance) { minDistance = dX; looked = EditorAxis.X; }
            if (dY < minDistance) { minDistance = dY; looked = EditorAxis.Y; }
            if (dZ < minDistance) { minDistance = dZ; looked = EditorAxis.Z; }
        } else {
            double dX = GizmoMath.distanceRayToRing(rayStart, rayDir, segStart, xAxis, AXIS_LENGTH_ROT, 12);
            double dY = GizmoMath.distanceRayToRing(rayStart, rayDir, segStart, yAxis, AXIS_LENGTH_ROT, 12);
            double dZ = GizmoMath.distanceRayToRing(rayStart, rayDir, segStart, zAxis, AXIS_LENGTH_ROT, 12);

            if (dX < minDistance) { minDistance = dX; looked = EditorAxis.X; }
            if (dY < minDistance) { minDistance = dY; looked = EditorAxis.Y; }
            if (dZ < minDistance) { minDistance = dZ; looked = EditorAxis.Z; }
        }

        return looked;
    }

    public GizmoMode getMode(Player player) {
        return playerModes.getOrDefault(player.getUniqueId(), GizmoMode.ROTATION);
    }

    public void toggleMode(Player player) {
        GizmoMode current = getMode(player);
        GizmoMode next = current == GizmoMode.ROTATION ? GizmoMode.TRANSLATION : GizmoMode.ROTATION;
        playerModes.put(player.getUniqueId(), next);
        destroyGizmoFor(player);
        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Modo cambiado a: " + next.name()));
    }

    public void grabAxis(Player player, EditorAxis axis, GrabMode mode) {
        if (axis == EditorAxis.NONE) {
            grabbedAxes.remove(player.getUniqueId());
            grabbedModes.remove(player.getUniqueId());
        } else {
            grabbedAxes.put(player.getUniqueId(), axis);
            grabbedModes.put(player.getUniqueId(), mode);
        }
    }

    public EditorAxis getGrabbedAxis(Player player) {
        return grabbedAxes.getOrDefault(player.getUniqueId(), EditorAxis.NONE);
    }

    public GrabMode getGrabbedMode(Player player) {
        return grabbedModes.getOrDefault(player.getUniqueId(), GrabMode.NONE);
    }

    public void releaseAxis(Player player) {
        grabbedAxes.remove(player.getUniqueId());
        grabbedModes.remove(player.getUniqueId());
    }

    private ItemPart getPrimaryPart(NpcEntity npc, EditorTarget type) {
        String key = switch (type) {
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
        return key != null ? npc.getPart(key) : null;
    }
}
