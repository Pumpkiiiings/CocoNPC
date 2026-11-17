package com.pumpkings.coconpc.menu;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.mob.CustomMob;
import com.pumpkings.coconpc.core.mob.MobDefinition;
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
import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game editor for a live {@link CustomMob}, opened with
 * {@code Shift + Right click} or {@code /coconpc mob edit <id|nearest>}.
 */
public final class MobEditorMenu implements InventoryHolder, Listener {

    private static final int SIZE = 36;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final CocoNPC plugin;
    private final CustomMob mob;
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    public MobEditorMenu(CocoNPC plugin, CustomMob mob) {
        this.plugin = plugin;
        this.mob = mob;

        String id = mob.getDefinition().getId();
        this.inventory = Bukkit.createInventory(this, SIZE,
                MM.deserialize("<dark_gray>Mob Editor <dark_gray>— <yellow>" + id));
        setItems();
    }

    public static boolean canEdit(Player player) {
        return player != null && (player.hasPermission("CocoNPC.mob.edit")
                || player.hasPermission("CocoNPC.admin"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
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
        fillRemainingSlots();

        MobDefinition definition = mob.getDefinition();
        String currentAnimation = mob.getCurrentAnimationName();

        setItem(4, Material.ARMOR_STAND, "<gold>" + definition.getId(), List.of(
                "<gray>Driver: <white>" + definition.getDriverType().name(),
                "<gray>Alive: <white>" + plugin.getMobRegistry()
                        .countMobs(definition.getId()),
                "<gray>Current animation: <white>"
                        + (currentAnimation != null ? currentAnimation : "idle")), null);

        List<String> animationLore = new ArrayList<>();
        animationLore.add("<gray>Animations in the .bbmodel: <yellow>"
                + mob.getCustomAnimationNames().size());
        animationLore.add("");
        animationLore.add("<yellow>Left click <gray>to open the selector.");
        setItem(11, Material.PAPER, "<gold>Animations", animationLore, event -> {
            Player player = (Player) event.getWhoClicked();
            later(player, () -> new MobAnimationMenu(plugin, mob, 0).open(player));
        });

        List<String> actionLore = new ArrayList<>();
        List<String> actions = definition.getActions();
        actionLore.add("<gray>Actions of this definition:");
        if (actions.isEmpty()) {
            actionLore.add("<dark_gray>• <gray>(none)");
        } else {
            int shown = Math.min(actions.size(), 8);
            for (int i = 0; i < shown; i++) {
                actionLore.add("<dark_gray>• <white>" + actions.get(i));
            }
            if (actions.size() > shown) {
                actionLore.add("<dark_gray>… and " + (actions.size() - shown) + " more");
            }
        }
        actionLore.add("");
        actionLore.add("<yellow>Left click <gray>to add one via chat.");
        actionLore.add("<yellow>Shift + right click <gray>to manage/remove.");
        setItem(13, Material.WRITABLE_BOOK, "<gold>Actions", actionLore, event -> {
            Player player = (Player) event.getWhoClicked();
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                later(player, () -> new MobActionListMenu(plugin, mob, 0).open(player));
                return;
            }
            if (event.getClick() == ClickType.LEFT) {
                player.closeInventory();
                plugin.getSelectionManager().beginMobAction(player, mob);
                Message.MOB_ACTION_PROMPT.send(plugin, player);
            }
        });

        setItem(15, Material.COMPARATOR, "<gold>Scale: <yellow>"
                + String.format(java.util.Locale.ROOT, "%.2f", definition.getScale()), List.of(
                "<gray>Left click: <green>+0.1",
                "<gray>Right click: <red>-0.1",
                "",
                "<dark_gray>Applies to every mob of this type."), event -> {
            if (!ensureValid((Player) event.getWhoClicked())) return;
            if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
                return;
            }

            double current = definition.getScale();
            double next = current + (event.getClick() == ClickType.LEFT ? 0.1 : -0.1);
            if (next < 0.1 || next > 10.0) {
                Message.MOB_SCALE_LIMIT.send(plugin, (Player) event.getWhoClicked());
                return;
            }

            Player player = (Player) event.getWhoClicked();
            if (plugin.getMobRegistry().saveScale(definition.getId(), next)) {
                Message.MOB_SCALE_UPDATED.send(plugin, player,
                        "{id}", definition.getId(),
                        "{scale}", String.format(java.util.Locale.ROOT, "%.2f",
                                mob.getDefinition().getScale()));
                later(player, () -> new MobEditorMenu(plugin, mob).open(player));
            } else {
                Message.MOB_SAVE_FAILED.send(plugin, player, "{id}", definition.getId());
            }
        });

        setItem(22, Material.ENDER_PEARL, "<gold>Teleport here", List.of(
                "<gray>Moves the driver, model and hitbox",
                "<gray>to your current location."), event -> {
            Player player = (Player) event.getWhoClicked();
            if (!ensureValid(player)) return;
            if (mob.teleportTo(player.getLocation())) {
                Message.MOB_TP_SUCCESS.send(plugin, player, "{id}", definition.getId());
            } else {
                Message.MOB_TP_FAILED.send(plugin, player, "{id}", definition.getId());
            }
        });

        setItem(31, Material.BARRIER, "<dark_gray>[<red>Delete mob<dark_gray>]", List.of(
                "<gray>Despawns the model, the hitbox",
                "<gray>and the driver of this instance."), event -> {
            Player player = (Player) event.getWhoClicked();
            if (!canEdit(player)) {
                Message.NO_PERMISSION.send(plugin, player);
                return;
            }
            plugin.getMobRegistry().removeMob(mob);
            Message.MOB_DELETED.send(plugin, player, "{id}", definition.getId());
            player.closeInventory();
        });

        setItem(35, Material.OAK_DOOR, "<red>Close", List.of(),
                event -> ((Player) event.getWhoClicked()).closeInventory());

        fillRemainingSlots();
    }

    private boolean ensureValid(Player player) {
        if (mob.isCleaned() || mob.getDriver() == null || !mob.getDriver().isValid()) {
            player.closeInventory();
            Message.MOB_NO_ACTIVE.send(plugin, player, "{id}", mob.getDefinition().getId());
            return false;
        }
        return true;
    }

    private void fillRemainingSlots() {
        ItemStack filler = buildItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
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

        Player player = (Player) event.getWhoClicked();
        if (!canEdit(player)) {
            Message.NO_PERMISSION.send(plugin, player);
            player.closeInventory();
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
