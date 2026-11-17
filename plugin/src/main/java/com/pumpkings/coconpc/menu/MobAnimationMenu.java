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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Selector with every animation of the mob's .bbmodel.
 *
 * <ul>
 *   <li>Left click: play in loop.</li>
 *   <li>Right click: play once.</li>
 *   <li>Shift + click: stop and return to idle/walk.</li>
 * </ul>
 */
public final class MobAnimationMenu implements InventoryHolder, Listener {

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
    private final List<String> animations;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    public MobAnimationMenu(CocoNPC plugin, CustomMob mob, int page) {
        this.plugin = plugin;
        this.mob = mob;
        this.animations = new ArrayList<>(mob.getCustomAnimationNames());
        int pages = pageCount();
        this.page = Math.max(0, Math.min(page, pages - 1));

        this.inventory = Bukkit.createInventory(this, SIZE,
                MM.deserialize("<dark_gray>Animations <dark_gray>— <yellow>"
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
        return Math.max(1, (animations.size() + PER_PAGE - 1) / PER_PAGE);
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
        String current = mob.getCurrentAnimationName();

        setItem(4, Material.BOOK, "<gold>Animations of <yellow>"
                + mob.getDefinition().getId(), List.of(
                "<gray>Current: <white>" + (current != null ? current : "idle"),
                "<gray>Page <yellow>" + (page + 1) + "<gray>/<yellow>" + pageCount(),
                "",
                "<yellow>Left click <gray>→ loop",
                "<yellow>Right click <gray>→ play once",
                "<yellow>Shift + click <gray>→ stop"), null);

        int start = page * PER_PAGE;
        for (int index = 0; index < PER_PAGE; index++) {
            int animationIndex = start + index;
            if (animationIndex >= animations.size()) break;

            String animation = animations.get(animationIndex);
            boolean active = animation.equalsIgnoreCase(current);
            ItemStack item = buildItem(active ? Material.LIME_DYE : Material.PAPER,
                    (active ? "<green>▶ " : "<yellow>") + animation, List.of(
                            "<gray>Left click: <green>loop",
                            "<gray>Right click: <yellow>play once",
                            "<gray>Shift + click: <red>stop",
                            "",
                            active ? "<green>Playing now" : "<dark_gray>Idle"));
            int slot = CONTENT_SLOTS[index];
            inventory.setItem(slot, item);
            handlers.put(slot, event -> handleAnimationClick(
                    (Player) event.getWhoClicked(), animation, event.getClick()));
        }

        if (animations.isEmpty()) {
            setItem(22, Material.STRUCTURE_VOID, "<red>No animations found", List.of(
                    "<gray>This .bbmodel has no animations."), null);
        }

        setItem(45, Material.ARROW, "<yellow>Previous page", List.of(),
                event -> {
                    Player player = (Player) event.getWhoClicked();
                    later(player, () -> new MobAnimationMenu(plugin, mob, page - 1)
                            .open(player));
                });
        setItem(53, Material.ARROW, "<yellow>Next page", List.of(),
                event -> {
                    Player player = (Player) event.getWhoClicked();
                    later(player, () -> new MobAnimationMenu(plugin, mob, page + 1)
                            .open(player));
                });
        setItem(48, Material.OAK_DOOR, "<red>Back", List.of(),
                event -> {
                    Player player = (Player) event.getWhoClicked();
                    later(player, () -> new MobEditorMenu(plugin, mob).open(player));
                });
        setItem(49, Material.BARRIER, "<red>Stop animation", List.of(
                "<gray>Returns the mob to <white>idle<gray>/<white>walk<gray>."), event -> {
            Player player = (Player) event.getWhoClicked();
            if (!mob.stopCustomAnimation()) {
                Message.MOB_STOPANIM_NONE.send(plugin, player,
                        "{id}", mob.getDefinition().getId());
            }
            later(player, () -> new MobAnimationMenu(plugin, mob, page).open(player));
        });

        ItemStack filler = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void handleAnimationClick(Player player, String animation, ClickType click) {
        if (click.isShiftClick()) {
            if (!mob.stopCustomAnimation()) {
                Message.MOB_STOPANIM_NONE.send(plugin, player,
                        "{id}", mob.getDefinition().getId());
            }
        } else if (click == ClickType.LEFT) {
            if (!mob.playCustomAnimation(animation, true)) {
                Message.MOB_ANIMATE_FAILED.send(plugin, player, "{animation}", animation);
            }
        } else if (click == ClickType.RIGHT) {
            if (!mob.playCustomAnimation(animation, false)) {
                Message.MOB_ANIMATE_FAILED.send(plugin, player, "{animation}", animation);
            }
        } else {
            return;
        }

        later(player, () -> new MobAnimationMenu(plugin, mob, page).open(player));
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
