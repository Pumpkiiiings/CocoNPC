package com.pumpkings.coconpc.core.npc;

import java.util.List;

public final class BodyParts {

    public static final List<String> SKIN_PARTS = List.of(
            "head", "torsoUpper", "torsoLower",
            "rightArmUpper", "rightArmLower", "leftArmUpper", "leftArmLower",
            "rightLegUpper", "rightLegLower", "leftLegUpper", "leftLegLower"
    );

    public static final List<String> ALL = List.of(
            "head", "torsoUpper", "torsoLower",
            "rightArmUpper", "rightArmLower", "leftArmUpper", "leftArmLower",
            "rightLegUpper", "rightLegLower", "leftLegUpper", "leftLegLower",
            "right_item", "left_item"
    );

    private BodyParts() {}
}

