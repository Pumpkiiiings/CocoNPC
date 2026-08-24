package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class FeedProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[feed]";
    }

    @Override
    public void execute(Player player, String data) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }
}
