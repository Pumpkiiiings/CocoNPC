package com.pumpkings.coconpc.core.npc.part;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NpcPart {
    void destroy();

    void updateTransform(Location baseLocation);

    void spawnFor(Player player, Location baseLocation);

    void destroyFor(Player player);

    void updateMetadataFor(Player player);

    int getVirtualEntityId();
}

