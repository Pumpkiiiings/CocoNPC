package com.pumpkings.coconpc.util;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcSkin;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineskin.ClientBuilder;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.data.JobStatus;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.JobResponse;
import org.mineskin.response.QueueResponse;

public class MineskinService {
   private static final int PART_COUNT = 11;

   private static final int REQUEST_TIMEOUT_SECONDS = 60;
   private static final long JOB_POLL_INTERVAL_MS = 2000L;
   private static final int MAX_JOB_POLLS = 60;
   private static final int UPLOAD_ATTEMPTS = 3;
   private static final long RETRY_BACKOFF_MS = 3000L;

   private final java.util.concurrent.ExecutorService uploadExecutor =
           java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
              Thread thread = new Thread(runnable, "CocoNPC-SkinUpload");
              thread.setDaemon(true);
              return thread;
           });

   CocoNPC plugin;
   public Map<String, NpcSkin> skins = new HashMap();
   MineSkinClient client;
   private final Map<java.util.UUID, Integer> upload_progress = new java.util.concurrent.ConcurrentHashMap<>();

   public MineskinService(CocoNPC plugin) {
      this.plugin = plugin;
      rebuildClient();
   }

   public void rebuildClient() {
      String userAgent = plugin.getConfigManager().getMineskinUserAgent();
      String apiKey = plugin.getConfigManager().getMineskinApiKey();

      ClientBuilder builder = MineSkinClient.builder()
              .userAgent(userAgent)
              .requestHandler(JsoupRequestHandler::new);

      if (apiKey == null) {
         plugin.getLogger().warning("No MineSkin API key set. Skin generation will fail — "
                 + "get a key at https://mineskin.org/account and apply it with /coconpc setkey <key>.");
      } else {
         builder.apiKey(apiKey);
      }

      this.client = builder.build();
   }

   public boolean hasApiKey() {
      return plugin.getConfigManager().getMineskinApiKey() != null;
   }

   public CompletableFuture<Map<String, String>> uploadSkins(Player player, BufferedImage baseskin, String texture) {
      CompletableFuture<Map<String, String>> resultFuture = new CompletableFuture<>();

      if (!hasApiKey()) {
         resultFuture.completeExceptionally(new IllegalStateException(
                 "No MineSkin API key configured. Set one with /coconpc setkey <key>."));
         return resultFuture;
      }
      Map<String, BufferedImage> parts = cropAllParts(baseskin);

      CompletableFuture.runAsync(() -> {
         Map<String, String> urlMap = new HashMap<>();
         try {
            for (Map.Entry<String, BufferedImage> entry : parts.entrySet()) {
               if (player != null) player.sendMessage(this.getProgressMessage(player));
               urlMap.put(entry.getKey(), uploadOne(entry.getKey(), entry.getValue()));
            }
            if (player != null) this.upload_progress.remove(player.getUniqueId());
            resultFuture.complete(urlMap);
         } catch (Exception ex) {
            if (player != null) this.upload_progress.remove(player.getUniqueId());
            resultFuture.completeExceptionally(ex);
         }
      }, uploadExecutor);

      return resultFuture;
   }

   public void shutdown() {
      uploadExecutor.shutdownNow();
   }

   private String uploadOne(String partName, BufferedImage image) throws Exception {
      Exception lastFailure = null;

      for (int attempt = 1; attempt <= UPLOAD_ATTEMPTS; attempt++) {
         try {
            return submitAndPoll(partName, image);
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
         } catch (Exception ex) {
            lastFailure = ex;
            if (attempt < UPLOAD_ATTEMPTS) {
               plugin.getLogger().warning("Upload of " + partName + " failed (attempt "
                       + attempt + "/" + UPLOAD_ATTEMPTS + "): " + ex.getMessage() + " — retrying.");
               Thread.sleep(RETRY_BACKOFF_MS * attempt);
            }
         }
      }
      throw new IllegalStateException(
              "Upload of " + partName + " failed after " + UPLOAD_ATTEMPTS + " attempts: "
                      + lastFailure.getMessage(), lastFailure);
   }

   private String submitAndPoll(String partName, BufferedImage image) throws Exception {
      QueueResponse queued = this.client.queue()
              .submit(GenerateRequest.upload((RenderedImage) image))
              .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      String jobId = queued.getJob().id();

      for (int poll = 0; poll < MAX_JOB_POLLS; poll++) {
         JobResponse jobResponse = this.client.queue()
                 .get(jobId)
                 .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
         JobStatus status = jobResponse.getJob().status();

         if (status == JobStatus.COMPLETED) {
            return jobResponse.getSkin()
                    .orElseThrow(() -> new IllegalStateException(
                            "MineSkin reported job " + jobId + " complete but returned no skin"))
                    .texture().url().skin();
         }
         if (status == JobStatus.FAILED) {
            throw new IllegalStateException("MineSkin job " + jobId + " for " + partName
                    + " failed — " + describe(jobResponse));
         }

         Thread.sleep(JOB_POLL_INTERVAL_MS);
      }

      throw new IllegalStateException("MineSkin job " + jobId + " for " + partName
              + " did not finish within " + (MAX_JOB_POLLS * JOB_POLL_INTERVAL_MS / 1000) + "s");
   }

   private static String describe(JobResponse response) {
      StringBuilder detail = new StringBuilder();
      response.getErrorOrMessage().ifPresent(
              m -> detail.append(m.code()).append(": ").append(m.message()));
      if (detail.isEmpty() && !response.getWarnings().isEmpty()) {
         detail.append("warnings=").append(response.getWarnings());
      }
      if (detail.isEmpty()) {
         detail.append("no reason given (HTTP ").append(response.getStatus()).append(')');
      }
      return detail.toString();
   }

   public void preloadSkins(org.bukkit.command.CommandSender sender) {
      java.io.File skinFolder = new java.io.File(plugin.getDataFolder(), "skins");
      if (!skinFolder.exists()) {
          skinFolder.mkdirs();
      }

      java.io.File[] files = skinFolder.listFiles();
      if (files == null) return;

      if (sender != null) sender.sendMessage(plugin.getUtils().component("<yellow>Starting background preload for local skins..."));

      java.util.List<java.io.File> missing = new java.util.ArrayList<>();
      for (java.io.File file : files) {
          if (!file.getName().endsWith(".png")) continue;
          String code = SkinUtils.urlToBase64(file.getName());
          if (!plugin.getConfigManager().skins.contains(code)) {
              missing.add(file);
          }
      }

      org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> {
          int count = 0;
          for (java.io.File file : missing) {
              String text = file.getName();
              String code = SkinUtils.urlToBase64(text);
              try {
                  BufferedImage baseskin = javax.imageio.ImageIO.read(file);
                  if (baseskin == null) continue;

                  if (isSlim(baseskin)) {
                      fixSlim(baseskin);
                  }

                  plugin.getLogger().info("Preloading skin: " + text);
                  Map<String, String> urlMap = uploadSkins(null, baseskin, text).join();
                  com.pumpkings.coconpc.core.npc.NpcSkin skin = plugin.getRegistry().buildSkin(code, urlMap);

                  org.bukkit.Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                      plugin.getConfigManager().skins.setSkin(code, skin);
                      plugin.getConfigManager().skins.saveConfig();
                  });
                  plugin.getLogger().info("Preloaded skin " + text + " successfully.");
                  count++;
              } catch (Exception ex) {
                  plugin.getLogger().warning("Failed to preload skin " + text + ": " + ex.getMessage());
              }
          }

          final int total = count;
          if (sender != null) {
              org.bukkit.Bukkit.getGlobalRegionScheduler().run(plugin, t ->
                  Message.SKIN_PRELOAD_FINISHED.send(plugin, sender, "{amount}", String.valueOf(total)));
          }
      });
   }

   private record Face(int srcX, int srcY, int srcW, int srcH, int dstX, int dstY) {}

   private record CropSpec(String name, Face[] base, Face[] overlay) {}

   private static final CropSpec[] CROP_SPECS = {
       new CropSpec("torsoUpper", new Face[]{
           new Face(20,20,8,8, 8,8), new Face(32,20,8,8, 24,8), new Face(20,16,8,4, 8,0),
           new Face(16,20,4,8, 0,8), new Face(28,20,4,8, 16,8)
       }, new Face[]{
           new Face(20,36,8,8, 40,8), new Face(32,36,8,8, 56,8), new Face(20,32,8,4, 40,0),
           new Face(16,36,4,8, 32,8), new Face(28,36,4,8, 48,8)
       }),
       new CropSpec("torsoLower", new Face[]{
           new Face(20,28,8,4, 8,8), new Face(32,28,8,4, 24,8), new Face(16,28,4,4, 0,8),
           new Face(28,28,4,4, 16,8), new Face(28,16,8,4, 16,0)
       }, new Face[]{
           new Face(20,44,8,4, 40,8), new Face(32,44,8,4, 56,8), new Face(16,44,4,4, 32,8),
           new Face(28,44,4,4, 48,8), new Face(28,32,8,4, 48,0)
       }),
       new CropSpec("rightArmUpper", new Face[]{
           new Face(44,20,4,8, 8,8), new Face(52,20,4,8, 24,8), new Face(44,16,4,4, 8,0),
           new Face(40,20,4,8, 0,8), new Face(48,20,4,8, 16,8)
       }, new Face[]{
           new Face(44,36,4,8, 40,8), new Face(52,36,4,8, 56,8), new Face(44,32,4,4, 40,0),
           new Face(40,36,4,8, 32,8), new Face(48,36,4,8, 48,8)
       }),
       new CropSpec("rightArmLower", new Face[]{
           new Face(44,28,4,4, 8,8), new Face(52,28,4,4, 24,8), new Face(40,28,4,4, 0,8),
           new Face(48,28,4,4, 16,8), new Face(48,16,4,4, 16,0)
       }, new Face[]{
           new Face(44,44,4,4, 40,8), new Face(52,44,4,4, 56,8), new Face(40,44,4,4, 32,8),
           new Face(48,44,4,4, 48,8), new Face(48,32,4,4, 48,0)
       }),
       new CropSpec("leftArmUpper", new Face[]{
           new Face(36,52,4,8, 8,8), new Face(44,52,4,8, 24,8), new Face(36,48,4,4, 8,0),
           new Face(32,52,4,8, 0,8), new Face(40,52,4,8, 16,8)
       }, new Face[]{
           new Face(52,52,4,8, 40,8), new Face(60,52,4,8, 56,8), new Face(52,48,4,4, 40,0),
           new Face(48,52,4,8, 32,8), new Face(56,52,4,8, 48,8)
       }),
       new CropSpec("leftArmLower", new Face[]{
           new Face(36,60,4,4, 8,8), new Face(44,60,4,4, 24,8), new Face(32,60,4,4, 0,8),
           new Face(40,60,4,4, 16,8), new Face(40,48,4,4, 16,0)
       }, new Face[]{
           new Face(52,60,4,4, 40,8), new Face(60,60,4,4, 56,8), new Face(48,60,4,4, 32,8),
           new Face(56,60,4,4, 48,8), new Face(56,48,4,4, 48,0)
       }),
       new CropSpec("rightLegUpper", new Face[]{
           new Face(4,20,4,8, 8,8), new Face(12,20,4,8, 24,8), new Face(4,16,4,4, 8,0),
           new Face(0,20,4,8, 0,8), new Face(8,20,4,8, 16,8)
       }, new Face[]{
           new Face(4,36,4,8, 40,8), new Face(12,36,4,8, 56,8), new Face(4,32,4,4, 40,0),
           new Face(0,36,4,8, 32,8), new Face(8,36,4,8, 48,8)
       }),
       new CropSpec("rightLegLower", new Face[]{
           new Face(4,28,4,4, 8,8), new Face(12,28,4,4, 24,8), new Face(0,28,4,4, 0,8),
           new Face(8,28,4,4, 16,8), new Face(8,16,4,4, 16,0)
       }, new Face[]{
           new Face(4,44,4,4, 40,8), new Face(8,32,4,4, 56,8), new Face(0,44,4,4, 32,8),
           new Face(8,44,4,4, 48,8), new Face(8,32,4,4, 48,0)
       }),
       new CropSpec("leftLegUpper", new Face[]{
           new Face(20,52,4,8, 8,8), new Face(28,52,4,8, 24,8), new Face(20,48,4,4, 8,0),
           new Face(16,52,4,8, 0,8), new Face(24,52,4,8, 16,8)
       }, new Face[]{
           new Face(4,52,4,8, 40,8), new Face(12,52,4,8, 56,8), new Face(4,48,4,4, 40,0),
           new Face(0,52,4,8, 32,8), new Face(8,52,4,8, 48,8)
       }),
       new CropSpec("leftLegLower", new Face[]{
           new Face(20,60,4,4, 8,8), new Face(28,60,4,4, 24,8), new Face(16,60,4,4, 0,8),
           new Face(24,60,4,4, 16,8), new Face(24,48,4,4, 16,0)
       }, new Face[]{
           new Face(4,60,4,4, 40,8), new Face(12,60,4,4, 56,8), new Face(0,60,4,4, 32,8),
           new Face(8,60,4,4, 48,8), new Face(8,48,4,4, 48,0)
       }),
   };

   public static Map<String, BufferedImage> cropAllParts(BufferedImage skin) {
      Map<String, BufferedImage> result = new java.util.LinkedHashMap<>();
      result.put("head", skin);
      for (CropSpec spec : CROP_SPECS) {
         result.put(spec.name(), cropPart(skin, spec));
      }
      return result;
   }

   private static BufferedImage cropPart(BufferedImage skin, CropSpec spec) {
      BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = canvas.createGraphics();
      for (Face f : spec.base()) {
         g.drawImage(cropRegionAndResize(skin, new Integer[]{f.srcX(), f.srcY(), f.srcW(), f.srcH()}, 8, 8),
                     (BufferedImageOp) null, f.dstX(), f.dstY());
      }
      for (Face f : spec.overlay()) {
         g.drawImage(cropRegionAndResize(skin, new Integer[]{f.srcX(), f.srcY(), f.srcW(), f.srcH()}, 8, 8),
                     (BufferedImageOp) null, f.dstX(), f.dstY());
      }
      g.dispose();
      return canvas;
   }



   public static BufferedImage cropRegionAndResize(BufferedImage base, Integer[] offset, int targetWidth, int targetHeight) {
      int x = offset[0];
      int y = offset[1];
      int w = offset[2];
      int h = offset[3];
      BufferedImage cropped = base.getSubimage(x, y, w, h);
      return resizeImage(cropped, targetWidth, targetHeight);
   }

   public static BufferedImage resizeImage(BufferedImage src, int width, int height) {
      BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = resized.createGraphics();
      g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(src, 0, 0, width, height, null);
      g.dispose();
      return resized;
   }

   public static boolean isSlim(BufferedImage skin) {
      if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 32) {
         return false;
      }
      return skin.getRGB(54, 20) == 0 && skin.getRGB(50, 16) == 0;
   }

   public static void fixSlim(BufferedImage image) {
      if (image == null || image.getWidth() < 64) return;
      Graphics2D g = image.createGraphics();
      
      convertArmToClassic(g, 40, 16);
      if (image.getHeight() >= 48) convertArmToClassic(g, 40, 32);
      if (image.getHeight() >= 64) {
         convertArmToClassic(g, 32, 48);
         convertArmToClassic(g, 48, 48);
      }
      
      g.dispose();
   }

   private static void convertArmToClassic(Graphics2D g, int x, int y) {
      // Back face: move right by 1, then duplicate right-most pixel to widen from 3 to 4
      g.copyArea(x + 11, y + 4, 3, 12, 1, 0);
      g.copyArea(x + 14, y + 4, 1, 12, 1, 0);

      // Inner face: just move right by 1 (it's already 4 wide)
      g.copyArea(x + 7, y + 4, 4, 12, 1, 0);

      // Front face: duplicate right-most pixel to widen from 3 to 4 (no shift needed)
      g.copyArea(x + 6, y + 4, 1, 12, 1, 0);

      // Top face: duplicate right-most pixel to widen from 3 to 4
      g.copyArea(x + 6, y, 1, 4, 1, 0);

      // Bottom face: move right by 1, then duplicate right-most pixel to widen from 3 to 4
      g.copyArea(x + 7, y, 3, 4, 1, 0);
      g.copyArea(x + 10, y, 1, 4, 1, 0);
   }

   private Component getProgressMessage(Player player) {
      int imageNumber = this.upload_progress.getOrDefault(player.getUniqueId(), 0);
      int percentage = (int)((double)imageNumber / (double) PART_COUNT * 100.0D);
      String colorCode = "<green>";
      if (percentage < 25) {
         colorCode = "<red>";
      } else if (percentage < 50) {
         colorCode = "<gold>";
      } else if (percentage < 75) {
         colorCode = "<yellow>";
      }

      this.upload_progress.put(player.getUniqueId(), imageNumber + 1);
      return this.plugin.getUtils().component(Message.SKIN_PROGRESS.get(this.plugin, "{color}", colorCode, "{progress}", String.valueOf(percentage)));
   }
}



