package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ListCommand extends SubCommand {

    public ListCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (plugin.getRegistry().getNpcs().isEmpty()) {
            Message.NPC_LIST_EMPTY.send(plugin, player);
            return;
        }
        Message.NPC_LIST_HEADER.send(plugin, player);
        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            Location loc = npc.getLocation();
            Message.NPC_LIST_ENTRY.send(plugin, player,
                    "{id}", npc.getId(),
                    "{x}", String.format("%.1f", loc.getX()),
                    "{y}", String.format("%.1f", loc.getY()),
                    "{z}", String.format("%.1f", loc.getZ()));
        }
    }
}
