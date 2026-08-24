package com.pumpkings.coconpc.core.packet.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualHitbox extends VirtualEntity {
    private float width = 0.8f;
    private float height = 1.8f;
    private boolean response = true;

    public VirtualHitbox(Location location) {
        super(location);
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }

    @Override
    public void spawn(Player player) {
        if (player == null || !player.isOnline() || location == null) return;
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.INTERACTION,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getPitch(),
                location.getYaw(),
                location.getYaw(),
                0,
                Optional.empty()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);
        updateMetadata(player);
    }

    @Override
    public void updateMetadata(Player player) {
        if (player == null || !player.isOnline()) return;
        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(8, EntityDataTypes.FLOAT, width));
        metadata.add(new EntityData<>(9, EntityDataTypes.FLOAT, height));
        metadata.add(new EntityData<>(10, EntityDataTypes.BOOLEAN, response));

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);
    }
}
