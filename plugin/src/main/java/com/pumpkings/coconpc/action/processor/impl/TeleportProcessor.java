package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[teleport]";
    }

    @Override
    public void execute(Player player, String data) {
        try {
            String[] parts = data.trim().split(",");
            if (parts.length < 4) return;
            World world = Bukkit.getWorld(parts[0].trim());
            if (world == null) return;
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : player.getLocation().getYaw();
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : player.getLocation().getPitch();
            player.teleportAsync(new Location(world, x, y, z, yaw, pitch));
        } catch (Exception e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid location data in [teleport]: " + data);
        }
    }
}
