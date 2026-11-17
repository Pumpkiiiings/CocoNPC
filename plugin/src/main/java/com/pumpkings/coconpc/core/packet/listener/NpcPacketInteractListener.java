package com.pumpkings.coconpc.core.packet.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.menu.NpcEditorMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NpcPacketInteractListener extends PacketListenerAbstract {
    private static final long INTERACT_DEBOUNCE_MS = 100L;

    private final CocoNPC plugin;
    private final Map<UUID, Long> lastInteractMs = new ConcurrentHashMap<>();

    public NpcPacketInteractListener(CocoNPC plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        int clickedEntityId = wrapper.getEntityId();

        NpcEntity npc = plugin.getRegistry().getByVirtualId(clickedEntityId);
        if (npc == null) return;

        Player player = (Player) event.getPlayer();
        if (player == null || !player.isOnline()) return;

        WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();
        if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT
                || action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            if (wrapper.getHand() == InteractionHand.OFF_HAND) return;
            long now = System.currentTimeMillis();
            Long previous = lastInteractMs.put(player.getUniqueId(), now);
            if (previous != null && (now - previous) < INTERACT_DEBOUNCE_MS) return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            if (plugin.getSelectionManager().inModify(player)) {
                if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK && player.isSneaking()) {
                    plugin.getSelectionManager().deselect(player);
                }
                return;
            }

            if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (!player.isSneaking()) {
                    List<String> actions = plugin.getConfigManager().getActions(npc);
                    plugin.getActionsManager().runActions(player, actions, npc.getId());
                }
            } else if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT
                    || action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
                if (!player.isSneaking()) {
                    List<String> actions = plugin.getConfigManager().getActions(npc);
                    plugin.getActionsManager().runActions(player, actions, npc.getId());
                } else if (!player.hasPermission("CocoNPC.npc.edit")) {
                    Message.NO_PERMISSION.send(plugin, player);
                } else {
                    plugin.getSelectionManager().select(player, npc);
                    NpcEditorMenu gui = new NpcEditorMenu(plugin, npc);
                    gui.open(player);
                }
            }
        });
    }
}