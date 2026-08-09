package com.pumpkings.coconpc;


import com.pumpkings.coconpc.command.NpcCommand;
import com.pumpkings.coconpc.core.animation.AnimationManager;
import com.pumpkings.coconpc.core.animation.AnimatorTask;
import com.pumpkings.coconpc.util.MojangApi;
import com.pumpkings.coconpc.util.ServerVersionInfo;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import com.pumpkings.coconpc.listener.ConnectionListener;
import com.pumpkings.coconpc.listener.NpcInteractListener;
import com.pumpkings.coconpc.listener.WorldLoadListener;
import com.pumpkings.coconpc.action.ActionsManager;
import com.pumpkings.coconpc.core.config.ConfigManager;
import com.pumpkings.coconpc.core.npc.registry.NpcRegistry;
import com.pumpkings.coconpc.core.npc.selection.SelectionManager;
import com.pumpkings.coconpc.util.MineskinService;
import com.pumpkings.coconpc.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class CocoNPC extends JavaPlugin {
   NpcRegistry registry;
   SelectionManager selectionManager;
   MineskinService mineskinService;
   ConfigManager configmanager;
   ActionsManager actionsmanager;
   com.pumpkings.coconpc.action.CooldownManager cooldownManager;
   Utils utils;
   MojangApi mojangApi;
   com.pumpkings.coconpc.core.packet.CocoPacketEngine packetEngine;
   AnimationManager animationManager;
   public boolean hasNewUpdate = false;

   public void onEnable() {
      this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
      this.initClass();
      this.loadConfigs();
      this.mineskinService.preloadSkins(null);
      this.initCommands();
      this.initListeners();
      this.packetEngine.init();
      this.animationManager.loadAnimations();
      new AnimatorTask(this).runTaskTimer(this, 1L, 1L);
      this.registry.loadAllPersistedNpcs();

      for (World world : Bukkit.getWorlds()) {
         for (Chunk chunk : world.getLoadedChunks()) {
            this.registry.loadChunk(chunk);
         }
      }

      new ServerVersionInfo(this);
   }

   @Override
   public void onDisable() {
      if (this.registry != null && this.registry.getNpcs() != null) {
         for (com.pumpkings.coconpc.core.npc.NpcEntity npc : this.registry.getNpcs().values()) {
            npc.destroyForAll();
         }
      }
      if (this.packetEngine != null) {
         this.packetEngine.shutdown();
      }
      if (this.cooldownManager != null) {
         this.cooldownManager.flush();
      }
      if (this.mineskinService != null) {
         this.mineskinService.shutdown();
      }
   }

   public void initClass() {
      this.configmanager = new ConfigManager(this);
      this.configmanager.loadMainConfig();
      this.registry = new NpcRegistry(this);
      this.selectionManager = new SelectionManager(this);
      this.mineskinService = new MineskinService(this);
      this.utils = new Utils(this);
      this.mojangApi = new MojangApi();
      this.cooldownManager = new com.pumpkings.coconpc.action.CooldownManager(this);
      this.actionsmanager = new ActionsManager(this);
      this.animationManager = new AnimationManager(this);
      this.packetEngine = new com.pumpkings.coconpc.core.packet.CocoPacketEngine(this);
   }

   public void initCommands() {
      this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, new io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler<>() {
         @Override
         public void run(io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent<io.papermc.paper.command.brigadier.Commands> event) {
            event.registrar().register("coconpc", "Main command for CocoNPC", List.of("npc"), new NpcCommand(CocoNPC.this));
         }
      });
   }

   public void initListeners() {
      this.getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
      this.getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
      this.getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
   }

   public void loadConfigs() {
      this.configmanager.loadMainConfig();
      this.configmanager.loadSkins();
      this.configmanager.loadNpc();
      this.configmanager.loadNpcData();
   }

   public void loadConfigurations() {
      loadConfigs();
   }

   public NpcRegistry getRegistry() {
      return this.registry;
   }

   public SelectionManager getSelectionManager() {
      return this.selectionManager;
   }

   public MineskinService getMineskinService() {
      return this.mineskinService;
   }

   public Utils getUtils() {
      return this.utils;
   }

   public MojangApi getMojangApi() {
      return this.mojangApi;
   }

   public ConfigManager getConfigManager() {
      return this.configmanager;
   }

   public ActionsManager getActionsManager() {
      return this.actionsmanager;
   }

   public com.pumpkings.coconpc.action.CooldownManager getCooldownManager() {
      return this.cooldownManager;
   }

   public AnimationManager getAnimationManager() {
      return this.animationManager;
   }

   public com.pumpkings.coconpc.core.packet.CocoPacketEngine getPacketEngine() {
      return this.packetEngine;
   }
}
