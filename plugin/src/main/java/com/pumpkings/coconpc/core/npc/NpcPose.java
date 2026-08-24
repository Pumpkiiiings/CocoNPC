package com.pumpkings.coconpc.core.npc;

import com.pumpkings.coconpc.menu.EditorTarget;

public enum NpcPose {
    STAND("stand") {
        @Override
        public void apply(NpcEntity npc) {
            npc.resetAllRotations();
        }
    },
    SIT("sit") {
        @Override
        public void apply(NpcEntity npc) {
            npc.resetAllRotations();
            npc.setOffset(EditorTarget.GLOBAL, 0f, -0.65f * npc.getSize(), 0f);
            npc.setPartRotation("rightLegUpper", -90f, 0f, 0f);
            npc.setPartRotation("leftLegUpper", -90f, 0f, 0f);
            npc.setPartRotation("rightLegLower", 0f, 0f, 0f);
            npc.setPartRotation("leftLegLower", 0f, 0f, 0f);
            npc.setPartRotation("rightArmUpper", -30f, 0f, 0f);
            npc.setPartRotation("rightArmLower", -30f, 0f, 0f);
            npc.setPartRotation("leftArmUpper", -30f, 0f, 0f);
            npc.setPartRotation("leftArmLower", -30f, 0f, 0f);
        }
    },
    LAY("lay") {
        @Override
        public void apply(NpcEntity npc) {
            npc.resetAllRotations();
            npc.setOffset(EditorTarget.GLOBAL, 0f, -0.95f * npc.getSize(), 0f);
            String[] allParts = {"head", "torsoUpper", "torsoLower", "rightArmUpper", "rightArmLower", "leftArmUpper", "leftArmLower", "rightLegUpper", "rightLegLower", "leftLegUpper", "leftLegLower"};
            for (String part : allParts) {
                npc.setPartRotation(part, -90f, 0f, 0f);
            }
        }
    },
    WAVE("wave") {
        @Override
        public void apply(NpcEntity npc) {
            npc.resetAllRotations();
            npc.setPartRotation("head", -5f, -10f, 0f);
            npc.setPartRotation("rightArmUpper", -150f, -20f, 0f);
            npc.setPartRotation("rightArmLower", -130f, -40f, 10f);
        }
    },
    SALUTE("salute") {
        @Override
        public void apply(NpcEntity npc) {
            npc.resetAllRotations();
            
            npc.setPartRotation("head", -5f, 0f, 0f);
            npc.setPartRotation("rightArmUpper", -100f, -45f, 0f);
            npc.setPartRotation("rightArmLower", -140f, 50f, 0f);
        }
    },
    CROSSED_ARMS("crossed_arms") {
        @Override
        public void apply(NpcEntity npc) {
            npc.resetAllRotations();
            npc.setPartRotation("rightArmUpper", -60f, -35f, 0f);
            npc.setPartRotation("rightArmLower", -75f, -65f, 0f);
            npc.setPartRotation("leftArmUpper", -60f, 35f, 0f);
            npc.setPartRotation("leftArmLower", -75f, 65f, 0f);
        }
    };

    private final String name;

    NpcPose(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void apply(NpcEntity npc);

    public static NpcPose fromName(String name) {
        for (NpcPose pose : values()) {
            if (pose.getName().equalsIgnoreCase(name) || pose.name().equalsIgnoreCase(name)) {
                return pose;
            }
        }
        return null;
    }
}

