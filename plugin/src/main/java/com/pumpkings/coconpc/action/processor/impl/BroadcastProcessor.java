package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public BroadcastProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[broadcast]";
    }

    @Override
    public void execute(Player player, String data) {
        Bukkit.getOnlinePlayers().forEach(p ->
            p.sendMessage(plugin.getUtils().color(data))
        );
    }
}

