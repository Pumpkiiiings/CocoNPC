package com.pumpkings.coconpc.core.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.pumpkings.coconpc.core.npc.NpcSkin;

public class PluginConfig {
   private File configFile;
   private FileConfiguration config;
   private Boolean hasDefault;
   private JavaPlugin plugin;

   public PluginConfig(String path, JavaPlugin plugin) {
      this.plugin = plugin;
      this.configFile = new File(plugin.getDataFolder() + File.separator + path);
      this.config = YamlConfiguration.loadConfiguration(this.configFile);
      if (!this.configFile.exists()) {
         try {
            plugin.saveResource(path, false);
            this.config = YamlConfiguration.loadConfiguration(this.configFile);
            this.hasDefault = true;
         } catch (Exception var4) {
         }
      } else {
         this.hasDefault = false;
      }

   }

   public Object get(String path, Object def) {
      return this.config.get(path, def);
   }

   public Object get(String path) {
      return this.config.get(path);
   }

   public Object get(String path, Class<?> clazz) {
      return this.config.get(path, clazz);
   }

   public String getString(String path, String def) {
      return this.config.getString(path, def);
   }

   public int getInt(String path, int def) {
      return this.config.getInt(path, def);
   }

   public double getDouble(String path, double def) {
      return this.config.getDouble(path, def);
   }

   public List<String> getStringList(String path, List<String> def) {
      return this.config.contains(path) ? this.config.getStringList(path) : def;
   }

   public ConfigurationSection getConfigurationSection(String path) {
      return this.config.getConfigurationSection(path);
   }

   public Boolean isConfigurationSection(String path) {
      return this.config.isConfigurationSection(path);
   }

   public Boolean getBoolean(String path, Boolean def) {
      return this.config.getBoolean(path, def);
   }

   public String getString(String path) {
      return this.config.getString(path);
   }

   public int getInt(String path) {
      return this.config.getInt(path);
   }

   public double getDouble(String path) {
      return this.config.getDouble(path);
   }

   public Boolean getBoolean(String path) {
      return this.config.getBoolean(path);
   }

   public List<String> getStringList(String path) {
      return this.config.getStringList(path);
   }

   public void set(String path, Object object) {
      this.config.set(path, object);
   }

   public Boolean contains(String paht) {
      return this.config.contains(paht);
   }

   public void setSkin(String path, NpcSkin skin) {
      this.config.set(path + ".key", skin.getKey());
      for (Map.Entry<String, String> entry : skin.getAll().entrySet()) {
         this.config.set(path + "." + entry.getKey(), entry.getValue());
      }
   }

   public NpcSkin getSkin(String path) {
      String key = this.config.getString(path + ".key");
      NpcSkin skin = new NpcSkin((CocoNPC)this.plugin, key);
      
      org.bukkit.configuration.ConfigurationSection section = this.config.getConfigurationSection(path);
      if (section != null) {
         boolean migrated = false;
         for (String partKey : section.getKeys(false)) {
            if (partKey.equals("key")) continue;
            
            String newKey = mapLegacySkinPart(partKey);
            if (!newKey.equals(partKey)) {
                skin.set(newKey, section.getString(partKey));
                this.config.set(path + "." + partKey, null);
                this.config.set(path + "." + newKey, section.getString(partKey));
                migrated = true;
            } else {
                skin.set(partKey, section.getString(partKey));
            }
         }
         if (migrated) {
             saveConfig();
         }
      }
      return skin;
   }

   private String mapLegacySkinPart(String key) {
       return switch (key) {
           case "body1" -> "torsoUpper";
           case "body2" -> "torsoLower";
           case "right_arm1" -> "rightArmUpper";
           case "right_arm2" -> "rightArmLower";
           case "left_arm1" -> "leftArmUpper";
           case "left_arm2" -> "leftArmLower";
           case "right_leg1" -> "rightLegUpper";
           case "right_leg2" -> "rightLegLower";
           case "left_leg1" -> "leftLegUpper";
           case "left_leg2" -> "leftLegLower";
           default -> key;
       };
   }

   public int mergeDefaults(String resourcePath) {
      try (java.io.InputStream in = this.plugin.getResource(resourcePath)) {
         if (in == null) return 0;

         YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                 new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));

         int added = 0;
         for (String key : bundled.getKeys(true)) {
            if (bundled.isConfigurationSection(key)) continue;
            if (this.config.contains(key)) continue;

            this.config.set(key, bundled.get(key));
            this.config.setComments(key, bundled.getComments(key));
            added++;
         }

         if (added > 0) {
            for (String key : bundled.getKeys(true)) {
               if (bundled.isConfigurationSection(key) && this.config.isConfigurationSection(key)
                       && this.config.getComments(key).isEmpty()) {
                  this.config.setComments(key, bundled.getComments(key));
               }
            }
            this.saveConfig();
         }
         return added;
      } catch (Exception ex) {
         this.plugin.getLogger().warning("Could not merge defaults into " + resourcePath + ": " + ex.getMessage());
         return 0;
      }
   }

   public Boolean hasDefault() {
      return this.hasDefault;
   }

   public FileConfiguration getConfig() {
      return this.config;
   }

   public void saveConfig() {
      try {
         this.config.save(this.configFile);
      } catch (IOException ex) {
         ex.printStackTrace();
      }

   }
}


