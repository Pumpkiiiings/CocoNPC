package com.pumpkings.coconpc.core.packet.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VirtualItemDisplay extends VirtualEntity {
    private ItemStack item;
    private ItemDisplay.ItemDisplayTransform transform = ItemDisplay.ItemDisplayTransform.HEAD;
    private final org.joml.Vector3f translation = new org.joml.Vector3f();
    private final org.joml.Vector3f scale = new org.joml.Vector3f(1f, 1f, 1f);
    private final org.joml.Quaternionf leftRotation = new org.joml.Quaternionf();
    private final org.joml.Quaternionf rightRotation = new org.joml.Quaternionf();
    private float shadowRadius = 0f;
    private float shadowStrength = 1f;

    public VirtualItemDisplay(Location location, ItemStack item) {
        super(location);
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public ItemDisplay.ItemDisplayTransform getTransform() {
        return transform;
    }

    public void setTransform(ItemDisplay.ItemDisplayTransform transform) {
        this.transform = transform;
    }

    public void setTransformation(org.joml.Vector3f translation, org.joml.Quaternionf leftRot, org.joml.Vector3f scale, org.joml.Quaternionf rightRot) {
        if (translation != null) this.translation.set(translation);
        if (leftRot != null) this.leftRotation.set(leftRot);
        if (scale != null) this.scale.set(scale);
        if (rightRot != null) this.rightRotation.set(rightRot);
    }

    public void setTranslation(float x, float y, float z) {
        this.translation.set(x, y, z);
    }

    public void setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
    }

    public void setLeftRotation(float x, float y, float z, float w) {
        this.leftRotation.set(x, y, z, w);
    }

    public void setRightRotation(float x, float y, float z, float w) {
        this.rightRotation.set(x, y, z, w);
    }

    public org.joml.Vector3f getTranslation() {
        return translation;
    }

    public org.joml.Vector3f getScale() {
        return scale;
    }

    public org.joml.Quaternionf getLeftRotation() {
        return leftRotation;
    }

    public org.joml.Quaternionf getRightRotation() {
        return rightRotation;
    }

    public void setShadow(float radius, float strength) {
        this.shadowRadius = radius;
        this.shadowStrength = strength;
    }

    @Override
    public void spawn(Player player) {
        if (player == null || !player.isOnline() || location == null) return;
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.ITEM_DISPLAY,
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
        metadata.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(translation.x, translation.y, translation.z)));
        metadata.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, new Vector3f(scale.x, scale.y, scale.z)));
        metadata.add(new EntityData<>(13, EntityDataTypes.QUATERNION, new Quaternion4f(leftRotation.x, leftRotation.y, leftRotation.z, leftRotation.w)));
        metadata.add(new EntityData<>(14, EntityDataTypes.QUATERNION, new Quaternion4f(rightRotation.x, rightRotation.y, rightRotation.z, rightRotation.w)));
        metadata.add(new EntityData<>(18, EntityDataTypes.FLOAT, shadowRadius));
        metadata.add(new EntityData<>(19, EntityDataTypes.FLOAT, shadowStrength));
        if (item != null) {
            metadata.add(new EntityData<>(23, EntityDataTypes.ITEMSTACK, SpigotConversionUtil.fromBukkitItemStack(item)));
        }
        metadata.add(new EntityData<>(24, EntityDataTypes.BYTE, toDisplayTransformByte(transform)));

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metadataPacket);
    }

    private byte toDisplayTransformByte(ItemDisplay.ItemDisplayTransform transform) {
        if (transform == null) return 0;
        return switch (transform) {
            case NONE -> (byte) 0;
            case THIRDPERSON_LEFTHAND -> (byte) 1;
            case THIRDPERSON_RIGHTHAND -> (byte) 2;
            case FIRSTPERSON_LEFTHAND -> (byte) 3;
            case FIRSTPERSON_RIGHTHAND -> (byte) 4;
            case HEAD -> (byte) 5;
            case GUI -> (byte) 6;
            case GROUND -> (byte) 7;
            case FIXED -> (byte) 8;
        };
    }
}
