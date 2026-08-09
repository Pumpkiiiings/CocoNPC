package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.entity.Player;

public class CloneCommand extends SubCommand {

    public CloneCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            Message.CLONE_USAGE.send(plugin, player);
            return;
        }
        String sourceId = args[1];
        String newId = args[2];
        if (!newId.matches("^[a-zA-Z0-9]+$")) {
            Message.INVALID_NUMBER.send(plugin, player, "{value}", args[2]);
            return;
        }
        NpcEntity source = getNpcById(sourceId);
        if (source == null) {
            Message.NPC_NOT_FOUND.send(plugin, player, "{id}", sourceId);
            return;
        }
        if (getNpcById(newId) != null) {
            Message.ID_ALREADY_IN_USE.send(plugin, player);
            return;
        }
        plugin.getRegistry().cloneNpc(source, playerFacingLocation(player), player, newId);
    }
}
