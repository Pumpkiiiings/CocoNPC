package com.pumpkings.coconpc.core.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.packet.listener.NpcPacketInteractListener;
import com.pumpkings.coconpc.core.packet.tracker.NpcViewerTracker;

public class CocoPacketEngine {
    private final CocoNPC plugin;
    private NpcViewerTracker tracker;
    private NpcPacketInteractListener interactListener;

    public CocoPacketEngine(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.tracker = new NpcViewerTracker(plugin);
        this.interactListener = new NpcPacketInteractListener(plugin);
        PacketEvents.getAPI().getEventManager().registerListener(interactListener);
    }

    public void shutdown() {
        if (interactListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(interactListener);
        }
        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            npc.destroyForAll();
        }
    }

    public NpcViewerTracker getTracker() {
        return tracker;
    }
}
