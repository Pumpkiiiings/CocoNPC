package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import com.pumpkings.coconpc.util.AttributeCompat;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class HealProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[heal]";
    }

    @Override
    public void execute(Player player, String data) {
        double maxHealth = 20.0;
        Attribute attribute = AttributeCompat.maxHealth();
        AttributeInstance instance = attribute != null ? player.getAttribute(attribute) : null;
        if (instance != null) {
            maxHealth = instance.getValue();
        }
        player.setHealth(maxHealth);
    }
}
