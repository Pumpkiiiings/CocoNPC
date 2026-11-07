package com.pumpkings.coconpc.core.animation;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Drives NPC animations each server tick using the Paper global-region scheduler,
 * which is compatible with both standard Paper and Folia.
 *
 * <p>The task is started once in {@link CocoNPC#onEnable()} and cancelled on disable.
 */
public class AnimatorTask implements Runnable {
    private final CocoNPC plugin;
    /** Delta time per tick at 20 TPS (seconds). */
    private static final float DELTA_SECONDS = 0.05f;

    private io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask;

    public AnimatorTask(CocoNPC plugin) {
        this.plugin = plugin;
    }

    /**
     * Schedules this task to run every tick via the global-region scheduler.
     * Call once from {@code onEnable}.
     */
    public void start() {
        scheduledTask = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, t -> run(), 1L, 1L);
    }

    /**
     * Cancels the repeating task. Call from {@code onDisable}.
     */
    public void stop() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    @Override
    public void run() {
        if (plugin.getRegistry() == null) return;

        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            ActiveAnimation active = npc.getActiveAnimation();
            if (active == null) continue;

            active.tick(DELTA_SECONDS);
            if (active.isFinished()) {
                npc.stopAnimation();
                continue;
            }

            java.util.Map<String, Vector3f>    posOffsets = new java.util.HashMap<>();
            java.util.Map<String, Quaternionf> rotOffsets = new java.util.HashMap<>();

            for (String partName : active.getAnimation().getBones().keySet()) {
                posOffsets.put(partName, active.getBonePosition(partName));
                rotOffsets.put(partName, active.getBoneRotation(partName));
            }
            npc.applyAnimation(posOffsets, rotOffsets);
        }
    }
}

