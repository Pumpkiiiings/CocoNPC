package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import com.pumpkings.coconpc.core.animation.Animation;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import com.pumpkings.coconpc.core.npc.registry.NpcRegistry;
import org.bukkit.entity.Player;

/**
 * Action tag: [animate] &lt;npc_id&gt; &lt;animation_name&gt;
 *
 * <p>Plays a loaded animation on the given NPC when a player clicks it.
 * Use "stop" as the animation name to stop any currently playing animation.
 *
 * <p>Examples:
 * <pre>
 *   [animate] 1 walk
 *   [animate] 1 stop
 *   [animate] myNpc idle
 * </pre>
 */
public class AnimateProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public AnimateProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[animate]";
    }

    @Override
    public void execute(Player player, String data) {
        if (data == null || data.isBlank()) {
            plugin.getLogger().warning("[CocoNPC] [animate] requires arguments: <npc_id> <animation_name>");
            return;
        }

        String[] args = data.trim().split(" ", 2);
        if (args.length < 2) {
            plugin.getLogger().warning("[CocoNPC] [animate] requires two arguments: <npc_id> <animation_name>. Got: \"" + data + "\"");
            return;
        }

        String npcId   = args[0].trim();
        String animName = args[1].trim();

        // Find the NPC by its string ID
        NpcEntity target = findNpcById(npcId);
        if (target == null) {
            plugin.getLogger().warning("[CocoNPC] [animate] could not find NPC with id \"" + npcId + "\".");
            return;
        }

        if (animName.equalsIgnoreCase("stop")) {
            target.stopAnimation();
            return;
        }

        Animation anim = plugin.getAnimationManager().getAnimation(animName);
        if (anim == null) {
            plugin.getLogger().warning("[CocoNPC] [animate] animation \"" + animName + "\" is not loaded. "
                    + "Make sure the file is in plugins/CocoNPC/animations/ and the server was reloaded.");
            return;
        }

        target.playAnimation(anim);
    }

    private NpcEntity findNpcById(String id) {
        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            if (id.equals(npc.getId())) return npc;
        }
        return null;
    }
}
