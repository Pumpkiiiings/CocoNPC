package com.pumpkings.coconpc.util;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.Bukkit;

public class ServerVersionInfo {
    private final CocoNPC plugin;
    private final String version;

    public static int DISPLAY_OFFSET = 0;

    public ServerVersionInfo(CocoNPC plugin) {
        this.plugin = plugin;
        this.version = Bukkit.getMinecraftVersion();

        int npcCount = plugin.getRegistry().getNpcs().size();
        boolean hasKey = plugin.getConfigManager().skins.getString("mineskin.api-key", "").length() > 0;
        String keyStatus = hasKey ? "<green>Yes" : "<red>No";

        String[] logo = {
                "<color:#FFD700>▄█████  ▄▄▄   ▄▄▄▄  ▄▄▄  ███  ██ █████▄ ▄█████ ",
                "<color:#FFD700>██     ██▀██ ██▀▀▀ ██▀██ ██ ▀▄██ ██▄▄█▀ ██     ",
                "<color:#FFD700>▀█████ ▀███▀ ▀████ ▀███▀ ██   ██ ██     ▀█████ ",
                "",
                "<color:#FFD700> 🌟 <white>Running on Minecraft " + this.version,
                "<color:#FFD700> 🌟 <white>NPCs Loaded: <green>" + npcCount,
                "<color:#FFD700> 🌟 <white>MineSkin API Key: " + keyStatus
        };

        for (String line : logo) {
            Bukkit.getConsoleSender()
                    .sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line));
        }
    }

    public String getVersion() {
        return this.version;
    }
}
