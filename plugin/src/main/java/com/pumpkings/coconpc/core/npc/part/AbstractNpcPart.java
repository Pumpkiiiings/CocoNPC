package com.pumpkings.coconpc.core.npc.part;

import com.pumpkings.coconpc.core.packet.entity.VirtualItemDisplay;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.pumpkings.coconpc.core.npc.TransformMath;

public abstract class AbstractNpcPart implements NpcPart {
    protected VirtualItemDisplay virtualDisplay;

    protected Vector3f translation = new Vector3f();
    protected Vector3f baseTranslation = new Vector3f();
    protected Vector3f customOffset = new Vector3f();
    protected Vector3f animTranslation = new Vector3f();
    protected Quaternionf leftRotation = new Quaternionf();
    protected Quaternionf animRotation = new Quaternionf();
    protected Vector3f scale = new Vector3f(1f, 1f, 1f);
    protected Quaternionf rightRotation = new Quaternionf();
    protected float pitch = 0f;
    protected float yaw = 0f;
    protected float roll = 0f;
    protected boolean hidden = false;
    protected float shadowRadius = 0f;
    protected float shadowStrength = 1f;

    private static final Vector3f HIDDEN_SCALE = new Vector3f(0.0001f, 0.0001f, 0.0001f);

    @Override
    public void destroy() {
        if (virtualDisplay != null) {
            virtualDisplay.destroyForAll();
            virtualDisplay = null;
        }
    }

    @Override
    public void updateTransform(Location baseLocation) {
        if (virtualDisplay == null) return;
        if (baseLocation != null) virtualDisplay.teleport(baseLocation);
        applyTransformation();
        virtualDisplay.sendMetadataUpdateToAll();
    }

    private void applyTransformation() {
        Vector3f finalTranslation = new Vector3f(translation).add(animTranslation);
        Quaternionf finalRotation = new Quaternionf(leftRotation).mul(animRotation);
        
        virtualDisplay.setTransformation(
                finalTranslation,
                finalRotation,
                hidden ? new Vector3f(HIDDEN_SCALE) : scale,
                rightRotation);
    }

    protected void syncVirtualDisplay(Location baseLocation) {
        if (virtualDisplay == null) {
            virtualDisplay = new VirtualItemDisplay(baseLocation, null);
        } else if (baseLocation != null) {
            virtualDisplay.setLocation(baseLocation);
        }
        virtualDisplay.setShadow(shadowRadius, shadowStrength);
        applyTransformation();
    }

    @Override
    public void spawnFor(Player player, Location baseLocation) {
        syncVirtualDisplay(baseLocation);
        virtualDisplay.addViewer(player);
    }

    @Override
    public void destroyFor(Player player) {
        if (virtualDisplay != null) {
            virtualDisplay.removeViewer(player);
        }
    }

    @Override
    public void updateMetadataFor(Player player) {
        if (virtualDisplay == null) return;
        applyTransformation();
        virtualDisplay.updateMetadata(player);
    }

    @Override
    public int getVirtualEntityId() {
        return virtualDisplay != null ? virtualDisplay.getEntityId() : -1;
    }

    public void sendMetadataUpdateToAll() {
        if (virtualDisplay != null) {
            virtualDisplay.sendMetadataUpdateToAll();
        }
    }

    public void setRotation(float pitch, float yaw, float roll) {
        this.pitch = TransformMath.normalizeDegrees(pitch);
        this.yaw = TransformMath.normalizeDegrees(yaw);
        this.roll = TransformMath.normalizeDegrees(roll);
        leftRotation.set(getLocalRotation());
    }

    public void updateTransformRotation(float basePitch, float baseYaw, float baseRoll) {
        leftRotation.set(TransformMath.rotation(basePitch, baseYaw, baseRoll)).mul(getLocalRotation());
    }

    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }
    public float getRoll() { return roll; }
    public Quaternionf getLeftRotation() { return leftRotation; }
    public Quaternionf getLocalRotation() { return TransformMath.rotation(pitch, yaw, roll); }

    public void setEffectiveRotation(Quaternionf rotation) {
        leftRotation.set(rotation).normalize();
    }

    public void setTranslation(float x, float y, float z) {
        baseTranslation.set(x, y, z);
        translation.set(baseTranslation).add(customOffset);
    }

    public void setComputedTranslation(Vector3f value) {
        translation.set(value);
    }

    public void setCustomOffset(float x, float y, float z) {
        customOffset.set(x, y, z);
        translation.set(baseTranslation).add(customOffset);
    }

    public void addCustomOffset(float dx, float dy, float dz) {
        customOffset.add(dx, dy, dz);
        translation.set(baseTranslation).add(customOffset);
    }

    public Vector3f getCustomOffset() { return customOffset; }
    public Vector3f getTranslation() { return translation; }

    public void setAnimOffset(Vector3f animTranslation, Quaternionf animRotation) {
        this.animTranslation.set(animTranslation);
        this.animRotation.set(animRotation);
    }

    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
    }

    public float getScaleX() { return scale.x; }
    public float getScaleY() { return scale.y; }
    public float getScaleZ() { return scale.z; }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setShadow(float radius, float strength) {
        this.shadowRadius = radius;
        this.shadowStrength = strength;
        if (virtualDisplay != null) {
            virtualDisplay.setShadow(radius, strength);
            virtualDisplay.sendMetadataUpdateToAll();
        }
    }

    public VirtualItemDisplay getVirtualDisplay() {
        return virtualDisplay;
    }
}



