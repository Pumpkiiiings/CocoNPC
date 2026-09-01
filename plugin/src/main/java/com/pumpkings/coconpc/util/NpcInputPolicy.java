package com.pumpkings.coconpc.util;

import java.util.regex.Pattern;

public final class NpcInputPolicy {
    private static final Pattern NPC_ID = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    private static final Pattern PLAYER_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    private NpcInputPolicy() {
    }

    public static boolean isValidNpcId(String value) {
        return value != null && NPC_ID.matcher(value).matches();
    }

    public static boolean isValidPlayerName(String value) {
        return value != null && PLAYER_NAME.matcher(value).matches();
    }
}
