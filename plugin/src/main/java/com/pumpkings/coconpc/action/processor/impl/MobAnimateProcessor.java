package com.pumpkings.coconpc.action.processor.impl;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.processor.ActionProcessor;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.mob.CustomMob;
import com.pumpkings.coconpc.core.mob.MobDefinition;
import com.pumpkings.coconpc.core.mob.MobRegistry;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Action tag: [mob_animate] &lt;target&gt; &lt;animation&gt; [loop]
 *
 * <p>Plays any animation of a custom .bbmodel mob. The target can be:
 * <ul>
 *   <li>{@code nearest} — the closest custom mob to the player</li>
 *   <li>{@code #<instance-uuid>} — a specific mob instance</li>
 *   <li>a definition id — every active mob spawned from that definition</li>
 * </ul>
 *
 * <p>Use {@code stop} as the animation name to return the mob to its normal
 * idle/walk flow. Examples:
 * <pre>
 *   [mob_animate] nearest greet
 *   [mob_animate] bibot celebrate loop
 *   [mob_animate] #3f2a... stop
 * </pre>
 */
public class MobAnimateProcessor implements ActionProcessor {
    private final CocoNPC plugin;

    public MobAnimateProcessor(CocoNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String tag() {
        return "[mob_animate]";
    }

    @Override
    public void execute(Player player, String data) {
        if (player == null) return;

        String input = data != null ? data.trim() : "";
        if (input.isEmpty()) {
            plugin.getLogger().warning("[CocoNPC] [mob_animate] needs a target and an animation.");
            return;
        }

        String[] parts = input.split("\\s+");
        if (parts.length < 2) {
            plugin.getLogger().warning(
                    "[CocoNPC] [mob_animate] needs a target and an animation: " + input);
            return;
        }

        String target = parts[0];
        String animation = parts[1];
        boolean loop = parts.length > 2 && isLoopToken(parts[2]);

        List<CustomMob> mobs = resolve(player, target);
        if (mobs.isEmpty()) {
            Message.MOB_NO_ACTIVE.send(plugin, player, "{id}", target);
            return;
        }

        if (animation.equalsIgnoreCase("stop")) {
            int stopped = 0;
            for (CustomMob mob : mobs) {
                if (mob.stopCustomAnimation()) {
                    stopped++;
                }
            }
            if (stopped == 0) {
                Message.MOB_STOPANIM_NONE.send(plugin, player, "{id}", target);
            }
            return;
        }

        int played = 0;
        String lastMissing = null;
        for (CustomMob mob : mobs) {
            if (!mob.hasCustomAnimation(animation)) {
                lastMissing = mob.getDefinition().getId();
                continue;
            }
            if (mob.playCustomAnimation(animation, loop)) {
                played++;
            }
        }

        if (played == 0) {
            if (lastMissing != null) {
                Message.MOB_ANIMATION_UNKNOWN.send(plugin, player,
                        "{id}", lastMissing,
                        "{animation}", animation);
            } else {
                Message.MOB_ANIMATE_FAILED.send(plugin, player,
                        "{animation}", animation);
            }
        }
    }

    private List<CustomMob> resolve(Player player, String target) {
        MobRegistry registry = plugin.getMobRegistry();
        if (registry == null || target == null || target.isBlank()) {
            return List.of();
        }

        if (target.equalsIgnoreCase("nearest")) {
            CustomMob nearest = registry.getNearest(player.getLocation());
            return nearest != null ? List.of(nearest) : List.of();
        }

        if (target.startsWith("#")) {
            UUID instanceId;
            try {
                instanceId = UUID.fromString(target.substring(1));
            } catch (IllegalArgumentException exception) {
                return List.of();
            }
            CustomMob mob = registry.getByInstance(instanceId);
            return mob != null ? List.of(mob) : List.of();
        }

        MobDefinition definition = registry.getDefinition(target);
        if (definition == null) {
            return List.of();
        }

        List<CustomMob> matches = new ArrayList<>();
        for (CustomMob mob : registry.getMobs()) {
            if (mob.getDefinition().getId().equals(definition.getId())) {
                matches.add(mob);
            }
        }
        return matches;
    }

    private static boolean isLoopToken(String token) {
        return token.equalsIgnoreCase("loop")
                || token.equalsIgnoreCase("true")
                || token.equalsIgnoreCase("yes")
                || token.equals("1");
    }
}
