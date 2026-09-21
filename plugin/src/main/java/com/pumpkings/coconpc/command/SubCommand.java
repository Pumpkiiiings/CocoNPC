package com.pumpkings.coconpc.command;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.npc.NpcEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.function.Consumer;
import com.pumpkings.coconpc.core.config.Message;

public abstract class SubCommand {
    protected final CocoNPC plugin;

    public SubCommand(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public abstract void execute(Player player, String[] args);

    protected NpcEntity getNpcById(String id) {
        for (NpcEntity npc : plugin.getRegistry().getNpcs().values()) {
            if (id.equals(npc.getId())) return npc;
        }
        return null;
    }

    protected Location playerFacingLocation(Player player) {
        Location loc = player.getLocation();
        loc.setYaw(loc.getYaw() + 180);
        loc.setPitch(0);
        return loc;
    }



    protected void withNpc(Player player, String id, Consumer<NpcEntity> action) {
        NpcEntity npc = getNpcById(id);
        if (npc == null) {
            Message.NPC_NOT_FOUND.send(plugin, player, "{id}", id);
        } else {
            action.accept(npc);
        }
    }

    protected Component mm(String str) {
        return MiniMessage.miniMessage().deserialize(str);
    }
}
