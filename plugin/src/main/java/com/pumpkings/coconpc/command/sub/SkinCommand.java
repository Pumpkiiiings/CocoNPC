package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class SkinCommand extends SubCommand {

    public SkinCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            Message.SKIN_USAGE.send(plugin, player);
            return;
        }
        withNpc(player, args[1], npc -> plugin.getRegistry().changeSkin(npc, player, args[2]));
    }
}
