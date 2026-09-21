package com.pumpkings.coconpc.core.npc;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TransformMath {
    private TransformMath() {
    }

    public static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0f;
        if (normalized >= 180.0f) normalized -= 360.0f;
        if (normalized < -180.0f) normalized += 360.0f;
        return normalized;
    }

    public static Quaternionf rotation(float pitch, float yaw, float roll) {
        return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(pitch),
                (float) Math.toRadians(yaw),
                (float) Math.toRadians(roll));
    }

    public static Quaternionf localFromWorld(Quaternionf parentWorld, Quaternionf childWorld) {
        return new Quaternionf(parentWorld).invert().mul(childWorld).normalize();
    }

    public static Vector3f degrees(Quaternionf rotation) {
        Vector3f radians = rotation.getEulerAnglesXYZ(new Vector3f());
        return radians.mul((float) (180.0 / Math.PI));
    }
}
