package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class GiveExpProcessor implements ActionProcessor {

    @Override
    public String tag() {
        return "[give_exp]";
    }

    @Override
    public void execute(Player player, String data) {
        try {
            player.giveExpLevels(Integer.parseInt(data.trim()));
        } catch (NumberFormatException e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid exp value in [give_exp]: " + data);
        }
    }
}
