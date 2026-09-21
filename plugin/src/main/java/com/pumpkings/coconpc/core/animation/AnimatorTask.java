package com.pumpkings.coconpc.core.animation;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.npc.part.ItemPart;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AnimatorTask extends BukkitRunnable {
    private final CocoNPC plugin;

    public AnimatorTask(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getRegistry() == null) return;
        float deltaSeconds = 0.05f;

        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            ActiveAnimation active = npc.getActiveAnimation();
            if (active == null) continue;

            active.tick(deltaSeconds);
            if (active.isFinished()) {
                npc.stopAnimation();
                continue;
            }
            java.util.Map<String, Vector3f> posOffsets = new java.util.HashMap<>();
            java.util.Map<String, Quaternionf> rotOffsets = new java.util.HashMap<>();

            for (String partName : active.getAnimation().getBones().keySet()) {
                posOffsets.put(partName, active.getBonePosition(partName));
                rotOffsets.put(partName, active.getBoneRotation(partName));
            }
            npc.applyAnimation(posOffsets, rotOffsets);
        }
    }
}

