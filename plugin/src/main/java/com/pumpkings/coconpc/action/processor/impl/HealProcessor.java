package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class HealProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[heal]";
    }

    @Override
    public void execute(Player player, String data) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null ? 
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0;
        player.setHealth(maxHealth);
    }
}
