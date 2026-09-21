package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;

public class PotionProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[potion]";
    }

    @Override
    public void execute(Player player, String data) {
        try {
            String[] args = data.trim().split(" ");
            PotionEffectType type = PotionEffectType.getByName(args[0].toUpperCase());
            if (type == null) return;
            int duration = args.length > 1 ? Integer.parseInt(args[1]) * 20 : 200;
            int amplifier = args.length > 2 ? Integer.parseInt(args[2]) : 0;
            player.addPotionEffect(new PotionEffect(type, duration, amplifier));
        } catch (Exception e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid potion data in [potion]: " + data);
        }
    }
}
