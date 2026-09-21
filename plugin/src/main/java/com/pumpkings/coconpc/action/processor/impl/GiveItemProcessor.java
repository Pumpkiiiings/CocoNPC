package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.action.processor.ActionProcessor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveItemProcessor implements ActionProcessor {
    @Override
    public String tag() {
        return "[give_item]";
    }

    @Override
    public void execute(Player player, String data) {
        try {
            String[] args = data.trim().split(" ");
            Material material = Material.matchMaterial(args[0].toUpperCase());
            if (material == null) return;
            int amount = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            player.getInventory().addItem(new ItemStack(material, amount));
        } catch (Exception e) {
            player.getServer().getLogger().warning("[CocoNPC] Invalid item data in [give_item]: " + data);
        }
    }
}
