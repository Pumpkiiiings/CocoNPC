package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.menu.MobEditorMenu;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobListener implements Listener {
    private static final long INTERACT_DEBOUNCE_MILLIS = 100L;

    private final CocoNPC plugin;
    private final MobRegistry registry;
    private final Map<UUID, Long> interactDebounce = new ConcurrentHashMap<>();

    public MobListener(CocoNPC plugin, MobRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onDamage(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        CustomMob mob = registry.getByDriver(damager.getUniqueId());
        if (mob != null) {
            mob.triggerAttack();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onAggro(event.getTarget());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        handleInteraction(event.getPlayer(), event.getRightClicked(), event.getHand());
    }

    /**
     * Paper fires {@link PlayerInteractAtEntityEvent} for precise hits; both
     * events can arrive for the same click, so the debounce keeps the actions
     * and the editor from running twice.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handleInteraction(event.getPlayer(), event.getRightClicked(), event.getHand());
    }

    private void handleInteraction(Player player, Entity clicked, EquipmentSlot hand) {
        if (player == null || clicked == null) return;
        if (hand != EquipmentSlot.HAND) return;
        if (!(clicked instanceof Interaction)) return;

        CustomMob mob = registry.getByInteraction(clicked);
        if (mob == null || mob.isCleaned()) return;
        if (isDebounced(player)) return;

        if (player.isSneaking()) {
            if (MobEditorMenu.canEdit(player)) {
                new MobEditorMenu(plugin, mob).open(player);
            } else {
                Message.NO_PERMISSION.send(plugin, player);
            }
            return;
        }

        mob.onInteraction(player, hand);
    }

    private boolean isDebounced(Player player) {
        long now = System.currentTimeMillis();
        Long previous = interactDebounce.put(player.getUniqueId(), now);
        return previous != null && now - previous < INTERACT_DEBOUNCE_MILLIS;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        interactDebounce.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onDriverRemoved(event.getCause());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        registry.handleEntitiesLoad(event.getChunk(), event.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        registry.handleChunkUnload(event.getChunk());
    }
}
