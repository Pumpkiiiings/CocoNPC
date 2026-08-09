package com.pumpkings.coconpc.core.packet.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualTextDisplay extends VirtualEntity {
    private Component text = Component.empty();
    private byte billboard = 3;
    private int backgroundColor = 1073741824;
    private byte textOpacity = -1;
    private byte styleFlags = 0;
    private boolean visible = true;
    private Vector3f scale = new Vector3f(1f, 1f, 1f);

    public VirtualTextDisplay(Location location, Component text) {
        super(location);
        if (text != null) {
            this.text = text;
        }
    }

    public Component getText() {
        return text;
    }

    public void setText(Component text) {
        this.text = text != null ? text : Component.empty();
    }

    public void setBillboard(byte billboard) {
        this.billboard = billboard;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setShadowed(boolean shadow) {
        if (shadow) {
            this.styleFlags |= 0x01;
        } else {
            this.styleFlags &= ~0x01;
        }
    }

    @Override
    public void spawn(Player player) {
        if (player == null || !player.isOnline() || location == null) return;
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.TEXT_DISPLAY,
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
        metadata.add(new EntityData<>(15, EntityDataTypes.BYTE, billboard));
        byte entityFlags = visible ? (byte) 0 : (byte) 0x20;
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, entityFlags));
        metadata.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, scale));
        metadata.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, text));
        metadata.add(new EntityData<>(25, EntityDataTypes.INT, backgroundColor));
        metadata.add(new EntityData<>(26, EntityDataTypes.BYTE, textOpacity));
        metadata.add(new EntityData<>(27, EntityDataTypes.BYTE, styleFlags));

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            this.scale = new Vector3f(1f, 1f, 1f);
        } else {
            this.scale = new Vector3f(0.0001f, 0.0001f, 0.0001f);
        }
    }
}


