package com.pumpkings.coconpc.listener;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;

public class NpcInteractListener implements Listener {
    private final CocoNPC plugin;

    public NpcInteractListener(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onScroll(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getSelectionManager().inModify(player)) return;

        event.setCancelled(true);

        int oldSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();
        if (oldSlot == newSlot) return;

        boolean scrolledRight = (newSlot == 0 && oldSlot == 8) || (newSlot > oldSlot && !(newSlot == 8 && oldSlot == 0));
        plugin.getSelectionManager().scroll(player, scrolledRight);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getSelectionManager().inModify(player)) {
            event.setCancelled(true);
            if (event.getAction().isRightClick()) {
                this.plugin.getSelectionManager().cycleMode(player);
            } else if (event.getAction().isLeftClick() && player.isSneaking()) {
                com.pumpkings.coconpc.core.npc.NpcEntity npc = this.plugin.getSelectionManager().getSelected(player);
                this.plugin.getSelectionManager().deselect(player);
                if (npc != null) plugin.getConfigManager().getRepository().saveParts(npc);
            }
        }
    }

    @EventHandler
    public void onSwapHand(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getSelectionManager().inModify(player)) {
            event.setCancelled(true);
            this.plugin.getSelectionManager().cycleMode(player);
        }
    }

    /**
     * Shift + left click on any entity leaves 3D editing mode.
     *
     * <p>NPC parts themselves are packet-only and never raise this event; clicks on
     * them arrive through {@code NpcPacketInteractListener}.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!player.isSneaking() || !this.plugin.getSelectionManager().inModify(player)) return;

        event.setCancelled(true);
        NpcEntity npc = this.plugin.getSelectionManager().getSelected(player);
        this.plugin.getSelectionManager().deselect(player);
        if (npc != null) plugin.getConfigManager().getRepository().saveParts(npc);
    }

    @EventHandler
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            Player player = event.getPlayer();
            if (player.isSneaking() && this.plugin.getSelectionManager().inModify(player)) {
                com.pumpkings.coconpc.core.npc.NpcEntity npc = this.plugin.getSelectionManager().getSelected(player);
                this.plugin.getSelectionManager().deselect(player);
                if (npc != null) plugin.getConfigManager().getRepository().saveParts(npc);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getSelectionManager().hasPendingSetKey(player)) {
            event.setCancelled(true);
            String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            this.plugin.getSelectionManager().consumeSetKey(player);
            if (!message.equalsIgnoreCase("cancel")) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getConfigManager().setMineskinApiKey(message);
                    plugin.getMineskinService().rebuildClient();
                    Message.API_KEY_SAVED.send(plugin, player);
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gold>Heads up: <yellow>the key you just typed was written to the server log. "
                            + "Clear it, or rotate the key at mineskin.org, if anyone else can read that log."));
                    plugin.getLogger().info("MineSkin API key updated by " + player.getName() + ".");
                });
            }
            return;
        }

        if (this.plugin.getSelectionManager().hasPendingHologramAdd(player)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            NpcEntity npc = this.plugin.getSelectionManager().consumeHologramAdd(player);
            if (npc != null && !message.equalsIgnoreCase("cancel")) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    java.util.List<String> lines = new java.util.ArrayList<>(plugin.getConfigManager().getRepository().getHologramLines(npc));
                    lines.add(message);
                    plugin.getConfigManager().getRepository().saveHologram(npc, lines);
                    npc.updateHologram(lines);
                    Message.HOLOGRAM_LINE_ADDED.send(plugin, player);
                    new com.pumpkings.coconpc.menu.HologramEditorMenu(plugin, npc).open(player);
                });
            } else if (npc != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    new com.pumpkings.coconpc.menu.HologramEditorMenu(plugin, npc).open(player);
                });
            }
            return;
        }

        if (this.plugin.getSelectionManager().hasPendingHologramBg(player)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            NpcEntity npc = this.plugin.getSelectionManager().consumeHologramBg(player);
            if (npc != null && !message.equalsIgnoreCase("cancel")) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    String billboard = plugin.getConfigManager().getRepository().getHologramBillboard(npc);
                    boolean shadow = plugin.getConfigManager().getRepository().getHologramShadowed(npc);
                    plugin.getConfigManager().getRepository().saveHologramStyle(npc, billboard, shadow, message);
                    npc.updateHologramStyle(billboard, shadow, message);
                    Message.HOLOGRAM_STYLE_UPDATED.send(plugin, player);
                    new com.pumpkings.coconpc.menu.HologramEditorMenu(plugin, npc).open(player);
                });
            } else if (npc != null) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    new com.pumpkings.coconpc.menu.HologramEditorMenu(plugin, npc).open(player);
                });
            }
            return;
        }

        if (this.plugin.getSelectionManager().hasPendingRotate(player)) {
            event.setCancelled(true);
            String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
            com.pumpkings.coconpc.menu.EditorTarget type = this.plugin.getSelectionManager().consumeManualRotate(player);
            NpcEntity npc = this.plugin.getSelectionManager().getSelected(player);

            if (npc == null) return;

            try {
                String[] parts = message.split(" ");
                float pitch = Float.parseFloat(parts[0]);
                float yaw = parts.length > 1 ? Float.parseFloat(parts[1]) : 0;
                float roll = parts.length > 2 ? Float.parseFloat(parts[2]) : 0;

                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    npc.setRotation(type, pitch, yaw, roll);
                    plugin.getConfigManager().getRepository().saveParts(npc);
                    Message.ROTATION_APPLIED.send(plugin, player);
                });
            } catch (Exception e) {
                Message.ROTATION_INVALID_FORMAT.send(plugin, player);
            }
        }
    }
}
