package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.entity.Player;

public class AnimateCommand extends SubCommand {

    public AnimateCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        String subcommand = args[0].toLowerCase();
        
        if (subcommand.equals("animation_stop")) {
            if (args.length < 2) {
                Message.ANIMATION_STOP_USAGE.send(plugin, player);
                return;
            }
            withNpc(player, args[1], npc -> {
                npc.stopAnimation();
                Message.ANIMATION_STOP_SUCCESS.send(plugin, player, "{id}", npc.getId());
            });
        } else {
            if (args.length < 3) {
                Message.ANIMATE_USAGE.send(plugin, player);
                return;
            }
            withNpc(player, args[1], npc -> {
                String animName = args[2];
                com.pumpkings.coconpc.core.animation.Animation anim = plugin.getAnimationManager().getAnimation(animName);
                if (anim == null) {
                    Message.ANIMATION_NOT_FOUND.send(plugin, player, "{anim}", animName);
                    return;
                }
                npc.playAnimation(anim);
                Message.ANIMATE_SUCCESS.send(plugin, player, "{anim}", animName, "{id}", npc.getId());
            });
        }
    }
}
