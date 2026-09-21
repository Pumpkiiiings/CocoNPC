package com.pumpkings.coconpc.core.npc;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.part.HitboxPart;
import com.pumpkings.coconpc.core.npc.part.ItemPart;
import com.pumpkings.coconpc.menu.EditorTarget;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import com.pumpkings.coconpc.core.animation.ActiveAnimation;

public class NpcEntity {
    private final CocoNPC plugin;
    private final UUID uuid;
    private Location location;
    private String id = null;
    private ActiveAnimation activeAnimation = null;
    private float size = 1.0F;
    private float globalPitch;
    private float globalYaw;
    private float globalRoll;

    private final Map<String, ItemPart> parts = new LinkedHashMap<>();
    private final HitboxPart hitbox = new HitboxPart();
    private final Set<UUID> viewers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private com.pumpkings.coconpc.core.packet.entity.VirtualTextDisplay virtualHologram = null;
    private NpcSkin skin;
    private final String defaultSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTI2NjY0YjEzYzcxN2Y1MGEwY2ZlODhlYzBkOGRiZmE5MWFlY2FhYjRjYjhiNDU5ZTRmM2UzMmVmYzhmNmNhYSJ9fX0=";
    private static final float HEAD_Y = 1.385f;
    private static final float TORSO_UPPER_Y = 1.386f;
    private static final float TORSO_LOWER_Y_OFFSET = -0.5f;
    private static final float ARM_Y = 1.266f;
    private static final float ARM_LOWER_Y_OFFSET = -0.38f;
    private static final float ARM_X_OFFSET = 0.255f;
    private static final float LEG_Y = 0.636f;
    private static final float LEG_LOWER_Y_OFFSET = -0.5f;
    private static final float LEG_X_OFFSET = 0.125f;

    public NpcEntity(CocoNPC plugin, Location location, UUID uuid) {
        this(plugin, location, uuid, 1.0F);
    }

    public NpcEntity(CocoNPC plugin, Location location, UUID uuid, float size) {
        this.plugin = plugin;
        this.uuid = uuid;
        if (location != null) {
            this.location = location.clone();
        }
        this.size = size;
        initParts();
    }

    private void initParts() {
        parts.put("head", new ItemPart(getDefaultHead()));
        parts.put("torsoUpper", new ItemPart(getDefaultHead()));
        parts.put("torsoLower", new ItemPart(getDefaultHead()));
        parts.put("rightArmUpper", new ItemPart(getDefaultHead()));
        parts.put("rightArmLower", new ItemPart(getDefaultHead()));
        parts.put("leftArmUpper", new ItemPart(getDefaultHead()));
        parts.put("leftArmLower", new ItemPart(getDefaultHead()));
        parts.put("rightLegUpper", new ItemPart(getDefaultHead()));
        parts.put("rightLegLower", new ItemPart(getDefaultHead()));
        parts.put("leftLegUpper", new ItemPart(getDefaultHead()));
        parts.put("leftLegLower", new ItemPart(getDefaultHead()));
        parts.put("right_item", new ItemPart(new ItemStack(org.bukkit.Material.AIR), ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND));
        parts.put("left_item", new ItemPart(new ItemStack(org.bukkit.Material.AIR), ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND));
        
        hitbox.setSize(0.8F * size, 1.8F * size);
    }

    private ItemStack getDefaultHead() {
        return com.pumpkings.coconpc.util.SkinUtils.getHead(defaultSkin, plugin.getLogger());
    }

    public void spawn() {
        if (location == null) return;
        updateTransforms();
        if (parts.containsKey("torsoUpper")) {
            parts.get("torsoUpper").setShadow(0.4F * size, 1.0F);
        }
        if (plugin.getPacketEngine() != null && plugin.getPacketEngine().getTracker() != null) {
            plugin.getPacketEngine().getTracker().refresh(this);
        }
    }

    public void updateTransforms() {
        org.joml.Quaternionf rootRotation = getGlobalRotation();

        configureRoot(parts.get("head"), new org.joml.Vector3f(0, HEAD_Y * size, 0),
                new org.joml.Vector3f(size, size, size), rootRotation);
        ItemPart torsoUpper = parts.get("torsoUpper");
        configureRoot(torsoUpper, new org.joml.Vector3f(0, TORSO_UPPER_Y * size, 0),
                new org.joml.Vector3f(size, size, 0.5f * size), rootRotation);
        ItemPart torsoLower = parts.get("torsoLower");
        configureChild(torsoUpper, torsoLower, new org.joml.Vector3f(0, TORSO_LOWER_Y_OFFSET * size, 0),
                new org.joml.Vector3f(size, 0.5f * size, 0.5f * size));

        ItemPart rightArm1 = parts.get("rightArmUpper");
        configureRoot(rightArm1, new org.joml.Vector3f(ARM_X_OFFSET * size, ARM_Y * size, 0),
                new org.joml.Vector3f(0.5f * size, size, 0.5f * size), rootRotation);
        ItemPart rightArm2 = parts.get("rightArmLower");
        configureChild(rightArm1, rightArm2, new org.joml.Vector3f(0, ARM_LOWER_Y_OFFSET * size, 0),
                new org.joml.Vector3f(0.5f * size, 0.5f * size, 0.5f * size));

        ItemPart leftArm1 = parts.get("leftArmUpper");
        configureRoot(leftArm1, new org.joml.Vector3f(-ARM_X_OFFSET * size, ARM_Y * size, 0),
                new org.joml.Vector3f(0.5f * size, size, 0.5f * size), rootRotation);
        ItemPart leftArm2 = parts.get("leftArmLower");
        configureChild(leftArm1, leftArm2, new org.joml.Vector3f(0, ARM_LOWER_Y_OFFSET * size, 0),
                new org.joml.Vector3f(0.5f * size, 0.5f * size, 0.5f * size));

        ItemPart rightItem = parts.get("right_item");
        if (rightItem != null) {
            configureChild(rightArm2, rightItem, new org.joml.Vector3f(0, -0.35f * size, 0),
                    new org.joml.Vector3f(0.6f * size));
        }
        ItemPart leftItem = parts.get("left_item");
        if (leftItem != null) {
            configureChild(leftArm2, leftItem, new org.joml.Vector3f(0, -0.35f * size, 0),
                    new org.joml.Vector3f(0.6f * size));
        }

        ItemPart rightLeg1 = parts.get("rightLegUpper");
        configureRoot(rightLeg1, new org.joml.Vector3f(LEG_X_OFFSET * size, LEG_Y * size, 0),
                new org.joml.Vector3f(0.5f * size, size, 0.5f * size), rootRotation);
        ItemPart rightLeg2 = parts.get("rightLegLower");
        configureChild(rightLeg1, rightLeg2, new org.joml.Vector3f(0, LEG_LOWER_Y_OFFSET * size, 0),
                new org.joml.Vector3f(0.5f * size));

        ItemPart leftLeg1 = parts.get("leftLegUpper");
        configureRoot(leftLeg1, new org.joml.Vector3f(-LEG_X_OFFSET * size, LEG_Y * size, 0),
                new org.joml.Vector3f(0.5f * size, size, 0.5f * size), rootRotation);
        ItemPart leftLeg2 = parts.get("leftLegLower");
        configureChild(leftLeg1, leftLeg2, new org.joml.Vector3f(0, LEG_LOWER_Y_OFFSET * size, 0),
                new org.joml.Vector3f(0.5f * size));
    }

    private void configureRoot(ItemPart part, org.joml.Vector3f base, org.joml.Vector3f scale,
                               org.joml.Quaternionf rootRotation) {
        part.setTranslation(base.x, base.y, base.z);
        part.setComputedTranslation(new org.joml.Vector3f(part.getTranslation()).rotate(rootRotation));
        part.setEffectiveRotation(new org.joml.Quaternionf(rootRotation).mul(part.getLocalRotation()));
        part.setScale(scale.x, scale.y, scale.z);
    }

    private void configureChild(ItemPart parent, ItemPart child, org.joml.Vector3f localOffset,
                                org.joml.Vector3f scale) {
        org.joml.Vector3f base = new org.joml.Vector3f(localOffset)
                .rotate(parent.getLeftRotation())
                .add(parent.getTranslation());
        child.setTranslation(base.x, base.y, base.z);
        org.joml.Vector3f localCustom = new org.joml.Vector3f(child.getCustomOffset())
                .rotate(parent.getLeftRotation());
        child.setComputedTranslation(base.add(localCustom));
        child.setEffectiveRotation(new org.joml.Quaternionf(parent.getLeftRotation()).mul(child.getLocalRotation()));
        child.setScale(scale.x, scale.y, scale.z);
    }

    public void teleport(Location nLocation) {
        this.location = nLocation;
        updateTransforms();
        parts.values().forEach(part -> part.updateTransform(location));
        hitbox.updateTransform(location);
        if (virtualHologram != null) {
            virtualHologram.teleport(hologramLocation());
        }
        if (plugin.getPacketEngine() != null && plugin.getPacketEngine().getTracker() != null) {
            plugin.getPacketEngine().getTracker().refresh(this);
        }
    }

    public void delete() {
        destroyForAll();
        plugin.getRegistry().getNpcs().remove(uuid);
        plugin.getRegistry().getNpcsId().remove(uuid);
        parts.values().forEach(ItemPart::destroy);
        hitbox.destroy();

        if (virtualHologram != null) {
            virtualHologram.destroyForAll();
            virtualHologram = null;
        }
        plugin.getConfigManager().delete(this);
    }

    public void setSkin(NpcSkin skin) {
        this.skin = skin;
        for (String partName : BodyParts.SKIN_PARTS) {
            String texture = skin.get(partName);
            if (texture != null) {
                parts.get(partName).setItem(
                        com.pumpkings.coconpc.util.SkinUtils.getHeadUrl(com.pumpkings.coconpc.util.SkinUtils.base64ToUrl(texture)));
            }
        }
    }

    public NpcSkin getSkin() {
        return skin;
    }

    public boolean toggleLimb(EditorTarget type) {
        if (type == EditorTarget.HOLOGRAM) {
            return toggleHologramVisibility();
        }

        String[] keys = partKeysFor(type);
        if (keys.length == 0) return false;

        ItemPart first = parts.get(keys[0]);
        if (first == null) return false;

        boolean targetVisible = first.isHidden();
        for (String key : keys) {
            setPartHidden(key, !targetVisible);
        }
        return targetVisible;
    }

    private String[] partKeysFor(EditorTarget type) {
        return switch (type) {
            case HEAD -> new String[]{"head"};
            case BODY -> new String[]{"torsoUpper", "torsoLower"};
            case RIGHT_ARM -> new String[]{"rightArmUpper", "rightArmLower"};
            case LEFT_ARM -> new String[]{"leftArmUpper", "leftArmLower"};
            case RIGHT_LEG -> new String[]{"rightLegUpper", "rightLegLower"};
            case LEFT_LEG -> new String[]{"leftLegUpper", "leftLegLower"};
            case RIGHT_ITEM -> new String[]{"right_item"};
            case LEFT_ITEM -> new String[]{"left_item"};
            case GLOBAL -> parts.keySet().toArray(new String[0]);
            default -> new String[0];
        };
    }

    private boolean toggleHologramVisibility() {
        if (virtualHologram != null) {
            boolean wasVisible = virtualHologram.isVisible();
            virtualHologram.setVisible(!wasVisible);
            virtualHologram.sendMetadataUpdateToAll();
            return !wasVisible;
        }
        return false;
    }

    public void setPartHidden(String key, boolean hidden) {
        ItemPart part = parts.get(key);
        if (part == null || part.isHidden() == hidden) return;
        part.setHidden(hidden);
        com.pumpkings.coconpc.core.packet.entity.VirtualItemDisplay vd = part.getVirtualDisplay();
        if (vd != null) {
            if (hidden) {
                vd.setScale(0.0001f, 0.0001f, 0.0001f);
            } else {
                vd.setScale(part.getScaleX(), part.getScaleY(), part.getScaleZ());
            }
            vd.sendMetadataUpdateToAll();
        }
    }

    public void setRotation(EditorTarget type, float pitch, float yaw, float roll) {
        if (type == EditorTarget.GLOBAL) {
            setGlobalRotation(pitch, yaw, roll);
        } else {
            forPrimaryParts(type, part -> part.setRotation(pitch, yaw, roll));
        }
        refreshTransforms();
    }

    public void addRotation(EditorTarget type, float dPitch, float dYaw, float dRoll) {
        if (type == EditorTarget.GLOBAL) {
            setGlobalRotation(globalPitch + dPitch, globalYaw + dYaw, globalRoll + dRoll);
        } else {
            forPrimaryParts(type, part -> part.setRotation(
                    part.getPitch() + dPitch, part.getYaw() + dYaw, part.getRoll() + dRoll));
        }
        refreshTransforms();
    }

    public void addJointRotation(EditorTarget type, float dPitch, float dYaw, float dRoll) {
        for (String key : jointKeysFor(type)) {
            ItemPart p = parts.get(key);
            if (p != null) p.setRotation(
                    p.getPitch() + dPitch, p.getYaw() + dYaw, p.getRoll() + dRoll);
        }
        refreshTransforms();
    }

    public void addOffset(EditorTarget type, float dX, float dY, float dZ) {
        forPrimaryParts(type, part -> part.addCustomOffset(dX, dY, dZ));
        refreshTransforms();
    }

    public void translate(float dX, float dY, float dZ) {
        if (location != null) teleport(location.clone().add(dX, dY, dZ));
    }

    public void setGlobalRotation(float pitch, float yaw, float roll) {
        this.globalPitch = TransformMath.normalizeDegrees(pitch);
        this.globalYaw = TransformMath.normalizeDegrees(yaw);
        this.globalRoll = TransformMath.normalizeDegrees(roll);
    }

    public org.joml.Quaternionf getGlobalRotation() {
        return TransformMath.rotation(globalPitch, globalYaw, globalRoll);
    }

    public float getGlobalPitch() { return globalPitch; }
    public float getGlobalYaw() { return globalYaw; }
    public float getGlobalRoll() { return globalRoll; }

    public void setOffset(EditorTarget type, float x, float y, float z) {
        forPrimaryParts(type, part -> part.setCustomOffset(x, y, z));
        refreshTransforms();
    }

    private void forEachPart(EditorTarget type, Consumer<ItemPart> action) {
        for (String key : partKeysFor(type)) {
            ItemPart part = parts.get(key);
            if (part != null) action.accept(part);
        }
    }

    private void forPrimaryParts(EditorTarget type, Consumer<ItemPart> action) {
        for (String key : primaryKeysFor(type)) {
            ItemPart part = parts.get(key);
            if (part != null) action.accept(part);
        }
    }

    private void refreshTransforms() {
        updateTransforms();
        parts.values().forEach(part -> part.updateTransform(location));
    }

    private String[] jointKeysFor(EditorTarget type) {
        return switch (type) {
            case HEAD -> new String[]{"head"};
            case BODY -> new String[]{"torsoLower"};
            case RIGHT_ARM -> new String[]{"rightArmLower"};
            case LEFT_ARM -> new String[]{"leftArmLower"};
            case RIGHT_LEG -> new String[]{"rightLegLower"};
            case LEFT_LEG -> new String[]{"leftLegLower"};
            case RIGHT_ITEM -> new String[]{"right_item"};
            case LEFT_ITEM -> new String[]{"left_item"};
            case GLOBAL -> parts.keySet().toArray(new String[0]);
            default -> new String[0];
        };
    }

    private String[] primaryKeysFor(EditorTarget type) {
        return switch (type) {
            case HEAD -> new String[]{"head"};
            case BODY -> new String[]{"torsoUpper"};
            case RIGHT_ARM -> new String[]{"rightArmUpper"};
            case LEFT_ARM -> new String[]{"leftArmUpper"};
            case RIGHT_LEG -> new String[]{"rightLegUpper"};
            case LEFT_LEG -> new String[]{"leftLegUpper"};
            case RIGHT_ITEM -> new String[]{"right_item"};
            case LEFT_ITEM -> new String[]{"left_item"};
            case GLOBAL -> new String[]{"head", "torsoUpper", "rightArmUpper", "leftArmUpper", "rightLegUpper", "leftLegUpper"};
            default -> new String[0];
        };
    }

    private Location hologramLocation() {
        return location.clone().add(0, (1.8F * size) + 0.5, 0);
    }

    public void moveHologram(double dy) {
        if (virtualHologram == null || virtualHologram.getLocation() == null) return;
        virtualHologram.teleport(virtualHologram.getLocation().clone().add(0, dy, 0));
    }

    public void spawnHologram(List<String> lines) {
        Location holoLoc = hologramLocation();
        String billboardStr = plugin.getConfigManager().getRepository().getHologramBillboard(this);
        boolean shadow = plugin.getConfigManager().getRepository().getHologramShadowed(this);
        String bgStr = plugin.getConfigManager().getRepository().getHologramBackground(this);
        byte billboardByte = 3;
        try {
            Display.Billboard b = Display.Billboard.valueOf(billboardStr.toUpperCase());
            billboardByte = switch (b) {
                case FIXED -> (byte) 0;
                case VERTICAL -> (byte) 1;
                case HORIZONTAL -> (byte) 2;
                case CENTER -> (byte) 3;
            };
        } catch (Exception ignored) {}

        if (virtualHologram == null) {
            virtualHologram = new com.pumpkings.coconpc.core.packet.entity.VirtualTextDisplay(holoLoc, null);
        } else {
            virtualHologram.setLocation(holoLoc);
        }
        virtualHologram.setBillboard(billboardByte);
        virtualHologram.setShadowed(shadow);
        org.bukkit.Color bgCol = plugin.getUtils().parseBackgroundColor(bgStr);
        if (bgCol != null) {
            virtualHologram.setBackgroundColor(bgCol.asARGB());
        }
        updateHologram(lines);
        for (UUID viewerId : viewers) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                virtualHologram.addViewer(p);
            }
        }
        if (plugin.getPacketEngine() != null && plugin.getPacketEngine().getTracker() != null) {
            plugin.getPacketEngine().getTracker().refresh(this);
        }
    }

    public void updateHologramStyle(String billboardStr, boolean shadowed, String bgStr) {
        if (virtualHologram != null) {
            byte billboardByte = 3;
            try {
                Display.Billboard b = Display.Billboard.valueOf(billboardStr.toUpperCase());
                billboardByte = switch (b) {
                    case FIXED -> (byte) 0;
                    case VERTICAL -> (byte) 1;
                    case HORIZONTAL -> (byte) 2;
                    case CENTER -> (byte) 3;
                };
            } catch (Exception ignored) {}
            virtualHologram.setBillboard(billboardByte);
            virtualHologram.setShadowed(shadowed);
            org.bukkit.Color bgCol = plugin.getUtils().parseBackgroundColor(bgStr);
            if (bgCol != null) {
                virtualHologram.setBackgroundColor(bgCol.asARGB());
            }
            virtualHologram.sendMetadataUpdateToAll();
        }
    }

    public void updateHologram(List<String> lines) {
        String billboardStr = plugin.getConfigManager().getRepository().getHologramBillboard(this);
        boolean shadow = plugin.getConfigManager().getRepository().getHologramShadowed(this);
        String bgStr = plugin.getConfigManager().getRepository().getHologramBackground(this);
        updateHologramStyle(billboardStr, shadow, bgStr);
        net.kyori.adventure.text.Component text = net.kyori.adventure.text.Component.empty();
        if (lines != null && !lines.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                text = text.append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line));
                if (i < lines.size() - 1) {
                    text = text.append(net.kyori.adventure.text.Component.newline());
                }
            }
        }
        if (virtualHologram != null) {
            virtualHologram.setText(text);
            virtualHologram.sendMetadataUpdateToAll();
        }
    }

    public boolean hasHologram() { return virtualHologram != null; }

    public Location getLocation() { return location; }
    public UUID getUUID() { return uuid; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public void setSize(float size) { this.size = size; }
    public float getSize() { return size; }

    public void resize(float newSize) {
        this.size = newSize;
        hitbox.setSize(0.8F * size, 1.8F * size);
        if (hitbox.isSpawned()) {
            hitbox.updateTransform(location);
        }
        if (parts.containsKey("torsoUpper")) {
            parts.get("torsoUpper").setShadow(0.4F * size, 1.0F);
        }
        updateTransforms();
        parts.values().forEach(part -> part.updateTransform(location));
        if (virtualHologram != null) {
            virtualHologram.teleport(hologramLocation());
        }
        sendMetadataUpdateToAllViewers();
    }

    public void sendMetadataUpdateToAllViewers() {
        for (UUID viewerId : viewers) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                hitbox.updateMetadataFor(player);
                for (ItemPart part : parts.values()) {
                    part.updateMetadataFor(player);
                }
                if (virtualHologram != null) {
                    virtualHologram.updateMetadata(player);
                }
            }
        }
    }
    public void setPartRotation(String partName, float pitch, float yaw, float roll) {
        ItemPart part = parts.get(partName);
        if (part != null) {
            String parentKey = parentKeyFor(partName);
            if (parentKey == null) {
                part.setRotation(pitch, yaw, roll);
            } else {
                ItemPart parent = parts.get(parentKey);
                org.joml.Quaternionf local = TransformMath.localFromWorld(
                        parent.getLeftRotation(), TransformMath.rotation(pitch, yaw, roll));
                org.joml.Vector3f localDegrees = TransformMath.degrees(local);
                part.setRotation(localDegrees.x, localDegrees.y, localDegrees.z);
            }
        }
    }

    public void migrateLegacyChildRotationsToLocal() {
        for (String childKey : new String[]{"torsoLower", "rightArmLower", "leftArmLower", "rightLegLower", "leftLegLower"}) {
            ItemPart child = parts.get(childKey);
            ItemPart parent = parts.get(parentKeyFor(childKey));
            org.joml.Quaternionf local = TransformMath.localFromWorld(parent.getLocalRotation(), child.getLocalRotation());
            org.joml.Vector3f degrees = TransformMath.degrees(local);
            child.setRotation(degrees.x, degrees.y, degrees.z);
        }
    }

    private String parentKeyFor(String childKey) {
        return switch (childKey) {
            case "torsoLower" -> "torsoUpper";
            case "rightArmLower" -> "rightArmUpper";
            case "leftArmLower" -> "leftArmUpper";
            case "rightLegLower" -> "rightLegUpper";
            case "leftLegLower" -> "leftLegUpper";
            case "right_item" -> "rightArmLower";
            case "left_item" -> "leftArmLower";
            default -> null;
        };
    }

    public ItemPart getPart(String partName) {
        return parts.get(partName);
    }

    public void resetAllRotations() {
        setGlobalRotation(0f, 0f, 0f);
        for (ItemPart part : parts.values()) {
            part.setRotation(0f, 0f, 0f);
        }
        setOffset(EditorTarget.GLOBAL, 0f, 0f, 0f);
        sendMetadataUpdateToAllViewers();
    }

    public void applyPose(NpcPose pose) {
        pose.apply(this);
        updateTransforms();
        if (location != null) {
            parts.values().forEach(part -> part.updateTransform(location));
        }
        sendMetadataUpdateToAllViewers();
    }

    public void setRightHandItem(ItemStack item) {
        ItemPart part = parts.get("right_item");
        if (part != null) {
            part.setItem(item != null ? item : new ItemStack(org.bukkit.Material.AIR));
        }
    }

    public void setLeftHandItem(ItemStack item) {
        ItemPart part = parts.get("left_item");
        if (part != null) {
            part.setItem(item != null ? item : new ItemStack(org.bukkit.Material.AIR));
        }
    }

    public ItemStack getRightHandItem() {
        ItemPart part = parts.get("right_item");
        return part != null && part.getItem() != null ? part.getItem() : new ItemStack(org.bukkit.Material.AIR);
    }

    public ItemStack getLeftHandItem() {
        ItemPart part = parts.get("left_item");
        return part != null && part.getItem() != null ? part.getItem() : new ItemStack(org.bukkit.Material.AIR);
    }

    public void showTo(org.bukkit.entity.Player player) {
        if (player == null || !player.isOnline() || location == null) return;
        if (viewers.add(player.getUniqueId())) {
            hitbox.spawnFor(player, location);
            for (ItemPart part : parts.values()) {
                part.spawnFor(player, location);
            }
            if (virtualHologram != null) {
                virtualHologram.addViewer(player);
            }
        }
    }

    public void hideFrom(org.bukkit.entity.Player player) {
        if (player == null) return;
        if (viewers.remove(player.getUniqueId())) {
            hitbox.destroyFor(player);
            for (ItemPart part : parts.values()) {
                part.destroyFor(player);
            }
            if (virtualHologram != null) {
                virtualHologram.removeViewer(player);
            }
        }
    }

    public void destroyForAll() {
        for (UUID viewerId : new HashSet<>(viewers)) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(viewerId);
            if (player != null && player.isOnline()) {
                hideFrom(player);
            }
        }
        viewers.clear();
    }

    public boolean isViewer(org.bukkit.entity.Player player) {
        return player != null && viewers.contains(player.getUniqueId());
    }

    public boolean matchesVirtualId(int virtualId) {
        if (hitbox.getVirtualEntityId() == virtualId) return true;
        for (ItemPart part : parts.values()) {
            if (part.getVirtualEntityId() == virtualId) return true;
        }
        if (virtualHologram != null && virtualHologram.getEntityId() == virtualId) return true;
        return false;
    }

    public void playAnimation(com.pumpkings.coconpc.core.animation.Animation animation) {
        this.activeAnimation = new com.pumpkings.coconpc.core.animation.ActiveAnimation(animation);
    }

    public void stopAnimation() {
        this.activeAnimation = null;
        for (ItemPart part : parts.values()) {
            part.setAnimOffset(new org.joml.Vector3f(), new org.joml.Quaternionf());
            part.sendMetadataUpdateToAll();
        }
    }

    public void applyAnimation(java.util.Map<String, org.joml.Vector3f> posOffsets, java.util.Map<String, org.joml.Quaternionf> rotOffsets) {
        applyAnimToPart("head", posOffsets.get("head"), rotOffsets.get("head"));
        
        org.joml.Vector3f torsoP = syncUpperAndLower("torsoUpper", "torsoLower", posOffsets.get("body"), rotOffsets.get("body"), TORSO_LOWER_Y_OFFSET);
        org.joml.Vector3f rightArmP = syncUpperAndLower("rightArmUpper", "rightArmLower", posOffsets.get("right_arm"), rotOffsets.get("right_arm"), ARM_LOWER_Y_OFFSET);
        org.joml.Vector3f leftArmP = syncUpperAndLower("leftArmUpper", "leftArmLower", posOffsets.get("left_arm"), rotOffsets.get("left_arm"), ARM_LOWER_Y_OFFSET);
        syncUpperAndLower("rightLegUpper", "rightLegLower", posOffsets.get("right_leg"), rotOffsets.get("right_leg"), LEG_LOWER_Y_OFFSET);
        syncUpperAndLower("leftLegUpper", "leftLegLower", posOffsets.get("left_leg"), rotOffsets.get("left_leg"), LEG_LOWER_Y_OFFSET);
        
        syncHandItem("rightArmLower", "right_item", rightArmP, rotOffsets.get("right_arm"), -0.35f);
        syncHandItem("leftArmLower", "left_item", leftArmP, rotOffsets.get("left_arm"), -0.35f);
    }

    private void applyAnimToPart(String partName, org.joml.Vector3f pos, org.joml.Quaternionf rot) {
        ItemPart part = parts.get(partName);
        if (part != null) {
            org.joml.Vector3f p = pos != null ? pos : new org.joml.Vector3f();
            org.joml.Quaternionf r = rot != null ? rot : new org.joml.Quaternionf();
            part.setAnimOffset(p, r);
            part.sendMetadataUpdateToAll();
        }
    }

    private org.joml.Vector3f syncUpperAndLower(String upperName, String lowerName, org.joml.Vector3f pos, org.joml.Quaternionf rot, float yOffset) {
        ItemPart upper = parts.get(upperName);
        ItemPart lower = parts.get(lowerName);
        
        org.joml.Vector3f p = pos != null ? pos : new org.joml.Vector3f();
        org.joml.Quaternionf r = rot != null ? rot : new org.joml.Quaternionf();
        
        if (upper != null) {
            upper.setAnimOffset(p, r);
            upper.sendMetadataUpdateToAll();
        }
        
        if (upper == null || lower == null) return p;
        
        org.joml.Vector3f offset = new org.joml.Vector3f(0, yOffset * size, 0);
        org.joml.Vector3f baseOffset = new org.joml.Vector3f(offset).rotate(upper.getLeftRotation());
        org.joml.Quaternionf totalRot = new org.joml.Quaternionf(upper.getLeftRotation()).mul(r);
        org.joml.Vector3f animOffset = new org.joml.Vector3f(offset).rotate(totalRot);
        
        org.joml.Vector3f diff = animOffset.sub(baseOffset);
        org.joml.Vector3f lowerP = new org.joml.Vector3f(p).add(diff);
        lower.setAnimOffset(lowerP, r);
        lower.sendMetadataUpdateToAll();
        return lowerP;
    }

    private void syncHandItem(String lowerName, String itemName, org.joml.Vector3f lowerP, org.joml.Quaternionf rot, float yOffset) {
        ItemPart lower = parts.get(lowerName);
        ItemPart item = parts.get(itemName);
        if (lower == null || item == null) return;
        
        org.joml.Quaternionf r = rot != null ? rot : new org.joml.Quaternionf();
        
        org.joml.Vector3f offset = new org.joml.Vector3f(0, yOffset * size, 0);
        org.joml.Vector3f baseOffset = new org.joml.Vector3f(offset).rotate(lower.getLeftRotation());
        org.joml.Quaternionf totalRot = new org.joml.Quaternionf(lower.getLeftRotation()).mul(r);
        org.joml.Vector3f animOffset = new org.joml.Vector3f(offset).rotate(totalRot);
        
        org.joml.Vector3f diff = animOffset.sub(baseOffset);
        org.joml.Vector3f itemP = new org.joml.Vector3f(lowerP).add(diff);
        item.setAnimOffset(itemP, r);
        item.sendMetadataUpdateToAll();
    }

    public com.pumpkings.coconpc.core.animation.ActiveAnimation getActiveAnimation() {
        return activeAnimation;
    }
}


