package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.action.ActionsManager;

import java.util.Locale;

/**
 * Helpers shared by the mob editor GUI and the {@code /coconpc mob action}
 * command to validate and normalize NPC-style action strings.
 */
public final class MobActionUtil {

    private MobActionUtil() {
    }

    /**
     * Accepts both a full action ({@code [message] Hello}) and free text, which
     * is treated as a message action.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        if (trimmed.charAt(0) == '[' && trimmed.indexOf(']') > 1) {
            return trimmed;
        }
        return "[message] " + trimmed;
    }

    /** Returns the lowercase tag of an action, for example {@code [message]}. */
    public static String tagOf(String action) {
        if (action == null) return "";
        String trimmed = action.trim();
        if (!trimmed.startsWith("[")) return "";
        int end = trimmed.indexOf(']');
        if (end <= 1) return "";
        return trimmed.substring(0, end + 1).toLowerCase(Locale.ROOT);
    }

    /** Returns the tag without brackets, for example {@code message}. */
    public static String typeOf(String action) {
        String tag = tagOf(action);
        return tag.isEmpty() ? "" : tag.substring(1, tag.length() - 1);
    }

    /** Returns whether the action uses a tag known to the action system. */
    public static boolean isValid(CocoNPC plugin, String action) {
        String tag = tagOf(action);
        if (tag.isEmpty()) return false;

        if (ActionsManager.getConditionTags().contains(tag)) {
            return true;
        }
        return plugin.getActionsManager() != null
                && plugin.getActionsManager().getRegisteredTags().contains(tag);
    }
}
