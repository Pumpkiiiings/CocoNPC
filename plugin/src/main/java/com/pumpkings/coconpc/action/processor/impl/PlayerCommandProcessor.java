package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.entity.Player;

public class PlayerCommandProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public PlayerCommandProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[player]";
    }

    @Override
    public void execute(Player player, String data) {
        player.performCommand(plugin.getUtils().expandForCommand(data, player));
    }
}
