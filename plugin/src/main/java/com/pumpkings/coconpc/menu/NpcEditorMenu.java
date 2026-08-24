package com.pumpkings.coconpc.menu;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class NpcEditorMenu implements Listener {

    private final CocoNPC plugin;
    private final NpcEntity npc;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public NpcEditorMenu(CocoNPC plugin, NpcEntity npc) {
        this.plugin = plugin;
        this.npc = npc;
        String title = plugin.getConfigManager().npcEditorMenu.getString("title", "<yellow>Edit NPC");
        this.inventory = Bukkit.createInventory(null, 36, MM.deserialize(title));
        setItems();
    }

    public void open(Player player) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    private ItemStack buildItem(Material mat, String name, List<String> loreStrings) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getUtils().itemComponent(name == null ? "" : name));
            if (loreStrings != null && !loreStrings.isEmpty()) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (String s : loreStrings) lore.add(plugin.getUtils().itemComponent(s));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setItem(int slot, Material mat, String name, List<String> lore, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, buildItem(mat, name, lore));
        if (handler != null) handlers.put(slot, handler);
    }

    private void setItems() {
        loadBorderFillers();

        placeModifyItem("head", EditorTarget.HEAD, 4);
        placeModifyItem("right_arm", EditorTarget.RIGHT_ARM, 12);
        placeModifyItem("body", EditorTarget.BODY, 13);
        placeModifyItem("left_arm", EditorTarget.LEFT_ARM, 14);
        placeModifyItem("right_item", EditorTarget.RIGHT_ITEM, 20);
        placeModifyItem("right_leg", EditorTarget.RIGHT_LEG, 21);
        placeModifyItem("global", EditorTarget.GLOBAL, 22);
        placeModifyItem("left_leg", EditorTarget.LEFT_LEG, 23);
        placeModifyItem("left_item", EditorTarget.LEFT_ITEM, 24);
        placeModifyItem("hologram", EditorTarget.HOLOGRAM, 8);

        String delPath = "items.delete.";
        int delSlot = plugin.getConfigManager().npcEditorMenu.getInt(delPath + "slot", 31);
        Material delMat = Material.valueOf(plugin.getConfigManager().npcEditorMenu.getString(delPath + "material", "BARRIER"));
        String delName = plugin.getConfigManager().npcEditorMenu.getString(delPath + "name", "<dark_gray>[<red>Delete<dark_gray>]");
        List<String> delLore = plugin.getConfigManager().npcEditorMenu.getStringList(delPath + "lore");

        setItem(delSlot, delMat, delName, delLore, event -> {
            Player player = (Player) event.getWhoClicked();
            if (!player.hasPermission("CocoNPC.npc.delete")) {
                Message.NO_PERMISSION.send(plugin, player);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.closeInventory();
                return;
            }
            npc.delete();
            Message.NPC_DELETED.send(plugin, player);
            player.closeInventory();
        });

        fillRemainingSlots();
    }

    private void placeModifyItem(String configKey, EditorTarget type, int defaultSlot) {
        String path = "items." + configKey + ".";
        int slot = plugin.getConfigManager().npcEditorMenu.getInt(path + "slot", defaultSlot);

        String defaultMat = "STONE";
        String defaultName = "<yellow>" + configKey;
        if (configKey.equals("right_item")) { defaultMat = "DIAMOND_SWORD"; defaultName = "<yellow>Right Hand Item"; }
        else if (configKey.equals("left_item")) { defaultMat = "SHIELD"; defaultName = "<yellow>Left Hand Item"; }

        Material mat = Material.valueOf(plugin.getConfigManager().npcEditorMenu.getString(path + "material", defaultMat));
        String name = plugin.getConfigManager().npcEditorMenu.getString(path + "name", defaultName);

        List<String> loreStrings = new ArrayList<>();
        if (plugin.getConfigManager().npcEditorMenu.contains(path + "lore")) {
            loreStrings = plugin.getConfigManager().npcEditorMenu.getStringList(path + "lore");
        } else if (configKey.equals("right_item") || configKey.equals("left_item")) {
            loreStrings.add("<gray>Left-Click: <white>Select to Rotate/Move");
            loreStrings.add("<gray>Right-Click: <white>Equip held item");
            loreStrings.add("<gray>Shift-Right-Click: <white>Clear item");
        }

        List<String> finalLoreStrings = loreStrings;
        String finalName = name;
        setItem(slot, mat, finalName, finalLoreStrings, event -> {
            Player player = (Player) event.getWhoClicked();
            if ((type == EditorTarget.RIGHT_ITEM || type == EditorTarget.LEFT_ITEM) && event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                String handName = (type == EditorTarget.RIGHT_ITEM) ? "right" : "left";
                if (type == EditorTarget.RIGHT_ITEM) npc.setRightHandItem(player.getInventory().getItemInMainHand().clone());
                else npc.setLeftHandItem(player.getInventory().getItemInMainHand().clone());
                plugin.getConfigManager().saveHandItems(npc, npc.getRightHandItem(), npc.getLeftHandItem());
                Message.ITEM_EQUIPPED_SUCCESS.send(plugin, player, "{hand}", handName, "{id}", npc.getId());
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
                player.closeInventory();
                return;
            }
            if ((type == EditorTarget.RIGHT_ITEM || type == EditorTarget.LEFT_ITEM) && event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                String handName = (type == EditorTarget.RIGHT_ITEM) ? "right" : "left";
                if (type == EditorTarget.RIGHT_ITEM) npc.setRightHandItem(new ItemStack(Material.AIR));
                else npc.setLeftHandItem(new ItemStack(Material.AIR));
                plugin.getConfigManager().saveHandItems(npc, npc.getRightHandItem(), npc.getLeftHandItem());
                Message.ITEM_CLEARED_SUCCESS.send(plugin, player, "{hand}", handName, "{id}", npc.getId());
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                player.closeInventory();
                return;
            }
            if (type == EditorTarget.HOLOGRAM && event.getClick().isShiftClick()) {
                Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
                return;
            }
            if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT) {
                plugin.getSelectionManager().beginModify(player, type);
                player.closeInventory();
                Message.EDITOR_HINT.send(plugin, player);

            } else if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT) {
                plugin.getSelectionManager().beginManualRotate(player, type);
                player.closeInventory();
                Message.ROTATION_PROMPT.send(plugin, player);
            } else if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                plugin.getSelectionManager().toggleLimb(player, type);
                plugin.getConfigManager().getRepository().saveParts(npc);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                player.closeInventory();
            }
        });
    }

    private void loadBorderFillers() {
        org.bukkit.configuration.ConfigurationSection fillersSection = plugin.getConfigManager().npcEditorMenu.getConfigurationSection("fillers");
        if (fillersSection != null) {
            for (String key : fillersSection.getKeys(false)) {
                if (fillersSection.contains(key + ".slots")) {
                    Material mat = Material.valueOf(fillersSection.getString(key + ".material", "BLACK_STAINED_GLASS_PANE"));
                    String name = fillersSection.getString(key + ".name", "");
                    ItemStack item = buildItem(mat, name, null);
                    for (int slot : fillersSection.getIntegerList(key + ".slots")) {
                        inventory.setItem(slot, item);
                    }
                }
            }
        }
    }

    private void fillRemainingSlots() {
        org.bukkit.configuration.ConfigurationSection fillersSection = plugin.getConfigManager().npcEditorMenu.getConfigurationSection("fillers");
        Material mat = Material.BLACK_STAINED_GLASS_PANE;
        String name = "";
        if (fillersSection != null && fillersSection.contains("default")) {
            mat = Material.valueOf(fillersSection.getString("default.material", "BLACK_STAINED_GLASS_PANE"));
            name = fillersSection.getString("default.name", "");
        }
        ItemStack filler = buildItem(mat, name, null);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) return;
        Consumer<InventoryClickEvent> handler = handlers.get(event.getSlot());
        if (handler != null) handler.accept(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }
}
