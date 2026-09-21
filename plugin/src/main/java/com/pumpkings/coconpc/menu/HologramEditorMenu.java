package com.pumpkings.coconpc.menu;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HologramEditorMenu implements Listener {

    private final CocoNPC plugin;
    private final NpcEntity npc;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public HologramEditorMenu(CocoNPC plugin, NpcEntity npc) {
        this.plugin = plugin;
        this.npc = npc;
        int rows = plugin.getConfigManager().hologramEditorMenu.getInt("rows", 6);
        String title = plugin.getConfigManager().hologramEditorMenu.getString("title", "<dark_gray>Hologram Editor");
        this.inventory = Bukkit.createInventory(null, rows * 9, MM.deserialize(title));
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

        List<String> currentLines = plugin.getConfigManager().getRepository().getHologramLines(npc);
        int[] lineSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        String templateMatStr = plugin.getConfigManager().hologramEditorMenu.getString("items.line_template.material", "PAPER");
        Material templateMat = Material.valueOf(templateMatStr);
        String templateName = plugin.getConfigManager().hologramEditorMenu.getString("items.line_template.name", "<b><color:#33CCFF>Line #{index}</color></b>");
        List<String> templateLore = plugin.getConfigManager().hologramEditorMenu.getStringList("items.line_template.lore");

        for (int i = 0; i < currentLines.size() && i < lineSlots.length; i++) {
            final int index = i;
            String lineText = currentLines.get(i);
            String name = templateName.replace("{index}", String.valueOf(i + 1));
            List<String> lore = new ArrayList<>();
            for (String s : templateLore) lore.add(s.replace("{line}", lineText));

            setItem(lineSlots[i], templateMat, name, lore, event -> {
                Player player = (Player) event.getWhoClicked();
                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.LEFT) {
                    List<String> modLines = new ArrayList<>(plugin.getConfigManager().getRepository().getHologramLines(npc));
                    if (index < modLines.size()) {
                        modLines.remove(index);
                        plugin.getConfigManager().getRepository().saveHologram(npc, modLines);
                        npc.updateHologram(modLines);
                        Message.HOLOGRAM_LINE_REMOVED.send(plugin, player);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
                    }
                }
            });
        }

        placeActionButton("back", 45, event -> {
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> new NpcEditorMenu(plugin, npc).open(player));
        });

        placeActionButton("add_line", 47, event -> {
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
            plugin.getSelectionManager().beginHologramAdd(player, npc);
            Message.HOLOGRAM_PROMPT.send(plugin, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        });

        placeActionButton("remove_line", 48, event -> {
            Player player = (Player) event.getWhoClicked();
            List<String> modLines = new ArrayList<>(plugin.getConfigManager().getRepository().getHologramLines(npc));
            if (!modLines.isEmpty()) {
                modLines.remove(modLines.size() - 1);
                plugin.getConfigManager().getRepository().saveHologram(npc, modLines);
                npc.updateHologram(modLines);
                Message.HOLOGRAM_LINE_REMOVED.send(plugin, player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
            }
        });

        placeActionButton("clear_all", 49, event -> {
            Player player = (Player) event.getWhoClicked();
            plugin.getConfigManager().getRepository().saveHologram(npc, new ArrayList<>());
            npc.updateHologram(new ArrayList<>());
            Message.HOLOGRAM_CLEARED.send(plugin, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
        });

        placeActionButton("billboard", 50, event -> {
            Player player = (Player) event.getWhoClicked();
            String current = plugin.getConfigManager().getRepository().getHologramBillboard(npc).toUpperCase();
            String next = switch (current) {
                case "CENTER" -> "VERTICAL";
                case "VERTICAL" -> "HORIZONTAL";
                case "HORIZONTAL" -> "FIXED";
                default -> "CENTER";
            };
            boolean shadow = plugin.getConfigManager().getRepository().getHologramShadowed(npc);
            String bg = plugin.getConfigManager().getRepository().getHologramBackground(npc);
            plugin.getConfigManager().getRepository().saveHologramStyle(npc, next, shadow, bg);
            npc.updateHologramStyle(next, shadow, bg);
            Message.HOLOGRAM_STYLE_UPDATED.send(plugin, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
        });

        placeActionButton("shadow", 51, event -> {
            Player player = (Player) event.getWhoClicked();
            boolean current = plugin.getConfigManager().getRepository().getHologramShadowed(npc);
            String billboard = plugin.getConfigManager().getRepository().getHologramBillboard(npc);
            String bg = plugin.getConfigManager().getRepository().getHologramBackground(npc);
            plugin.getConfigManager().getRepository().saveHologramStyle(npc, billboard, !current, bg);
            npc.updateHologramStyle(billboard, !current, bg);
            Message.HOLOGRAM_STYLE_UPDATED.send(plugin, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
        });

        placeActionButton("background", 52, event -> {
            Player player = (Player) event.getWhoClicked();
            if (event.getClick().isShiftClick()) {
                player.closeInventory();
                plugin.getSelectionManager().beginHologramBg(player, npc);
                Message.HOLOGRAM_BG_PROMPT.send(plugin, player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            } else {
                String current = plugin.getConfigManager().getRepository().getHologramBackground(npc);
                String next = "transparent".equalsIgnoreCase(current) ? "default" : "transparent";
                String billboard = plugin.getConfigManager().getRepository().getHologramBillboard(npc);
                boolean shadow = plugin.getConfigManager().getRepository().getHologramShadowed(npc);
                plugin.getConfigManager().getRepository().saveHologramStyle(npc, billboard, shadow, next);
                npc.updateHologramStyle(billboard, shadow, next);
                Message.HOLOGRAM_STYLE_UPDATED.send(plugin, player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                Bukkit.getScheduler().runTask(plugin, () -> new HologramEditorMenu(plugin, npc).open(player));
            }
        });

        fillRemainingSlots();
    }

    private void placeActionButton(String key, int defaultSlot, Consumer<InventoryClickEvent> action) {
        String path = "items." + key + ".";
        int slot = plugin.getConfigManager().hologramEditorMenu.getInt(path + "slot", defaultSlot);
        String matStr = plugin.getConfigManager().hologramEditorMenu.getString(path + "material", "STONE");
        Material mat = Material.valueOf(matStr);
        String name = plugin.getConfigManager().hologramEditorMenu.getString(path + "name", "<yellow>" + key);

        String billboard = plugin.getConfigManager().getRepository().getHologramBillboard(npc);
        String shadow = plugin.getConfigManager().getRepository().getHologramShadowed(npc) ? "Enabled" : "Disabled";
        String background = plugin.getConfigManager().getRepository().getHologramBackground(npc);

        name = replacePlaceholders(name, billboard, shadow, background);
        List<String> lore = new ArrayList<>();
        for (String s : plugin.getConfigManager().hologramEditorMenu.getStringList(path + "lore")) {
            lore.add(replacePlaceholders(s, billboard, shadow, background));
        }

        setItem(slot, mat, name, lore, action);
    }

    private String replacePlaceholders(String text, String billboard, String shadow, String background) {
        if (text == null) return "";
        return text.replace("%billboard%", billboard)
                .replace("%shadow%", shadow)
                .replace("%background%", background);
    }

    private void loadBorderFillers() {
        ConfigurationSection fillersSection = plugin.getConfigManager().hologramEditorMenu.getConfigurationSection("fillers");
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
        ConfigurationSection fillersSection = plugin.getConfigManager().hologramEditorMenu.getConfigurationSection("fillers");
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
