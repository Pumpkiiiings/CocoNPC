package com.pumpkings.coconpc.action.processor.impl;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class ConnectProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public ConnectProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[server]";
    }

    @Override
    public void execute(Player player, String data) {
        if (data == null || data.isBlank()) return;
        
        try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
             java.io.DataOutputStream out = new java.io.DataOutputStream(bytes)) {
            
            out.writeUTF("Connect");
            out.writeUTF(data.trim());
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
            
        } catch (java.io.IOException ignored) {
            // Memory streams do not throw IOException
        }
    }
}

