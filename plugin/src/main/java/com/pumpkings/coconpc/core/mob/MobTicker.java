package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MobTicker {
    private static final boolean FOLIA = detectFolia();

    private final CocoNPC plugin;
    private final MobRegistry registry;
    private final Map<UUID, ScheduledTask> foliaTasks = new HashMap<>();

    private BukkitTask task;

    MobTicker(CocoNPC plugin, MobRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        if (FOLIA || task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, registry::tickAll, 1L, 1L);
    }

    public void attach(CustomMob mob) {
        if (!FOLIA || mob == null) return;

        LivingEntity driver = mob.getDriver();
        if (driver == null) return;

        ScheduledTask previous = foliaTasks.remove(mob.getInstanceId());
        if (previous != null) {
            previous.cancel();
        }

        ScheduledTask scheduled = driver.getScheduler().runAtFixedRate(
                plugin,
                ignored -> mob.tick(),
                mob::handleChunkUnload,
                1L,
                1L);
        if (scheduled != null) {
            foliaTasks.put(mob.getInstanceId(), scheduled);
        }
    }

    public void detach(CustomMob mob) {
        if (mob == null) return;
        ScheduledTask scheduled = foliaTasks.remove(mob.getInstanceId());
        if (scheduled != null) {
            scheduled.cancel();
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        for (ScheduledTask scheduled : new ArrayList<>(foliaTasks.values())) {
            scheduled.cancel();
        }
        foliaTasks.clear();
    }

    public boolean isFolia() {
        return FOLIA;
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
