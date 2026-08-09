package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.menu.NpcEditorMenu;
import org.bukkit.entity.Player;

public class EditCommand extends SubCommand {

    public EditCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            Message.HELP_EDIT.send(plugin, player);
            return;
        }
        withNpc(player, args[1], npc -> {
            plugin.getSelectionManager().select(player, npc);
            new NpcEditorMenu(plugin, npc).open(player);
        });
    }
}
