package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.mob.model.BbModel;
import com.pumpkings.coconpc.core.mob.model.MobTextureGenerator;
import com.pumpkings.coconpc.core.mob.model.ModelRig;
import org.bukkit.Bukkit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prepares the per-cube visuals of every custom mob definition. Composing the
 * skins is CPU work, and MineSkin uploads are network work, so everything runs
 * off the server thread and only the final refresh is scheduled back onto the
 * owning region.
 */
public final class MobTextureService {
    private final CocoNPC plugin;
    private final MobSkinCache cache;
    private final ExecutorService executor;
    private final Map<String, CompletableFuture<Void>> futures = new ConcurrentHashMap<>();
    private final Map<String, String> errors = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public MobTextureService(CocoNPC plugin) {
        this.plugin = plugin;
        this.cache = new MobSkinCache(new File(plugin.getDataFolder(), "mob-skins.yml"));
        AtomicInteger threadNumber = new AtomicInteger();
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable,
                    "CocoNPC-MobTextures-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    public void loadCache() {
        cache.load();
    }

    public void reload(List<MobDefinition> definitions) {
        futures.clear();
        errors.clear();
        if (definitions == null) return;
        for (MobDefinition definition : definitions) {
            generate(definition);
        }
    }

    public CompletableFuture<Void> generate(MobDefinition definition) {
        if (definition == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Missing mob definition"));
        }
        if (closed) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Texture service is shutting down"));
        }

        CompletableFuture<Void> existing = futures.get(definition.getId());
        if (existing != null) return existing;

        CompletableFuture<Void> created = new CompletableFuture<>();
        CompletableFuture<Void> race = futures.putIfAbsent(definition.getId(), created);
        if (race != null) return race;

        executor.execute(() -> generateNow(definition, created));
        return created;
    }

    public CompletableFuture<Void> whenReady(String definitionId) {
        MobDefinition definition = plugin.getMobRegistry() != null
                ? plugin.getMobRegistry().getDefinition(definitionId)
                : null;
        if (definition == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unknown mob definition: " + definitionId));
        }

        CompletableFuture<Void> future = futures.get(definitionId);
        return future != null ? future : generate(definition);
    }

    public boolean isReady(String definitionId) {
        CompletableFuture<Void> future = definitionId == null ? null : futures.get(definitionId);
        return future != null && future.isDone() && !future.isCompletedExceptionally();
    }

    public boolean isGenerating(String definitionId) {
        CompletableFuture<Void> future = definitionId == null ? null : futures.get(definitionId);
        return future != null && !future.isDone();
    }

    public String getError(String definitionId) {
        return definitionId == null ? null : errors.get(definitionId);
    }

    public void shutdown() {
        closed = true;
        executor.shutdownNow();
    }

    private void generateNow(MobDefinition definition, CompletableFuture<Void> future) {
        long started = System.currentTimeMillis();
        String definitionId = definition.getId();
        List<CubeRef> cubes = collectCubes(definition);
        definition.getCubeVisuals().clear();

        if (cubes.isEmpty()) {
            plugin.getLogger().info("[CocoNPC] Mob '" + definitionId
                    + "' has no cubes to texturize.");
            complete(definition, future, started, null);
            return;
        }

        String problem = null;
        try {
            Map<String, BufferedImage> atlases = MobTextureGenerator.loadAtlases(
                    definition.getModel(), plugin.getLogger());

            boolean skins = definition.getTextureMode() == MobTextureMode.SKINS;
            boolean hasApiKey = plugin.getMineskinService() != null
                    && plugin.getMineskinService().hasApiKey();

            if (skins && !hasApiKey) {
                problem = "No MineSkin API key; using blocks instead.";
                plugin.getLogger().warning("[CocoNPC] Mob '" + definitionId
                        + "': no MineSkin API key configured. Falling back to blocks. "
                        + "Set one with /coconpc setkey <key> and reload.");
                skins = false;
            }
            if (skins && atlases.isEmpty()) {
                problem = "The .bbmodel has no readable textures; using blocks instead.";
                plugin.getLogger().warning("[CocoNPC] Mob '" + definitionId
                        + "': no readable textures were found. Falling back to blocks.");
                skins = false;
            }

            plugin.getLogger().info("[CocoNPC] Preparing " + cubes.size() + " cube texture(s) for mob '"
                    + definitionId + "' using " + (skins ? "player-head skins" : "concrete blocks")
                    + ".");

            int generated = 0;
            int cached = 0;
            int fallbacks = 0;
            int index = 0;

            for (CubeRef ref : cubes) {
                if (closed) {
                    throw new IllegalStateException("Plugin disabled while generating textures");
                }
                index++;

                BoneVisual visual = null;
                boolean fellBack = false;
                if (skins) {
                    BufferedImage image = MobTextureGenerator.composeSkin(
                            definition.getModel(), ref.cube, atlases);
                    int visiblePixels = countVisiblePixels(image);

                    if (visiblePixels > 0) {
                        String hash = MobTextureGenerator.sha256(image);
                        String url = cache.get(hash);

                        if (url == null) {
                            String label = definitionId + "-" + ref.key;
                            plugin.getLogger().info("[CocoNPC] Uploading cube skin " + index
                                    + "/" + cubes.size() + " of mob '" + definitionId + "'...");
                            try {
                                url = plugin.getMineskinService()
                                        .uploadSkinImage(label, image)
                                        .join();
                                cache.put(hash, url);
                                generated++;
                            } catch (Exception exception) {
                                fellBack = true;
                                problem = "MineSkin upload failed: " + exception.getMessage();
                                plugin.getLogger().warning("[CocoNPC] Could not generate skin for "
                                        + label + ": " + exception.getMessage()
                                        + " — using a concrete block for this cube.");
                            }
                        } else {
                            cached++;
                        }

                        if (url != null && !url.isBlank()) {
                            visual = BoneVisual.head(url);
                        }
                    }
                }

                if (visual == null) {
                    fellBack = true;
                    int average = atlases.isEmpty()
                            ? 0xFFFFFF
                            : MobTextureGenerator.averageColor(definition.getModel(), ref.cube, atlases);
                    visual = BoneVisual.block(MobTextureGenerator.closestConcrete(average));
                }
                if (fellBack) {
                    fallbacks++;
                }

                definition.getCubeVisuals().put(ref.key, visual);
            }

            cache.save();
            plugin.getLogger().info("[CocoNPC] Mob '" + definitionId + "' textures ready in "
                    + (System.currentTimeMillis() - started) + " ms (uploads=" + generated
                    + ", cached=" + cached + ", fallbacks=" + fallbacks + ").");
        } catch (Exception exception) {
            problem = exception.getMessage();
            plugin.getLogger().warning("[CocoNPC] Texture generation for mob '" + definitionId
                    + "' failed: " + exception.getMessage());
            fillBlockFallback(definition, cubes);
        }

        complete(definition, future, started, problem);
    }

    private void fillBlockFallback(MobDefinition definition, List<CubeRef> cubes) {
        definition.getCubeVisuals().clear();
        for (CubeRef ref : cubes) {
            definition.getCubeVisuals().put(ref.key,
                    BoneVisual.block(MobTextureGenerator.closestConcrete(0xFFFFFF)));
        }
    }

    private void complete(MobDefinition definition, CompletableFuture<Void> future,
                          long started, String problem) {
        if (problem != null) {
            errors.put(definition.getId(), problem);
        }

        Runnable refresh = () -> {
            if (closed) {
                future.complete(null);
                return;
            }
            try {
                if (plugin.getMobRegistry() != null) {
                    plugin.getMobRegistry().refreshVisuals(definition.getId());
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("[CocoNPC] Could not refresh mob '"
                        + definition.getId() + "' after texturing: " + exception.getMessage());
            } finally {
                future.complete(null);
            }
        };

        if (closed) {
            future.complete(null);
            return;
        }

        try {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> refresh.run());
        } catch (RuntimeException exception) {
            // The server may already be shutting down; complete without refresh.
            future.complete(null);
        }
    }

    private List<CubeRef> collectCubes(MobDefinition definition) {
        List<CubeRef> cubes = new ArrayList<>();
        BbModel model = definition.getModel();
        if (model == null) return cubes;

        for (BbModel.Bone bone : model.getBones()) {
            int index = 0;
            for (BbModel.Cube cube : bone.getCubes()) {
                String key = ModelRig.cubeKey(bone, cube, index);
                cubes.add(new CubeRef(key, bone, cube));
                index++;
            }
        }
        return cubes;
    }

    private static int countVisiblePixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) >= 16) count++;
            }
        }
        return count;
    }

    private record CubeRef(String key, BbModel.Bone bone, BbModel.Cube cube) {
    }
}
