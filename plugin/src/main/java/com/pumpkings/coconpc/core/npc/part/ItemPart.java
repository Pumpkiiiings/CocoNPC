package com.pumpkings.coconpc.core.npc.part;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

public class ItemPart extends AbstractNpcPart {
    private ItemStack item;
    private final ItemDisplay.ItemDisplayTransform transform;

    public ItemPart(ItemStack defaultItem) {
        this(defaultItem, ItemDisplay.ItemDisplayTransform.HEAD);
    }

    public ItemPart(ItemStack defaultItem, ItemDisplay.ItemDisplayTransform transform) {
        this.item = defaultItem;
        this.transform = transform;
    }

    public void setItem(ItemStack item) {
        this.item = item;
        if (virtualDisplay != null) {
            virtualDisplay.setItem(item);
            virtualDisplay.sendMetadataUpdateToAll();
        }
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    protected void syncVirtualDisplay(Location baseLocation) {
        super.syncVirtualDisplay(baseLocation);
        virtualDisplay.setItem(item);
        virtualDisplay.setTransform(transform);
    }
}

