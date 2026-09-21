package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class MessageProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public MessageProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[message]";
    }

    @Override
    public void execute(Player player, String data) {
        player.sendMessage(plugin.getUtils().color(data, player));
    }
}
