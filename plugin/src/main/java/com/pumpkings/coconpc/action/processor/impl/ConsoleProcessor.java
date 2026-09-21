package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public ConsoleProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[console]";
    }

    @Override
    public void execute(Player player, String data) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getUtils().expandForCommand(data, player));
    }
}
