package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class DeleteCommand extends SubCommand {

    public DeleteCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            Message.HELP_DELETE.send(plugin, player);
            return;
        }
        withNpc(player, args[1], npc -> {
            npc.delete();
            Message.NPC_DELETED.send(plugin, player);
        });
    }
}
