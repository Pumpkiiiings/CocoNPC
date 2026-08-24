package com.pumpkings.coconpc.core.npc.registry;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.config.PluginConfig;
import com.pumpkings.coconpc.core.npc.BodyParts;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.npc.NpcSkin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NpcRegistry {
    private final CocoNPC plugin;
    private final Map<UUID, NpcEntity> npcs = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, String> npcs_id = new java.util.concurrent.ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    public NpcRegistry(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, NpcEntity> getNpcs() {
        return npcs;
    }

    public Map<UUID, String> getNpcsId() {
        return npcs_id;
    }

    public NpcEntity getByVirtualId(int virtualId) {
        for (NpcEntity npc : npcs.values()) {
            if (npc.matchesVirtualId(virtualId)) {
                return npc;
            }
        }
        return null;
    }

    public void spawn(Location location, Player player, String text, float size, String customId) {
        Message.SPAWNING.send(plugin, player);
        long start = System.currentTimeMillis();
        resolveSkin(player, text, skin -> spawnNpc(player, location, skin, start, size, customId));
    }

    public void changeSkin(NpcEntity npc, Player player, String text) {
        Message.SKIN_CHANGING.send(plugin, player);
        long start = System.currentTimeMillis();
        resolveSkin(player, text, skin -> applySkinChange(npc, player, skin, start));
    }

    private void resolveSkin(Player player, String rawText, java.util.function.Consumer<NpcSkin> onReady) {
        java.io.File skinFolder = new java.io.File(plugin.getDataFolder(), "skins");
        if (!skinFolder.exists()) {
            skinFolder.mkdirs();
        }

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            String text = rawText;
            java.io.File localFile = new java.io.File(skinFolder, text);
            if (!localFile.exists() && !text.endsWith(".png")) {
                localFile = new java.io.File(skinFolder, text + ".png");
            }

            if (localFile.exists()) {
                text = localFile.getName();
            } else {
                Matcher matcher = pattern.matcher(text);
                if (text.length() <= 16 && matcher.matches()) {
                    String link = plugin.getMojangApi().fetchMojangSkinUrl(text);
                    if (link == null) {
                        runOnMain(() -> Message.SKIN_NOT_FOUND.send(plugin, player));
                        return;
                    }
                    text = link;
                }
            }

            final String resolved = text;
            final String code = com.pumpkings.coconpc.util.SkinUtils.urlToBase64(resolved);
            runOnMain(() -> {
                PluginConfig config = plugin.getConfigManager().skins;
                if (config.contains(code)) {
                    com.pumpkings.coconpc.core.npc.NpcSkin cached = config.getSkin(code);
                    if (cached.get("torsoUpper") != null) {
                        onReady.accept(cached);
                        return;
                    }
                }
                uploadAndCache(player, resolved, code, onReady);
            });
        });
    }

    private void uploadAndCache(Player player, String resolved, String code,
                                java.util.function.Consumer<NpcSkin> onReady) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            BufferedImage baseskin = getSkin(resolved);
            if (baseskin == null) {
                runOnMain(() -> Message.SKIN_URL_INVALID.send(plugin, player));
                return;
            }
            if (com.pumpkings.coconpc.util.MineskinService.isSlim(baseskin)) {
                com.pumpkings.coconpc.util.MineskinService.fixSlim(baseskin);
            }

            plugin.getMineskinService().uploadSkins(player, baseskin, resolved)
                    .thenAccept(urlMap -> runOnMain(() -> {
                        NpcSkin skin = buildSkin(com.pumpkings.coconpc.util.SkinUtils.urlToBase64(resolved), urlMap);
                        PluginConfig config = plugin.getConfigManager().skins;
                        config.setSkin(code, skin);
                        config.saveConfig();
                        onReady.accept(skin);
                    }))
                    .exceptionally(ex -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        plugin.getLogger().warning("Skin upload failed for " + resolved + ": " + cause.getMessage());
                        runOnMain(() -> Message.SKIN_GENERATION_FAILED.send(plugin, player, "{error}", cause.getMessage()));
                        return null;
                    });
        });
    }

    public NpcSkin buildSkin(String key, Map<String, String> urlMap) {
        NpcSkin skin = new NpcSkin(plugin, key);
        for (String partName : BodyParts.SKIN_PARTS) {
            String url = urlMap.get(partName);
            if (url != null) {
                skin.set(partName, com.pumpkings.coconpc.util.SkinUtils.urlToBase64(url));
            }
        }
        return skin;
    }

    private void runOnMain(Runnable action) {
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> action.run());
    }

    private void applySkinChange(NpcEntity npc, Player player, NpcSkin skin, Long old) {
        Bukkit.getRegionScheduler().run(plugin, npc.getLocation(), task -> {
            npc.setSkin(skin);
            plugin.getConfigManager().getRepository().saveLocationAndSkin(npc);
            long n = System.currentTimeMillis() - old;
            Message.SKIN_APPLIED.send(plugin, player, "{time}", String.valueOf(n / 1000L));
        });
    }

    private void spawnNpc(final Player player, final Location location, final NpcSkin skin, final Long old, final float size, final String customId) {
        Bukkit.getRegionScheduler().runDelayed(plugin, location, task -> {
            UUID uuid = UUID.randomUUID();
            NpcEntity npc = new NpcEntity(plugin, location, uuid, size);
            npc.setSkin(skin);
            npc.spawn();
            npcs.put(uuid, npc);
            
            long n = System.currentTimeMillis() - old;
            Message.SPAWNED_SUCCESS.send(plugin, player, "{time}", String.valueOf(n / 1000L));
            
            npc.setId(customId);
            plugin.getSelectionManager().select(player, npc);
            com.pumpkings.coconpc.core.config.NpcData data = plugin.getConfigManager().spawn(npc, customId);
            plugin.getConfigManager().getRepository().saveParts(npc);
            npc.spawnHologram(data.hologramLines());
        }, 1L);
    }

    public void cloneNpc(NpcEntity source, Location location, Player player, String newId) {
        Bukkit.getRegionScheduler().runDelayed(plugin, location, task -> {
            UUID uuid = UUID.randomUUID();
            NpcEntity clone = new NpcEntity(plugin, location, uuid, source.getSize());
            NpcSkin skin = plugin.getConfigManager().getRepository().loadSkin(source.getUUID());
            if (skin != null) {
                clone.setSkin(skin);
            }
            for (String partName : BodyParts.ALL) {
                com.pumpkings.coconpc.core.npc.part.ItemPart sp = source.getPart(partName);
                com.pumpkings.coconpc.core.npc.part.ItemPart cp = clone.getPart(partName);
                if (sp != null && cp != null) {
                    cp.setRotation(sp.getPitch(), sp.getYaw(), sp.getRoll());
                    cp.setCustomOffset(sp.getCustomOffset().x, sp.getCustomOffset().y, sp.getCustomOffset().z);
                }
            }
            if (source.getRightHandItem() != null) clone.setRightHandItem(source.getRightHandItem().clone());
            if (source.getLeftHandItem() != null) clone.setLeftHandItem(source.getLeftHandItem().clone());

            clone.spawn();
            npcs.put(uuid, clone);
            com.pumpkings.coconpc.core.config.NpcData data = plugin.getConfigManager().getRepository().save(clone, newId);
            List<String> srcHoloLines = plugin.getConfigManager().getRepository().getHologramLines(source);
            String srcBillboard = plugin.getConfigManager().getRepository().getHologramBillboard(source);
            boolean srcShadow = plugin.getConfigManager().getRepository().getHologramShadowed(source);
            String srcBg = plugin.getConfigManager().getRepository().getHologramBackground(source);
            plugin.getConfigManager().getRepository().saveHologram(clone, srcHoloLines);
            plugin.getConfigManager().getRepository().saveHologramStyle(clone, srcBillboard, srcShadow, srcBg);
            plugin.getConfigManager().getRepository().saveHandItems(clone, clone.getRightHandItem(), clone.getLeftHandItem());
            List<String> srcActions = plugin.getConfigManager().getActions(source);
            plugin.getConfigManager().saveActions(clone, srcActions);
            plugin.getConfigManager().getRepository().saveLocationAndSkin(clone);
            plugin.getConfigManager().getRepository().saveParts(clone);

            clone.spawnHologram(srcHoloLines);
            clone.updateTransforms();

            plugin.getSelectionManager().select(player, clone);
            npcs_id.put(uuid, newId);

            Message.CLONE_SUCCESS.send(plugin, player, "{source}", source.getId(), "{new}", newId);
        }, 1L);
    }

    public void loadAllPersistedNpcs() {
        Map<UUID, com.pumpkings.coconpc.core.config.NpcData> dataMap = plugin.getConfigManager().getRepository().loadAll();
        for (Map.Entry<UUID, com.pumpkings.coconpc.core.config.NpcData> entry : dataMap.entrySet()) {
            UUID uuid = entry.getKey();
            com.pumpkings.coconpc.core.config.NpcData data = entry.getValue();
            org.bukkit.Location loc = plugin.getConfigManager().getRepository().loadLocation(uuid);
            if (loc == null || loc.getWorld() == null) continue;
            NpcEntity npc = npcs.get(uuid);
            if (npc == null) {
                npc = new NpcEntity(plugin, loc, uuid, data.size());
                npc.setId(data.id());
                npc.setRightHandItem(data.rightHandItem());
                npc.setLeftHandItem(data.leftHandItem());
                com.pumpkings.coconpc.core.npc.NpcSkin skin = plugin.getConfigManager().getRepository().loadSkin(uuid);
                if (skin != null) {
                    npc.setSkin(skin);
                }
                plugin.getConfigManager().getRepository().loadParts(npc);
                npc.spawn();
                npc.spawnHologram(data.hologramLines());
                npcs.put(uuid, npc);
                npcs_id.put(uuid, data.id());
            }
        }
    }

    public void loadChunk(final Chunk chunk) {
        Bukkit.getRegionScheduler().run(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), task -> {
            for (Entity entity : chunk.getEntities()) {
                if (isNpcPart(entity)) {
                    entity.remove();
                }
            }
        });
    }

    public boolean isNpcPart(Entity entity) {
        NamespacedKey key1 = new NamespacedKey(plugin, "CocoNPC");
        NamespacedKey key2 = new NamespacedKey("coconpc", "is_npc");
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (entity.getCustomName() != null && entity.getCustomName().startsWith("CocoNPC")) {
            return true;
        }
        return container.has(key1, PersistentDataType.STRING) 
            || container.has(key2, PersistentDataType.STRING) 
            || container.has(key1, PersistentDataType.BYTE) 
            || container.has(key2, PersistentDataType.BYTE);
    }

    public String getNpcPartData(Entity entity) {
        NamespacedKey key1 = new NamespacedKey(plugin, "CocoNPC");
        NamespacedKey key2 = new NamespacedKey("coconpc", "is_npc");
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (entity.getCustomName() != null && entity.getCustomName().startsWith("CocoNPC")) {
            return entity.getCustomName();
        } else if (container.has(key1, PersistentDataType.STRING)) {
            return container.get(key1, PersistentDataType.STRING);
        } else if (container.has(key2, PersistentDataType.STRING)) {
            return container.get(key2, PersistentDataType.STRING);
        }
        return null;
    }

    private BufferedImage getSkin(String link) {
        try {
            java.io.File skinFolder = new java.io.File(plugin.getDataFolder(), "skins");
            java.io.File localFile = new java.io.File(skinFolder, link);
            if (!localFile.exists() && !link.endsWith(".png")) {
                localFile = new java.io.File(skinFolder, link + ".png");
            }
            if (localFile.exists()) {
                return javax.imageio.ImageIO.read(localFile);
            }

            java.net.URL url = new java.net.URL(link);
            return javax.imageio.ImageIO.read(url);
        } catch (Exception e) {
            return null;
        }
    }

}


