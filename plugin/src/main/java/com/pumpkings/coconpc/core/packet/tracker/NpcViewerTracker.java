package com.pumpkings.coconpc.core.packet.tracker;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcViewerTracker implements Listener {
    private static final double RECHECK_DISTANCE_SQUARED = 16.0;

    private final CocoNPC plugin;
    private int viewDistance;
    private int viewDistanceSquared;

    private final Map<UUID, Location> lastChecked = new ConcurrentHashMap<>();

    public NpcViewerTracker(CocoNPC plugin) {
        this.plugin = plugin;
        setViewDistanceQuietly(plugin.getConfigManager().getViewDistance());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int distance) {
        setViewDistanceQuietly(distance);
        plugin.getConfigManager().setViewDistance(this.viewDistance);
        refreshAll();
    }

    private void setViewDistanceQuietly(int distance) {
        this.viewDistance = Math.max(8, distance);
        this.viewDistanceSquared = this.viewDistance * this.viewDistance;
    }

    public void checkVisibility(Player player) {
        if (player == null || !player.isOnline()) return;
        Location playerLoc = player.getLocation();
        lastChecked.put(player.getUniqueId(), playerLoc.clone());

        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            applyVisibility(npc, player, playerLoc);
        }
    }

    public void refresh(NpcEntity npc) {
        if (npc == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyVisibility(npc, player, player.getLocation());
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkVisibility(player);
        }
    }

    private void applyVisibility(NpcEntity npc, Player player, Location playerLoc) {
        Location npcLoc = npc.getLocation();
        if (npcLoc == null || npcLoc.getWorld() == null) return;

        if (npcLoc.getWorld().equals(playerLoc.getWorld())
                && npcLoc.distanceSquared(playerLoc) <= viewDistanceSquared) {
            npc.showTo(player);
        } else {
            npc.hideFrom(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin,
                task -> checkVisibility(event.getPlayer()), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastChecked.remove(player.getUniqueId());
        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            npc.hideFrom(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        checkVisibility(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> checkVisibility(event.getPlayer()));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location previous = lastChecked.get(event.getPlayer().getUniqueId());

        if (previous != null
                && previous.getWorld() == to.getWorld()
                && previous.distanceSquared(to) < RECHECK_DISTANCE_SQUARED) {
            return;
        }
        checkVisibility(event.getPlayer());
    }
}



