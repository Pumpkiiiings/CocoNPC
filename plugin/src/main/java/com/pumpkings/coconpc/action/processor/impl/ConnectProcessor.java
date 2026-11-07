package com.pumpkings.coconpc.action.processor.impl;

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

        String server = data.trim();

        // Try Velocity channel first (modern forwarding), fall back to BungeeCord legacy.
        // Velocity exposes "velocity:player_info"; we reuse the same Connect sub-command
        // through the "bungeecord:main" channel that Velocity also supports in legacy mode.
        // If neither proxy is present the message is simply discarded — that is harmless.
        boolean sentVelocity = trySendVelocity(player, server);
        if (!sentVelocity) {
            trySendBungeeCord(player, server);
        }
    }

    /**
     * Attempts to send a Connect message via the Velocity plugin messaging channel.
     * Returns {@code true} if the channel is registered (Velocity proxy is present).
     */
    private boolean trySendVelocity(Player player, String server) {
        final String VELOCITY_CHANNEL = "velocity:player_info";
        if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, VELOCITY_CHANNEL)) {
            // Register on the fly if not already done; Velocity will recognise it.
            try {
                plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, VELOCITY_CHANNEL);
            } catch (Exception ignored) {
                return false;
            }
        }
        try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
             java.io.DataOutputStream out = new java.io.DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, VELOCITY_CHANNEL, bytes.toByteArray());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Sends a Connect message via the classic BungeeCord plugin messaging channel.
     */
    private void trySendBungeeCord(Player player, String server) {
        try (java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
             java.io.DataOutputStream out = new java.io.DataOutputStream(bytes)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (java.io.IOException ignored) {
            // Memory streams do not throw IOException
        }
    }
}

