package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class SetKeyCommand extends SubCommand {

    public SetKeyCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        plugin.getSelectionManager().setPendingSetKey(player);
        Message.SETKEY_PROMPT.send(plugin, player);
    }
}
