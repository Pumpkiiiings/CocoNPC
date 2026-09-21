package com.pumpkings.coconpc.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.clip.placeholderapi.PlaceholderAPI;
import com.pumpkings.coconpc.CocoNPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;


public class Utils {
   CocoNPC plugin;

   public Utils(CocoNPC plugin) {
      this.plugin = plugin;
   }

   public void sendMessage(CommandSender sender, String text) {
      sender.sendMessage(this.component(text));
   }




   public Component component(String text) {
      return MiniMessage.miniMessage().deserialize(text);
   }

   public Component itemComponent(String text) {
      if (text == null || text.isEmpty()) return Component.empty();
      return MiniMessage.miniMessage().deserialize(text)
              .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
   }

   public org.bukkit.Color parseBackgroundColor(String colorStr) {
      if (colorStr == null || colorStr.equalsIgnoreCase("transparent") || colorStr.equalsIgnoreCase("none")) {
         return org.bukkit.Color.fromARGB(0, 0, 0, 0);
      }
      if (colorStr.equalsIgnoreCase("default") || colorStr.equalsIgnoreCase("black") || colorStr.equalsIgnoreCase("dark")) {
         return org.bukkit.Color.fromARGB(64, 0, 0, 0);
      }
      try {
         String hex = colorStr.startsWith("#") ? colorStr.substring(1) : colorStr;
         if (hex.length() == 8) {
            long val = Long.parseLong(hex, 16);
            int a = (int) ((val >> 24) & 0xFF);
            int r = (int) ((val >> 16) & 0xFF);
            int g = (int) ((val >> 8) & 0xFF);
            int b = (int) (val & 0xFF);
            return org.bukkit.Color.fromARGB(a, r, g, b);
         } else if (hex.length() == 6) {
            int rgb = Integer.parseInt(hex, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return org.bukkit.Color.fromRGB(r, g, b);
         }
      } catch (Exception ignored) {}
      return org.bukkit.Color.fromARGB(0, 0, 0, 0);
   }

   public Component component(String text, Player player) {
      if (this.plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         text = PlaceholderAPI.setPlaceholders(player, text);
      }
      return this.component(text);
   }

   public List<Component> component(List<String> list, Player player) {
      List<Component> nlist = new ArrayList<>();
      for (String text : list) {
         nlist.add(player == null ? this.component(text) : this.component(text, player));
      }
      return nlist;
   }

   public List<Component> component(List<String> list) {
      return this.component(list, null);
   }

   public String color(String text) {
      return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
   }

   public String color(String text, Player player) {
      return this.plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? PlaceholderAPI.setPlaceholders(player, this.color(text)) : this.color(text);
   }

   public String expandForCommand(String text, Player player) {
      String expanded = this.color(text, player);

      StringBuilder cleaned = new StringBuilder(expanded.length());
      boolean stripped = false;
      List<String> blocked = this.plugin.getConfigManager().getConsoleBlockedCharacters();

      for (int i = 0; i < expanded.length(); i++) {
         char c = expanded.charAt(i);
         if (c != '\t' && Character.isISOControl(c)) {
            stripped = true;
            continue;
         }
         if (blocked.contains(String.valueOf(c))) {
            stripped = true;
            continue;
         }
         cleaned.append(c);
      }

      if (stripped) {
         this.plugin.getLogger().warning("[CocoNPC] Stripped blocked characters from a command run for "
                 + player.getName() + ". Original: \"" + expanded + "\"");
      }
      return cleaned.toString();
   }

   public String sanitizeCommandArgument(String value) {
      if (value == null) return "";
      return value.replaceAll("\\s+", "");
   }

   public List<String> color(List<String> list, Player player) {
      List<String> nlist = new ArrayList<>();
      for(String text : list) {
         nlist.add(player == null ? this.color(text) : this.color(text, player));
      }
      return nlist;
   }

   public List<String> color(List<String> list) {
      return this.color(list, null);
   }
   private boolean floodgateResolved = false;
   private Object floodgateApi = null;
   private java.lang.reflect.Method floodgateIsPlayer = null;

   public boolean isFloodgatePlayer(UUID uuid) {
      if (!floodgateResolved) {
         resolveFloodgate();
      }
      if (floodgateIsPlayer == null) return false;

      try {
         return (boolean) floodgateIsPlayer.invoke(floodgateApi, uuid);
      } catch (Throwable ignored) {
         return false;
      }
   }

   private synchronized void resolveFloodgate() {
      if (floodgateResolved) return;
      floodgateResolved = true;

      if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) return;
      try {
         Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
         this.floodgateApi = apiClass.getMethod("getInstance").invoke(null);
         this.floodgateIsPlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
         this.plugin.getLogger().info("Floodgate detected — Bedrock players will be handled separately.");
      } catch (Throwable ignored) {
         this.floodgateApi = null;
         this.floodgateIsPlayer = null;
      }
   }

   public void resetFloodgateCache() {
      this.floodgateResolved = false;
      this.floodgateApi = null;
      this.floodgateIsPlayer = null;
   }
}


