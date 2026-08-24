package com.pumpkings.coconpc.core.config;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public enum Message {
    PREFIX("<dark_gray>[<yellow>CocoNPC<dark_gray>]<light_purple>! "),
    NO_PERMISSION("{prefix}<red>You do not have permission."),
    ONLY_PLAYERS("<red>Only players can use this command."),
    RELOAD_SUCCESS("{prefix}<green>Plugin reloaded successfully."),
    SPAWN_USAGE("{prefix}<red>Usage: /coconpc create <id> <skin/name> [size]"),
    SPAWNING("{prefix}<gray>Spawning NPC..."),
    SPAWNED_SUCCESS("{prefix}<gray>Spawned in <yellow>{time} <gray>seconds."),
    SKIN_NOT_FOUND("{prefix}<red>The name entered does not belong to an account with a skin."),
    SKIN_URL_INVALID("{prefix}<red>The indicated URL is not a correct image/skin."),
    NPC_SELECTED("{prefix}<green>You have selected NPC <yellow>{id}<green>."),
    NO_NPC_NEARBY("{prefix}<red>No nearby NPC found."),
    NPC_DESELECTED("<red>You have deselected the NPC."),
    NPC_DELETED("<red>Deleted NPC."),
    EDITOR_HINT("<green>Left Click to Rotate limb. Right Click to bend Joint. Scroll to move position. <red>Shift + Left Click to exit."),
    UPDATE_AVAILABLE("{prefix}<green>There is a new update available!"),
    UPDATE_URL("{prefix}<green>https://www.spigotmc.org/resources/109428/"),
    HELP_HEADER("<yellow>--- CocoNPC Help ---"),
    HELP_SPAWN("<green>/coconpc create <id> <skin> [size] <gray>- Spawns an NPC"),
    HELP_RELOAD("<green>/coconpc reload <gray>- Reloads the config"),
    HELP_LIST("<green>/coconpc list <gray>- Lists all spawned NPCs"),
    HELP_TELEPORT("<green>/coconpc tp <id> <gray>- Teleports you to an NPC"),
    HELP_TELEPORT_HERE("<green>/coconpc tphere <id> <gray>- Teleports an NPC to you"),
    HELP_DELETE("<green>/coconpc delete <id> <gray>- Deletes an NPC"),
    HELP_EDIT("<green>/coconpc edit <id> <gray>- Opens the NPC editor"),
    HELP_ACTION("<green>/coconpc action <id> <list/add/remove/clear> <gray>- Modifies NPC actions"),
    HELP_POSE("<green>/coconpc pose <id> <pose_name> <gray>- Sets a preset pose (stand, sit, lay, etc.)"),
    NPC_NOT_FOUND("{prefix}<red>No NPC found with ID <yellow>{id}<red>."),
    INVALID_NUMBER("{prefix}<red>Invalid number provided: <yellow>{value}"),
    TELEPORT_SUCCESS("{prefix}<green>Teleported to NPC <yellow>{id}<green>."),
    TELEPORT_HERE_SUCCESS("{prefix}<green>Teleported NPC <yellow>{id} <green>to your location."),
    ACTION_ADDED("{prefix}<green>Added action to NPC <yellow>{id}<green>."),
    ACTION_CLEARED("{prefix}<green>Cleared all actions from NPC <yellow>{id}<green>."),
    NPC_LIST_HEADER("{prefix}<yellow>--- Spawned NPCs ---"),
    NPC_LIST_ENTRY("<green>ID: <yellow>{id} <gray>- Location: <white>{x}, {y}, {z}"),
    NPC_LIST_EMPTY("{prefix}<red>There are no NPCs spawned."),
    ID_ALREADY_IN_USE("{prefix}<red>That ID is already in use!"),
    LOADING_NPC_DATA("{prefix}<gray>Loading NPC data, please click again."),
    ROTATION_APPLIED("{prefix}<green>Rotation applied!"),
    ROTATION_INVALID_FORMAT("{prefix}<red>Invalid format! Please use numbers like: <yellow>45 0 -20"),
    ROTATION_PROMPT("{prefix}<gray>Type the rotation in chat (format: <pitch> <yaw> <roll>). Example: <yellow>45 0 -20"),
    PART_VISIBLE("{prefix}<green>{part} is now visible."),
    PART_INVISIBLE("{prefix}<red>{part} is now invisible."),
    COOLDOWN_WAIT("{prefix}<red>You must wait {time}s to do this again."),
    SKIN_PROGRESS("{prefix}<gray>Progress {color}{progress}%"),

    ACTION_LIST_HEADER("{prefix}<yellow>--- Actions for NPC #{id} ---"),
    ACTION_LIST_ENTRY("<gold>#{index}: <white>{action}"),
    ACTION_LIST_EMPTY("{prefix}<red>This NPC has no actions configured."),
    ACTION_REMOVED("{prefix}<green>Removed action <yellow>#{index} <green>from NPC <yellow>{id}<green>."),
    ACTION_INVALID_INDEX("{prefix}<red>No action found at index <yellow>{index}<red>."),
    ACTION_INVALID_TYPE("{prefix}<red>Invalid action type: <yellow>{type}<red>. Valid types: message, player, console, chat, broadcast, title, subtitle, give_exp, take_exp, connect, cooldown, require_permission, require_money, sound, teleport, actionbar, potion, heal, feed, give_item, take_item"),
    ACTION_HELP_HEADER("<yellow>--- CocoNPC Actions Management ---"),
    ACTION_HELP_LIST("<green>/coconpc action <id> list <gray>- See all actions of an NPC"),
    ACTION_HELP_ADD("<green>/coconpc action <id> add <type> <text> <gray>- Add an action"),
    ACTION_HELP_REMOVE("<green>/coconpc action <id> remove <index> <gray>- Remove action by number"),
    ACTION_HELP_CLEAR("<green>/coconpc action <id> clear <gray>- Remove all actions"),
    POSE_USAGE("{prefix}<red>Usage: /coconpc pose <id> <stand/sit/lay/wave/salute/crossed_arms>"),
    POSE_APPLIED("{prefix}<green>Applied pose <yellow>{pose} <green>to NPC <yellow>#{id}<green>."),
    POSE_INVALID("{prefix}<red>Invalid pose <yellow>{pose}<red>! Available poses: <yellow>stand, sit, lay, wave, salute, crossed_arms"),
    HOLOGRAM_PROMPT("{prefix}<gray>Type the new hologram line in chat (or type <red>cancel<gray> to abort):"),
    HOLOGRAM_BG_PROMPT("{prefix}<gray>Type the background color in chat (e.g. <yellow>#80000000<gray>, <yellow>transparent<gray> or <yellow>default<gray>):"),
    HOLOGRAM_LINE_ADDED("{prefix}<green>Hologram line added!"),
    HOLOGRAM_LINE_REMOVED("{prefix}<green>Hologram line removed!"),
    HOLOGRAM_CLEARED("{prefix}<green>Hologram cleared!"),
    HOLOGRAM_STYLE_UPDATED("{prefix}<green>Hologram style updated!"),
    RESIZE_USAGE("{prefix}<red>Usage: /coconpc resize <id> <size>"),
    RESIZE_SUCCESS("{prefix}<green>NPC <yellow>#{id} <green>resized to <yellow>{size}<green>."),
    HELP_RESIZE("<green>/coconpc resize <id> <size> <gray>- Resizes an NPC"),
    ITEM_USAGE("{prefix}<red>Usage: /coconpc item <id> <right|left> [clear]"),
    ITEM_EQUIPPED_SUCCESS("{prefix}<green>Equipped item to <yellow>{hand} hand <green>of NPC <yellow>#{id}<green>."),
    ITEM_CLEARED_SUCCESS("{prefix}<green>Cleared <yellow>{hand} hand <green>of NPC <yellow>#{id}<green>."),
    HELP_ITEM("<green>/coconpc item <id> <right|left> [clear] <gray>- Equips or removes item from NPC hand"),
    BEDROCK_UNSUPPORTED("{prefix}<red>NPCs are not supported on Bedrock — you cannot see or use them."),
    SKIN_USAGE("{prefix}<red>Usage: <yellow>/coconpc skin <id> <skin_name_or_url>"),
    SETKEY_USAGE("{prefix}<red>Usage: <yellow>/coconpc setkey"),
    SETKEY_PROMPT("{prefix}<gray>Type your MineSkin API key in chat (or type <red>cancel<gray> to abort):"),
    CLONE_USAGE("{prefix}<red>Usage: <yellow>/coconpc clone <source_id> <new_id>"),
    ANIMATE_USAGE("{prefix}<red>Usage: /coconpc animate <id> <animation_name>"),
    ANIMATION_NOT_FOUND("{prefix}<red>Animation '{anim}' not found!"),
    ANIMATE_SUCCESS("{prefix}<green>Playing animation '{anim}' on NPC {id}"),
    ANIMATION_STOP_USAGE("{prefix}<red>Usage: /coconpc animation_stop <id>"),
    ANIMATION_STOP_SUCCESS("{prefix}<green>Stopped animation on NPC {id}"),
    VIEW_DISTANCE_UPDATED("{prefix}<green>NPC view distance updated to <yellow>{distance} blocks<green> and refreshed for all players!"),
    API_KEY_SAVED("{prefix}<green>MineSkin API key saved and applied. New skins can be generated now."),
    SKIN_PRELOAD_FINISHED("{prefix}<green>Finished preloading {amount} new skin(s)!"),
    SKIN_CHANGING("{prefix}<yellow>Changing skin, please wait..."),
    SKIN_GENERATION_FAILED("{prefix}<red>Skin generation failed: <yellow>{error}"),
    SKIN_APPLIED("{prefix}<green>Skin applied successfully in <yellow>{time}s<green>!"),
    CLONE_SUCCESS("{prefix}<green>NPC <yellow>#{source} <green>cloned as <yellow>#{new} <green>successfully!");

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


