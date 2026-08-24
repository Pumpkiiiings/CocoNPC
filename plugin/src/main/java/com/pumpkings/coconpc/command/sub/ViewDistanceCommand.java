package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class ViewDistanceCommand extends SubCommand {

    public ViewDistanceCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(mm("<yellow>Current NPC view distance: <gold>"
                    + plugin.getPacketEngine().getTracker().getViewDistance()
                    + " blocks<yellow>. Use <gold>/cnpc viewdistance <blocks><yellow> to change it."));
            return;
        }
        try {
            int dist = Integer.parseInt(args[1]);
            plugin.getPacketEngine().getTracker().setViewDistance(dist);
            Message.VIEW_DISTANCE_UPDATED.send(plugin, player, "{distance}", String.valueOf(dist));
        } catch (NumberFormatException e) {
            Message.INVALID_NUMBER.send(plugin, player, "{value}", args[1]);
        }
    }
}
