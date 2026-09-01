package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.pumpkings.coconpc.util.NpcInputPolicy;

public class SpawnCommand extends SubCommand {

    public SpawnCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            Message.SPAWN_USAGE.send(plugin, player);
            return;
        }
        String customId = args[1];
        if (!NpcInputPolicy.isValidNpcId(customId)) {
            Message.INVALID_NUMBER.send(plugin, player, "{value}", args[1]);
            return;
        }
        if (getNpcById(customId) != null) {
            Message.ID_ALREADY_IN_USE.send(plugin, player);
            return;
        }

        String skin = args[2];
        float size = 1.0f;
        if (args.length >= 4) {
            try {
                size = Float.parseFloat(args[3]);
            } catch (NumberFormatException ignored) {}
        }

        Location spawnLoc = playerFacingLocation(player);
        plugin.getRegistry().spawn(spawnLoc, player, skin, size, customId);
    }
}
