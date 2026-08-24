package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.npc.NpcPose;
import org.bukkit.entity.Player;

public class PoseCommand extends SubCommand {

    public PoseCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            Message.POSE_USAGE.send(plugin, player);
            return;
        }
        withNpc(player, args[1], npc -> {
            NpcPose pose = NpcPose.fromName(args[2]);
            if (pose == null) {
                Message.POSE_INVALID.send(plugin, player, "{pose}", args[2]);
            } else {
                npc.applyPose(pose);
                plugin.getConfigManager().getRepository().saveParts(npc);
                Message.POSE_APPLIED.send(plugin, player, "{pose}", pose.getName(), "{id}", npc.getId());
            }
        });
    }
}
