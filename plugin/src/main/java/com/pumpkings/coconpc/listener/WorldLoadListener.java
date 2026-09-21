package com.pumpkings.coconpc.listener;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class WorldLoadListener implements Listener {
    private final CocoNPC plugin;

    public WorldLoadListener(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLoadChunk(ChunkLoadEvent event) {
        this.plugin.getRegistry().loadChunk(event.getChunk());
    }
}
