package com.pumpkings.coconpc.core.npc.part;

import com.pumpkings.coconpc.core.packet.entity.VirtualHitbox;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HitboxPart implements NpcPart {
    private VirtualHitbox virtualHitbox;
    private float width = 0.8f;
    private float height = 1.8f;

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
        if (virtualHitbox != null) {
            virtualHitbox.setSize(width, height);
            virtualHitbox.sendMetadataUpdateToAll();
        }
    }

    @Override
    public void destroy() {
        if (virtualHitbox != null) {
            virtualHitbox.destroyForAll();
            virtualHitbox = null;
        }
    }

    @Override
    public void updateTransform(Location baseLocation) {
        if (virtualHitbox != null && baseLocation != null) {
            virtualHitbox.teleport(baseLocation);
        }
    }

    @Override
    public void spawnFor(Player player, Location baseLocation) {
        if (virtualHitbox == null) {
            virtualHitbox = new VirtualHitbox(baseLocation);
            virtualHitbox.setSize(width, height);
        } else {
            virtualHitbox.teleport(baseLocation);
        }
        virtualHitbox.addViewer(player);
    }

    @Override
    public void destroyFor(Player player) {
        if (virtualHitbox != null) {
            virtualHitbox.removeViewer(player);
        }
    }

    @Override
    public void updateMetadataFor(Player player) {
        if (virtualHitbox != null) {
            virtualHitbox.setSize(width, height);
            virtualHitbox.updateMetadata(player);
        }
    }

    @Override
    public int getVirtualEntityId() {
        return virtualHitbox != null ? virtualHitbox.getEntityId() : -1;
    }

    public boolean isSpawned() {
        return virtualHitbox != null;
    }
}

