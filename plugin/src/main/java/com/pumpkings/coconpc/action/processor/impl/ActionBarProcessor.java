package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class ActionBarProcessor implements ActionProcessor {
    
    private final CocoNPC plugin;

    public ActionBarProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[actionbar]";
    }

    @Override
    public void execute(Player player, String data) {
        String colored = plugin.getUtils().color(data);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(colored));
    }
}
