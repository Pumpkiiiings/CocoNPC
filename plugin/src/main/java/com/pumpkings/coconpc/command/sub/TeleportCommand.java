package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class TeleportCommand extends SubCommand {

    public TeleportCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        String subcommand = args[0].toLowerCase();
        
        if (subcommand.equals("tphere")) {
            if (args.length < 2) {
                Message.HELP_TELEPORT_HERE.send(plugin, player);
                return;
            }
            withNpc(player, args[1], npc -> {
                npc.teleport(playerFacingLocation(player));
                plugin.getConfigManager().getRepository().saveLocationAndSkin(npc);
                Message.TELEPORT_HERE_SUCCESS.send(plugin, player, "{id}", npc.getId());
            });
        } else {
            if (args.length < 2) {
                Message.HELP_TELEPORT.send(plugin, player);
                return;
            }
            withNpc(player, args[1], npc -> {
                player.teleport(npc.getLocation());
                Message.TELEPORT_SUCCESS.send(plugin, player, "{id}", npc.getId());
            });
        }
    }
}
