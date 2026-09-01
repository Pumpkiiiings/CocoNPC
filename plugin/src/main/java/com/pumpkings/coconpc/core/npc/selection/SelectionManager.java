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
    private final java.util.Set<UUID> pendingSetKey = ConcurrentHashMap.newKeySet();

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
        NpcEntity npc = selected.get(id);
        if (npc != null) {
            plugin.getConfigManager().getRepository().saveParts(npc);
            plugin.getConfigManager().getRepository().saveLocationAndSkin(npc);
        }
        if (plugin.getGizmoManager() != null) {
            plugin.getGizmoManager().destroyGizmoFor(player);
        }
        modify.remove(id);

        selected.remove(id);
        Message.NPC_DESELECTED.send(plugin, player);
    }

    public void clear(Player player) {
        UUID id = player.getUniqueId();
        NpcEntity npc = selected.get(id);
        if (npc != null) {
            plugin.getConfigManager().getRepository().saveParts(npc);
            plugin.getConfigManager().getRepository().saveLocationAndSkin(npc);
        }
        if (plugin.getGizmoManager() != null) {
            plugin.getGizmoManager().destroyGizmoFor(player);
        }
        selected.remove(id);
        modify.remove(id);
        manualRotate.remove(id);
        manualHologramAdd.remove(id);
        manualHologramBg.remove(id);

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
        player.getInventory().setHeldItemSlot(4);
        NpcEntity npc = getSelected(player);
        if (npc != null && plugin.getGizmoManager() != null) {
            plugin.getGizmoManager().activate(player, npc, type);
        }
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
            plugin.getConfigManager().getRepository().saveLocationAndSkin(npc);
        } else if (npc != null) {
            float posStep = right ? -0.04F : 0.04F;
            if (isSneaking) {
                npc.addOffset(type, posStep, 0, 0);
            } else {
                npc.addOffset(type, 0, posStep, 0);
            }
        }
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


