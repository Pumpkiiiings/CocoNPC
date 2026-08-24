package com.pumpkings.coconpc.core.packet.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class VirtualEntity {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(100_000_000);

    protected final int entityId;
    protected final UUID uuid;
    protected Location location;
    protected final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    public VirtualEntity(Location location) {
        this.entityId = ID_GENERATOR.incrementAndGet();
        this.uuid = UUID.randomUUID();
        this.location = location != null ? location.clone() : null;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (location != null) {
            this.location = location.clone();
        }
    }

    public Set<UUID> getViewers() {
        return viewers;
    }

    public void addViewer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (viewers.add(player.getUniqueId())) {
            spawn(player);
        }
    }

    public void removeViewer(Player player) {
        if (player == null) return;
        if (viewers.remove(player.getUniqueId())) {
            destroy(player);
        }
    }

    public boolean isViewer(Player player) {
        return player != null && viewers.contains(player.getUniqueId());
    }

    public abstract void spawn(Player player);

    public abstract void updateMetadata(Player player);

    public void destroy(Player player) {
        if (player == null || !player.isOnline()) return;
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    public void destroyForAll() {
        for (UUID viewerId : viewers) {
            Player player = org.bukkit.Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                destroy(player);
            }
        }
        viewers.clear();
    }

    public void teleport(Location newLocation) {
        this.location = newLocation != null ? newLocation.clone() : null;
        if (this.location == null) return;
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                false
        );
        for (UUID viewerId : viewers) {
            Player player = org.bukkit.Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        }
    }

    public void sendMetadataUpdateToAll() {
        for (UUID viewerId : viewers) {
            Player player = org.bukkit.Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                updateMetadata(player);
            }
        }
    }
}
