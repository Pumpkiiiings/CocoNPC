package com.pumpkings.coconpc.core.npc.selection;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.menu.EditorTarget;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionManager {
    private final CocoNPC plugin;

    private final Map<UUID, NpcEntity> selected = new ConcurrentHashMap<>();
    private final Map<UUID, EditorTarget> modify = new ConcurrentHashMap<>();
    private final Map<UUID, EditorTarget> manualRotate = new ConcurrentHashMap<>();
    private final Map<UUID, NpcEntity> manualHologramAdd = new ConcurrentHashMap<>();
    private final Map<UUID, NpcEntity> manualHologramBg = new ConcurrentHashMap<>();
    private final Map<UUID, EditMode> editModes = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> pendingSetKey = ConcurrentHashMap.newKeySet();

    public enum EditMode {
        ROTATION_PRIMARY,
        ROTATION_ROLL,
        ROTATION_JOINT_PRIMARY,
        ROTATION_JOINT_ROLL,
        POSITION_VERTICAL,
        POSITION_HORIZONTAL
    }

    public SelectionManager(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public void select(Player player, NpcEntity npc) {
        selected.put(player.getUniqueId(), npc);
        Message.NPC_SELECTED.send(plugin, player, "{id}", npc.getId());
    }

    public NpcEntity getSelected(Player player) {
        return player == null ? null : selected.get(player.getUniqueId());
    }

    public void selectNpc(Player player) {
        NpcEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            Location npcLoc = npc.getLocation();
            if (npcLoc == null || npcLoc.getWorld() == null) continue;
            if (!npcLoc.getWorld().equals(player.getWorld())) continue;

            double distance = npcLoc.distanceSquared(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = npc;
            }
        }

        if (nearest == null) {
            Message.NO_NPC_NEARBY.send(plugin, player);
        } else {
            select(player, nearest);
        }
    }

    public void deselect(Player player) {
        UUID id = player.getUniqueId();
        modify.remove(id);
        editModes.remove(id);
        selected.remove(id);
        Message.NPC_DESELECTED.send(plugin, player);
    }

    public void clear(Player player) {
        UUID id = player.getUniqueId();
        selected.remove(id);
        modify.remove(id);
        manualRotate.remove(id);
        manualHologramAdd.remove(id);
        manualHologramBg.remove(id);
        editModes.remove(id);
        pendingSetKey.remove(id);
    }

    public boolean inModify(Player player) {
        return player != null && modify.containsKey(player.getUniqueId());
    }

    public EditorTarget getEditorTarget(Player player) {
        return player == null ? null : modify.get(player.getUniqueId());
    }

    public void beginModify(Player player, EditorTarget type) {
        modify.put(player.getUniqueId(), type);
        editModes.put(player.getUniqueId(), EditMode.ROTATION_PRIMARY);
    }

    public void beginManualRotate(Player player, EditorTarget type) {
        manualRotate.put(player.getUniqueId(), type);
    }

    public boolean hasPendingRotate(Player player) {
        return manualRotate.containsKey(player.getUniqueId());
    }

    public EditorTarget consumeManualRotate(Player player) {
        return manualRotate.remove(player.getUniqueId());
    }

    public void setPendingSetKey(Player player) {
        if (player != null) pendingSetKey.add(player.getUniqueId());
    }

    public boolean hasPendingSetKey(Player player) {
        return player != null && pendingSetKey.contains(player.getUniqueId());
    }

    public boolean consumeSetKey(Player player) {
        return player != null && pendingSetKey.remove(player.getUniqueId());
    }

    public void beginHologramAdd(Player player, NpcEntity npc) {
        manualHologramAdd.put(player.getUniqueId(), npc);
    }

    public boolean hasPendingHologramAdd(Player player) {
        return manualHologramAdd.containsKey(player.getUniqueId());
    }

    public NpcEntity consumeHologramAdd(Player player) {
        return manualHologramAdd.remove(player.getUniqueId());
    }

    public void beginHologramBg(Player player, NpcEntity npc) {
        manualHologramBg.put(player.getUniqueId(), npc);
    }

    public boolean hasPendingHologramBg(Player player) {
        return manualHologramBg.containsKey(player.getUniqueId());
    }

    public NpcEntity consumeHologramBg(Player player) {
        return manualHologramBg.remove(player.getUniqueId());
    }

    public void scroll(Player player, boolean right) {
        boolean isSneaking = player.isSneaking();
        EditorTarget type = getEditorTarget(player);
        NpcEntity npc = getSelected(player);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4f, right ? 1.4f : 0.8f);

        if (type == EditorTarget.HOLOGRAM && npc != null && npc.hasHologram()) {
            npc.moveHologram(right ? -0.15 : 0.15);
        } else if (npc != null) {
            EditMode mode = editModes.getOrDefault(player.getUniqueId(), EditMode.ROTATION_PRIMARY);
            float angleStep = right ? -2.0F : 2.0F;
            float posStep = right ? -0.04F : 0.04F;

            switch (mode) {
                case ROTATION_PRIMARY:
                    if (isSneaking) {
                        npc.addRotation(type, angleStep, 0, 0);
                    } else {
                        npc.addRotation(type, 0, angleStep, 0);
                    }
                    break;
                case ROTATION_ROLL:
                    npc.addRotation(type, 0, 0, angleStep);
                    break;
                case ROTATION_JOINT_PRIMARY:
                    if (isSneaking) {
                        npc.addJointRotation(type, angleStep, 0, 0);
                    } else {
                        npc.addJointRotation(type, 0, angleStep, 0);
                    }
                    break;
                case ROTATION_JOINT_ROLL:
                    npc.addJointRotation(type, 0, 0, angleStep);
                    break;
                case POSITION_VERTICAL:
                    if (isSneaking) {
                        npc.addOffset(type, 0, 0, posStep);
                    } else {
                        npc.addOffset(type, 0, posStep, 0);
                    }
                    break;
                case POSITION_HORIZONTAL:
                    npc.addOffset(type, posStep, 0, 0);
                    break;
            }
        }
    }

    public void cycleMode(Player player) {
        if (!inModify(player)) return;
        EditorTarget type = getEditorTarget(player);
        if (type == EditorTarget.HOLOGRAM) return;

        EditMode current = editModes.getOrDefault(player.getUniqueId(), EditMode.ROTATION_PRIMARY);
        EditMode next;
        switch (current) {
            case ROTATION_PRIMARY:
                next = EditMode.ROTATION_ROLL;
                Message.MODE_ROTATION_ROLL.send(plugin, player);
                break;
            case ROTATION_ROLL:
                next = EditMode.ROTATION_JOINT_PRIMARY;
                Message.MODE_ROTATION_JOINT_PRIMARY.send(plugin, player);
                break;
            case ROTATION_JOINT_PRIMARY:
                next = EditMode.ROTATION_JOINT_ROLL;
                Message.MODE_ROTATION_JOINT_ROLL.send(plugin, player);
                break;
            case ROTATION_JOINT_ROLL:
                next = EditMode.POSITION_VERTICAL;
                Message.MODE_POSITION_VERTICAL.send(plugin, player);
                break;
            case POSITION_VERTICAL:
                next = EditMode.POSITION_HORIZONTAL;
                Message.MODE_POSITION_HORIZONTAL.send(plugin, player);
                break;
            case POSITION_HORIZONTAL:
            default:
                next = EditMode.ROTATION_PRIMARY;
                Message.MODE_ROTATION_PRIMARY.send(plugin, player);
                break;
        }
        editModes.put(player.getUniqueId(), next);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 1.2F);
    }

    public void toggleLimb(Player player, EditorTarget type) {
        NpcEntity npc = getSelected(player);
        if (npc != null) {
            boolean visible = npc.toggleLimb(type);
            String partName = WordUtils.capitalizeFully(type.name());
            if (visible) {
                Message.PART_VISIBLE.send(plugin, player, "{part}", partName);
            } else {
                Message.PART_INVISIBLE.send(plugin, player, "{part}", partName);
            }
        }
    }
}


