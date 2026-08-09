package com.pumpkings.coconpc.core.packet.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.menu.NpcEditorMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class NpcPacketInteractListener extends PacketListenerAbstract {
    private final CocoNPC plugin;

    public NpcPacketInteractListener(CocoNPC plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            int clickedEntityId = wrapper.getEntityId();

            NpcEntity npc = plugin.getRegistry().getByVirtualId(clickedEntityId);
            if (npc == null) return;

            Player player = (Player) event.getPlayer();
            if (player == null || !player.isOnline()) return;

            WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                if (plugin.getSelectionManager().inModify(player)) {
                    if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT ||
                        action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
                        plugin.getSelectionManager().cycleMode(player);
                    } else if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK && player.isSneaking()) {
                        plugin.getSelectionManager().deselect(player);
                    }
                    return;
                }
                if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                    if (!player.isSneaking()) {
                        List<String> actions = plugin.getConfigManager().getActions(npc);
                        plugin.getActionsManager().runActions(player, actions, npc.getId());
                    }
                } else if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT ||
                           action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
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
}

