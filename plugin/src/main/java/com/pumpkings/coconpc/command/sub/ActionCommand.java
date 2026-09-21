package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ActionCommand extends SubCommand {

    public ActionCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            sendActionHelp(player);
            return;
        }

        withNpc(player, args[1], npc -> {
            String sub = args[2].toLowerCase();
            List<String> currentActions = new ArrayList<>(plugin.getConfigManager().getActions(npc));
            String id = npc.getId();

            switch (sub) {
                case "list" -> {
                    if (currentActions.isEmpty()) { Message.ACTION_LIST_EMPTY.send(plugin, player); return; }
                    Message.ACTION_LIST_HEADER.send(plugin, player, "{id}", id);
                    for (int i = 0; i < currentActions.size(); i++) {
                        Message.ACTION_LIST_ENTRY.send(plugin, player, "{index}", String.valueOf(i + 1), "{action}", currentActions.get(i));
                    }
                }
                case "clear" -> {
                    plugin.getConfigManager().saveActions(npc, new ArrayList<>());
                    Message.ACTION_CLEARED.send(plugin, player, "{id}", id);
                }
                case "remove", "del", "delete" -> {
                    if (args.length < 4) { Message.ACTION_HELP_REMOVE.send(plugin, player, "<id>", id); return; }
                    try {
                        int index = Integer.parseInt(args[3]);
                        if (index < 1 || index > currentActions.size()) {
                            Message.ACTION_INVALID_INDEX.send(plugin, player, "{index}", args[3]);
                        } else {
                            currentActions.remove(index - 1);
                            plugin.getConfigManager().saveActions(npc, currentActions);
                            Message.ACTION_REMOVED.send(plugin, player, "{index}", String.valueOf(index), "{id}", id);
                        }
                    } catch (NumberFormatException e) {
                        Message.INVALID_NUMBER.send(plugin, player, "{value}", args[3]);
                    }
                }
                case "add" -> {
                    if (args.length < 5) { Message.ACTION_HELP_ADD.send(plugin, player, "<id>", id); return; }
                    String type = args[3].toLowerCase();
                    if (type.startsWith("[") && type.endsWith("]")) type = type.substring(1, type.length() - 1);
                    if (type.equals("connect")) type = "server";
                    if (!com.pumpkings.coconpc.command.NpcCommand.ACTION_TYPES.contains(type)) {
                        Message.ACTION_INVALID_TYPE.send(plugin, player, "{type}", args[3]); return;
                    }
                    String fullAction = "[" + type + "] " + String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    currentActions.add(fullAction);
                    plugin.getConfigManager().saveActions(npc, currentActions);
                    Message.ACTION_ADDED.send(plugin, player, "{id}", id);
                }
                default -> sendActionHelp(player);
            }
        });
    }

    private void sendActionHelp(Player player) {
        Message.ACTION_HELP_HEADER.send(plugin, player);
        Message.ACTION_HELP_LIST.send(plugin, player);
        Message.ACTION_HELP_ADD.send(plugin, player);
        Message.ACTION_HELP_REMOVE.send(plugin, player);
        Message.ACTION_HELP_CLEAR.send(plugin, player);
    }
}
