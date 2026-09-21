package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class TakeExpProcessor implements ActionProcessor {

    @Override
    public String tag() {
        return "[take_exp]";
    }

    @Override
    public void execute(Player player, String data) {
        try {
            int levels = Integer.parseInt(data.trim());
            player.giveExpLevels(-levels);
        } catch (NumberFormatException e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid exp value in [take_exp]: " + data);
        }
    }
}
