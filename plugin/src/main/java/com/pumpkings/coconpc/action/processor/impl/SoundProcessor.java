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
        if (data == null || data.isBlank()) return;
        try {
            String[] args = data.trim().split("\\s+");
            String soundArg = args[0];
            float volume = args.length > 1 ? Float.parseFloat(args[1]) : 1.0f;
            float pitch = args.length > 2 ? Float.parseFloat(args[2]) : 1.0f;

            Sound soundEnum = null;
            try {
                soundEnum = Sound.valueOf(soundArg.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                try {
                    soundEnum = Sound.valueOf(soundArg.toUpperCase().replace(".", "_").replace("MINECRAFT:", ""));
                } catch (IllegalArgumentException ignored2) {}
            }

            if (soundEnum != null) {
                player.playSound(player.getLocation(), soundEnum, volume, pitch);
            } else {
                player.playSound(player.getLocation(), soundArg.toLowerCase(), volume, pitch);
            }
        } catch (Exception e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid sound data in [sound]: " + data + " (" + e.getMessage() + ")");
        }
    }
}
