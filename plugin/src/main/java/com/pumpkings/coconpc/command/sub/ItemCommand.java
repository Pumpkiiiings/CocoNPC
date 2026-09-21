package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import org.bukkit.entity.Player;
import java.util.Set;

public class ItemCommand extends SubCommand {

    public ItemCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            Message.ITEM_USAGE.send(plugin, player);
            return;
        }
        withNpc(player, args[1], npc -> {
            String hand = args[2].toLowerCase();
            boolean clear = args.length >= 4 && Set.of("clear", "remove", "quitar").contains(args[3].toLowerCase());

            boolean isRight = Set.of("right", "r", "derecho", "derecha").contains(hand);
            boolean isLeft  = Set.of("left", "l", "izquierdo", "izq", "izquierda").contains(hand);

            if (!isRight && !isLeft) {
                Message.ITEM_USAGE.send(plugin, player);
                return;
            }

            String side = isRight ? "right" : "left";
            if (clear) {
                if (isRight) npc.setRightHandItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
                else         npc.setLeftHandItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
                Message.ITEM_CLEARED_SUCCESS.send(plugin, player, "{hand}", side, "{id}", npc.getId());
            } else {
                org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand().clone();
                if (isRight) npc.setRightHandItem(held);
                else         npc.setLeftHandItem(held);
                Message.ITEM_EQUIPPED_SUCCESS.send(plugin, player, "{hand}", side, "{id}", npc.getId());
            }
            plugin.getConfigManager().saveHandItems(npc, npc.getRightHandItem(), npc.getLeftHandItem());
        });
    }
}
