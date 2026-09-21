package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class ChatProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public ChatProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[chat]";
    }

    @Override
    public void execute(Player player, String data) {
        String processed = plugin.getUtils().color(data, player);
        player.chat(processed.replace('\u00A7', '&'));
    }
}


