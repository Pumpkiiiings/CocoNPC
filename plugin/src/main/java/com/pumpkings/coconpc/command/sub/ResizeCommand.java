package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class ResizeCommand extends SubCommand {

    public ResizeCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            Message.RESIZE_USAGE.send(plugin, player);
            return;
        }
        withNpc(player, args[1], npc -> {
            float newSize;
            try {
                newSize = Float.parseFloat(args[2]);
            } catch (NumberFormatException e) {
                Message.INVALID_NUMBER.send(plugin, player, "{value}", args[2]);
                return;
            }
            npc.resize(newSize);
            plugin.getConfigManager().saveSize(npc, newSize);
            Message.RESIZE_SUCCESS.send(plugin, player, "{id}", npc.getId(), "{size}", String.valueOf(newSize));
        });
    }
}
