package com.pumpkings.coconpc.menu;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.mob.CustomMob;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * List view used to remove individual actions of a custom mob, or clear them
 * all. Opened from the Actions button (Shift + right click) of
 * {@link MobEditorMenu}.
 */
public final class MobActionListMenu implements InventoryHolder, Listener {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final CocoNPC plugin;
    private final CustomMob mob;
    private final int page;
    private final List<String> actions;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    public MobActionListMenu(CocoNPC plugin, CustomMob mob, int page) {
        this.plugin = plugin;
        this.mob = mob;
        this.actions = new ArrayList<>(mob.getDefinition().getActions());
        int pages = pageCount();
        this.page = Math.max(0, Math.min(page, pages - 1));

        this.inventory = Bukkit.createInventory(this, SIZE,
                MM.deserialize("<dark_gray>Actions <dark_gray>— <yellow>"
                        + mob.getDefinition().getId()));
        setItems();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    private int pageCount() {
        return Math.max(1, (actions.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private ItemStack buildItem(Material mat, String name, List<String> loreStrings) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getUtils().itemComponent(name == null ? "" : name));
            if (loreStrings != null && !loreStrings.isEmpty()) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (String line : loreStrings) {
                    lore.add(plugin.getUtils().itemComponent(line));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setItem(int slot, Material mat, String name, List<String> lore,
                         Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, buildItem(mat, name, lore));
        if (handler != null) {
            handlers.put(slot, handler);
        }
    }

    private void setItems() {
        setItem(4, Material.WRITABLE_BOOK, "<gold>Actions of <yellow>"
                + mob.getDefinition().getId(), List.of(
                "<gray>Total: <yellow>" + actions.size(),
                "<gray>Page <yellow>" + (page + 1) + "<gray>/<yellow>" + pageCount(),
                "",
                "<yellow>Left click an action <gray>to delete it."), null);

        int start = page * PER_PAGE;
        for (int index = 0; index < PER_PAGE; index++) {
            int actionIndex = start + index;
            if (actionIndex >= actions.size()) break;

            String action = actions.get(actionIndex);
            int slot = CONTENT_SLOTS[index];
            inventory.setItem(slot, buildItem(Material.PAPER,
                    "<yellow>#" + (actionIndex + 1), List.of(
                            "<white>" + action,
                            "",
                            "<red>Left click to delete")));
            handlers.put(slot, event -> delete((Player) event.getWhoClicked(), actionIndex));
        }

        if (actions.isEmpty()) {
            setItem(22, Material.BARRIER, "<red>No actions configured", List.of(
                    "<gray>Use the Add button to create one."), null);
        }

        setItem(45, Material.ARROW, "<yellow>Previous page", List.of(),
                event -> {
                    Player player = (Player) event.getWhoClicked();
                    later(player, () -> new MobActionListMenu(plugin, mob, page - 1)
                            .open(player));
                });
        setItem(53, Material.ARROW, "<yellow>Next page", List.of(),
                event -> {
                    Player player = (Player) event.getWhoClicked();
                    later(player, () -> new MobActionListMenu(plugin, mob, page + 1)
                            .open(player));
                });
        setItem(48, Material.OAK_DOOR, "<red>Back", List.of(),
                event -> {
                    Player player = (Player) event.getWhoClicked();
                    later(player, () -> new MobEditorMenu(plugin, mob).open(player));
                });
        setItem(49, Material.EMERALD, "<green>Add action", List.of(
                "<gray>Closes the menu and asks for",
                "<gray>the new action in chat."), event -> {
            Player player = (Player) event.getWhoClicked();
            player.closeInventory();
            plugin.getSelectionManager().beginMobAction(player, mob);
            Message.MOB_ACTION_PROMPT.send(plugin, player);
        });
        setItem(50, Material.LAVA_BUCKET, "<red>Clear all", List.of(
                "<gray>Removes every action of this mob."), event -> {
            Player player = (Player) event.getWhoClicked();
            if (plugin.getMobRegistry().saveActions(mob.getDefinition().getId(),
                    List.of())) {
                Message.MOB_ACTION_CLEARED.send(plugin, player,
                        "{id}", mob.getDefinition().getId());
                later(player, () -> new MobActionListMenu(plugin, mob, 0).open(player));
            } else {
                Message.MOB_SAVE_FAILED.send(plugin, player,
                        "{id}", mob.getDefinition().getId());
            }
        });

        ItemStack filler = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void delete(Player player, int index) {
        List<String> current = new ArrayList<>(mob.getDefinition().getActions());
        if (index < 0 || index >= current.size()) {
            Message.MOB_ACTION_INVALID_INDEX.send(plugin, player,
                    "{index}", String.valueOf(index + 1));
            return;
        }

        current.remove(index);
        if (plugin.getMobRegistry().saveActions(mob.getDefinition().getId(), current)) {
            Message.MOB_ACTION_REMOVED.send(plugin, player,
                    "{index}", String.valueOf(index + 1),
                    "{id}", mob.getDefinition().getId());
            later(player, () -> new MobActionListMenu(plugin, mob, page).open(player));
        } else {
            Message.MOB_SAVE_FAILED.send(plugin, player,
                    "{id}", mob.getDefinition().getId());
        }
    }

    private void later(Player player, Runnable action) {
        try {
            player.getScheduler().run(plugin, task -> action.run(), null);
        } catch (RuntimeException exception) {
            action.run();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getHolder() != this) {
            return;
        }

        Consumer<InventoryClickEvent> handler = handlers.get(event.getSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) return;
        HandlerList.unregisterAll(this);
    }
}
