package com.pumpkings.coconpc.listener;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {
    private final CocoNPC plugin;

    public ConnectionListener(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSelectionManager() != null) {
            plugin.getSelectionManager().clear(player);
        }
        if (plugin.getActionsManager() != null) {
            plugin.getActionsManager().clear(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.hasNewUpdate && player.hasPermission("coconpc.admin")) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (!player.isOnline()) return;
                Message.UPDATE_AVAILABLE.send(plugin, player);
                Message.UPDATE_URL.send(plugin, player);
                player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0F, 1.0F);
            }, null, 60L);
        }
    }
}

