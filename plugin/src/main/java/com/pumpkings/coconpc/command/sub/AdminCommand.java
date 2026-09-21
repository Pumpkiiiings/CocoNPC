package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;

public class AdminCommand extends SubCommand {

    public AdminCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("preload")) {
            plugin.getMineskinService().preloadSkins(player);
        } else if (subcommand.equals("reload")) {
            plugin.loadConfigurations();
            plugin.getMineskinService().rebuildClient();
            plugin.getUtils().resetFloodgateCache();
            Message.RELOAD_SUCCESS.send(plugin, player);
        }
    }
}
