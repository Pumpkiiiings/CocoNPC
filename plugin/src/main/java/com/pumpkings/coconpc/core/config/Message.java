package com.pumpkings.coconpc.core.config;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public enum Message {
    PREFIX("<color:#3BFFC7>ᴄᴏᴄᴏɴᴘᴄ <color:#888888>| <reset>"),
    NO_PERMISSION("{prefix}<color:#FF3344>You don't have permission to do that."),
    ONLY_PLAYERS("<color:#FF3344>Only players can execute this command."),
    RELOAD_SUCCESS("{prefix}<color:#42F56C>Configuration reloaded <color:#888888>successfully."),
    SPAWN_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc create <id> <skin/name> [size]"),
    SPAWNING("{prefix}<color:#CCCCCC>Spawning NPC<color:#888888>..."),
    SPAWNED_SUCCESS("{prefix}<color:#42F56C>NPC spawned in <color:#FFE300>{time}s<color:#42F56C>."),
    SKIN_NOT_FOUND("{prefix}<color:#FF3344>That username has no skin associated with it."),
    SKIN_URL_INVALID("{prefix}<color:#FF3344>The provided URL is not a valid image or skin."),
    NPC_SELECTED("{prefix}<color:#CCCCCC>Selected NPC <color:#FFE300>{id}<color:#888888>."),
    NO_NPC_NEARBY("{prefix}<color:#FF3344>No NPC found nearby."),
    NPC_DESELECTED("{prefix}<color:#888888>NPC deselected."),
    NPC_DELETED("{prefix}<color:#FF3344>NPC deleted <color:#888888>successfully."),
    EDITOR_HINT("<color:#42F56C>ʀɪɢʜᴛ ᴄʟɪᴄᴋ <color:#CCCCCC>(or <color:#FFE300>F<color:#CCCCCC>) to switch mode <color:#888888>| <color:#FF3344>Shift + Click <color:#CCCCCC>to exit."),
    UPDATE_AVAILABLE("{prefix}<color:#42F56C>A new update is available<color:#888888>!"),
    UPDATE_URL("{prefix}<color:#33CCFF>https://www.spigotmc.org/resources/109428/"),
    HELP_HEADER("<color:#FFE300><st>                    </st> <color:#3BFFC7>ᴄᴏᴄᴏɴᴘᴄ <color:#888888>ʜᴇʟᴘ <color:#FFE300><st>                    </st>"),
    HELP_SPAWN("<color:#888888>» <color:#33CCFF>/coconpc create <id> <skin> [size] <color:#888888>— <color:#CCCCCC>Spawns an NPC"),
    HELP_RELOAD("<color:#888888>» <color:#33CCFF>/coconpc reload <color:#888888>— <color:#CCCCCC>Reloads the config"),
    HELP_LIST("<color:#888888>» <color:#33CCFF>/coconpc list <color:#888888>— <color:#CCCCCC>Lists all spawned NPCs"),
    HELP_TELEPORT("<color:#888888>» <color:#33CCFF>/coconpc tp <id> <color:#888888>— <color:#CCCCCC>Teleports you to an NPC"),
    HELP_TELEPORT_HERE("<color:#888888>» <color:#33CCFF>/coconpc tphere <id> <color:#888888>— <color:#CCCCCC>Teleports an NPC to you"),
    HELP_DELETE("<color:#888888>» <color:#33CCFF>/coconpc delete <id> <color:#888888>— <color:#CCCCCC>Deletes an NPC"),
    HELP_EDIT("<color:#888888>» <color:#33CCFF>/coconpc edit <id> <color:#888888>— <color:#CCCCCC>Opens the NPC editor"),
    HELP_ACTION("<color:#888888>» <color:#33CCFF>/coconpc action <id> <list/add/remove/clear> <color:#888888>— <color:#CCCCCC>Modifies NPC actions"),
    HELP_POSE("<color:#888888>» <color:#33CCFF>/coconpc pose <id> <pose> <color:#888888>— <color:#CCCCCC>Sets a preset pose"),
    NPC_NOT_FOUND("{prefix}<color:#FF3344>No NPC found with ID <color:#FFE300>{id}<color:#FF3344>."),
    INVALID_NUMBER("{prefix}<color:#FF3344>Invalid number: <color:#FFE300>{value}<color:#FF3344>."),
    TELEPORT_SUCCESS("{prefix}<color:#CCCCCC>Teleported to NPC <color:#FFE300>{id}<color:#888888>."),
    TELEPORT_HERE_SUCCESS("{prefix}<color:#CCCCCC>NPC <color:#FFE300>{id} <color:#CCCCCC>teleported to your location."),
    ACTION_ADDED("{prefix}<color:#42F56C>Action added to NPC <color:#FFE300>{id}<color:#42F56C>."),
    ACTION_CLEARED("{prefix}<color:#42F56C>All actions cleared from NPC <color:#FFE300>{id}<color:#42F56C>."),
    NPC_LIST_HEADER("<color:#FFE300><st>                    </st> <color:#3BFFC7>ꜱᴘᴀᴡɴᴇᴅ ɴᴘᴄꜱ <color:#FFE300><st>                    </st>"),
    NPC_LIST_ENTRY("<color:#888888>» <color:#FFE300>{id} <color:#888888>| <color:#CCCCCC>{x}, {y}, {z}"),
    NPC_LIST_EMPTY("{prefix}<color:#FF3344>There are no NPCs spawned."),
    ID_ALREADY_IN_USE("{prefix}<color:#FF3344>That ID is already in use."),
    LOADING_NPC_DATA("{prefix}<color:#CCCCCC>Loading NPC data<color:#888888>... <color:#CCCCCC>Please click again."),
    ROTATION_APPLIED("{prefix}<color:#42F56C>Rotation applied<color:#888888>!"),
    ROTATION_INVALID_FORMAT("{prefix}<color:#FF3344>Invalid format! Use numbers like: <color:#FFE300>45 0 -20"),
    ROTATION_PROMPT("{prefix}<color:#CCCCCC>Type the rotation in chat <color:#888888>(<color:#CCCCCC>pitch yaw roll<color:#888888>) — <color:#FFE300>e.g. 45 0 -20"),
    PART_VISIBLE("{prefix}<color:#42F56C>{part} <color:#CCCCCC>is now <color:#42F56C>visible<color:#888888>."),
    PART_INVISIBLE("{prefix}<color:#FF3344>{part} <color:#CCCCCC>is now <color:#FF3344>hidden<color:#888888>."),
    COOLDOWN_WAIT("{prefix}<color:#FF3344>Please wait <color:#FFE300>{time}s <color:#FF3344>before doing this again."),
    SKIN_PROGRESS("{prefix}<color:#CCCCCC>Progress <color:#888888>| {color}{progress}%"),
    ACTION_LIST_HEADER("<color:#FFE300><st>                    </st> <color:#3BFFC7>ᴀᴄᴛɪᴏɴꜱ <color:#888888>— NPC <color:#FFE300>#{id} <color:#FFE300><st>                    </st>"),
    ACTION_LIST_ENTRY("<color:#888888>» <color:#FFE300>#{index} <color:#888888>| <color:#CCCCCC>{action}"),
    ACTION_LIST_EMPTY("{prefix}<color:#FF3344>This NPC has no actions configured."),
    ACTION_REMOVED("{prefix}<color:#42F56C>Removed action <color:#FFE300>#{index} <color:#42F56C>from NPC <color:#FFE300>{id}<color:#888888>."),
    ACTION_INVALID_INDEX("{prefix}<color:#FF3344>No action found at index <color:#FFE300>{index}<color:#FF3344>."),
    ACTION_INVALID_TYPE("{prefix}<color:#FF3344>Invalid action type: <color:#FFE300>{type}<color:#FF3344>.\\n<color:#888888>Valid: <color:#CCCCCC>message, player, console, chat, broadcast, title, subtitle, give_exp, take_exp, connect, cooldown, require_permission, require_money, sound, teleport, actionbar, potion, heal, feed, give_item, take_item"),
    ACTION_HELP_HEADER("<color:#FFE300><st>                    </st> <color:#3BFFC7>ᴀᴄᴛɪᴏɴꜱ <color:#888888>ᴍᴀɴᴀɢᴇᴍᴇɴᴛ <color:#FFE300><st>                    </st>"),
    ACTION_HELP_LIST("<color:#888888>» <color:#33CCFF>/coconpc action <id> list <color:#888888>— <color:#CCCCCC>See all actions"),
    ACTION_HELP_ADD("<color:#888888>» <color:#33CCFF>/coconpc action <id> add <type> <text> <color:#888888>— <color:#CCCCCC>Add an action"),
    ACTION_HELP_REMOVE("<color:#888888>» <color:#33CCFF>/coconpc action <id> remove <index> <color:#888888>— <color:#CCCCCC>Remove action by index"),
    ACTION_HELP_CLEAR("<color:#888888>» <color:#33CCFF>/coconpc action <id> clear <color:#888888>— <color:#CCCCCC>Clear all actions"),
    POSE_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc pose <id> <stand/sit/lay/wave/salute/crossed_arms>"),
    POSE_APPLIED("{prefix}<color:#CCCCCC>Pose <color:#FFE300>{pose} <color:#CCCCCC>applied to NPC <color:#FFE300>#{id}<color:#888888>."),
    POSE_INVALID("{prefix}<color:#FF3344>Invalid pose <color:#FFE300>{pose}<color:#FF3344>. <color:#888888>Available: <color:#CCCCCC>stand, sit, lay, wave, salute, crossed_arms"),
    HOLOGRAM_PROMPT("{prefix}<color:#CCCCCC>Type the new hologram line in chat <color:#888888>(<color:#FF3344>cancel <color:#888888>to abort)<color:#888888>."),
    HOLOGRAM_BG_PROMPT("{prefix}<color:#CCCCCC>Type a background color in chat <color:#888888>— <color:#FFE300>#80000000<color:#888888>, <color:#FFE300>transparent <color:#888888>or <color:#FFE300>default<color:#888888>."),
    HOLOGRAM_LINE_ADDED("{prefix}<color:#42F56C>Hologram line added<color:#888888>!"),
    HOLOGRAM_LINE_REMOVED("{prefix}<color:#FF3344>Hologram line removed<color:#888888>."),
    HOLOGRAM_CLEARED("{prefix}<color:#FF3344>Hologram cleared<color:#888888>."),
    HOLOGRAM_STYLE_UPDATED("{prefix}<color:#42F56C>Hologram style updated<color:#888888>."),
    RESIZE_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc resize <id> <size>"),
    RESIZE_SUCCESS("{prefix}<color:#CCCCCC>NPC <color:#FFE300>#{id} <color:#CCCCCC>resized to <color:#FFE300>{size}<color:#888888>."),
    HELP_RESIZE("<color:#888888>» <color:#33CCFF>/coconpc resize <id> <size> <color:#888888>— <color:#CCCCCC>Resizes an NPC"),
    ITEM_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc item <id> <right|left> [clear]"),
    ITEM_EQUIPPED_SUCCESS("{prefix}<color:#42F56C>Equipped item to <color:#FFE300>{hand} hand <color:#42F56C>of NPC <color:#FFE300>#{id}<color:#888888>."),
    ITEM_CLEARED_SUCCESS("{prefix}<color:#CCCCCC>Cleared <color:#FFE300>{hand} hand <color:#CCCCCC>of NPC <color:#FFE300>#{id}<color:#888888>."),
    HELP_ITEM("<color:#888888>» <color:#33CCFF>/coconpc item <id> <right|left> [clear] <color:#888888>— <color:#CCCCCC>Equips or removes an item"),
    BEDROCK_UNSUPPORTED("{prefix}<color:#FF3344>NPCs are not supported on Bedrock."),
    SKIN_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc skin <id> <skin_name_or_url>"),
    SETKEY_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc setkey"),
    SETKEY_PROMPT("{prefix}<color:#CCCCCC>Type your MineSkin API key in chat <color:#888888>(<color:#FF3344>cancel <color:#888888>to abort)<color:#888888>."),
    CLONE_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc clone <source_id> <new_id>"),
    ANIMATE_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc animate <id> <animation_name>"),
    ANIMATION_NOT_FOUND("{prefix}<color:#FF3344>Animation <color:#FFE300>'{anim}' <color:#FF3344>not found."),
    ANIMATE_SUCCESS("{prefix}<color:#CCCCCC>Playing animation <color:#FFE300>'{anim}' <color:#CCCCCC>on NPC <color:#FFE300>{id}<color:#888888>."),
    ANIMATION_STOP_USAGE("{prefix}<color:#FF3344>Usage: <color:#FFE300>/coconpc animation_stop <id>"),
    ANIMATION_STOP_SUCCESS("{prefix}<color:#CCCCCC>Stopped animation on NPC <color:#FFE300>{id}<color:#888888>."),
    VIEW_DISTANCE_UPDATED("{prefix}<color:#42F56C>View distance updated to <color:#FFE300>{distance} blocks <color:#42F56C>and refreshed for all players."),
    API_KEY_SAVED("{prefix}<color:#42F56C>MineSkin API key saved. New skins can be generated now."),
    SKIN_PRELOAD_FINISHED("{prefix}<color:#42F56C>Finished preloading <color:#FFE300>{amount} <color:#42F56C>new skin(s)."),
    SKIN_CHANGING("{prefix}<color:#FFE300>Changing skin<color:#888888>..."),
    SKIN_GENERATION_FAILED("{prefix}<color:#FF3344>Skin generation failed: <color:#FFE300>{error}"),
    SKIN_APPLIED("{prefix}<color:#42F56C>Skin applied in <color:#FFE300>{time}s<color:#42F56C>."),
    CLONE_SUCCESS("{prefix}<color:#CCCCCC>NPC <color:#FFE300>#{source} <color:#CCCCCC>cloned as <color:#FFE300>#{new}<color:#888888>.");

    private final String defaultMessage;

    Message(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefault() {
        return defaultMessage;
    }

    public String get(CocoNPC plugin, String... replacements) {
        String message = plugin.getConfigManager().messages.getString(this.name().toLowerCase(), defaultMessage);
        if (this != PREFIX) {
            String prefix = plugin.getConfigManager().messages.getString(PREFIX.name().toLowerCase(), PREFIX.getDefault());
            message = message.replace("{prefix}", prefix);
        }
        if (replacements != null && replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    String target = replacements[i] != null ? replacements[i] : "null";
                    String repl = replacements[i + 1] != null ? replacements[i + 1] : "null";
                    message = message.replace(target, repl);
                }
            }
        }
        return message;
    }

    public void send(CocoNPC plugin, CommandSender sender, String... replacements) {
        plugin.getUtils().sendMessage(sender, get(plugin, replacements));
    }

    public void send(CocoNPC plugin, Player player, String... replacements) {
        plugin.getUtils().sendMessage(player, get(plugin, replacements));
    }
}
