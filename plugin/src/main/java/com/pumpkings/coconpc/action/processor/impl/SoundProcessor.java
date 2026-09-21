package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[sound]";
    }

    @Override
    public void execute(Player player, String data) {
        try {
            String[] args = data.trim().split(" ");
            Sound sound = Sound.valueOf(args[0].toUpperCase());
            float volume = args.length > 1 ? Float.parseFloat(args[1]) : 1.0f;
            float pitch = args.length > 2 ? Float.parseFloat(args[2]) : 1.0f;
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid sound data in [sound]: " + data);
        }
    }
}
