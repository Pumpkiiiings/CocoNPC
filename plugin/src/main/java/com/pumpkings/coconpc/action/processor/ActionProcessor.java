package com.pumpkings.coconpc.action.processor;

import org.bukkit.entity.Player;

public interface ActionProcessor {

    String tag();

    void execute(Player player, String data);
}

