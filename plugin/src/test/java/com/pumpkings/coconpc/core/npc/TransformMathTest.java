package com.pumpkings.coconpc.core.npc;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransformMathTest {
    @Test
    void normalizesAnglesToStableRange() {
        assertThat(TransformMath.normalizeDegrees(540f)).isEqualTo(-180f);
        assertThat(TransformMath.normalizeDegrees(-181f)).isEqualTo(179f);
        assertThat(TransformMath.normalizeDegrees(45f)).isEqualTo(45f);
    }

    @Test
    void localRotationRecomposesToOriginalWorldRotation() {
        Quaternionf parent = TransformMath.rotation(35f, -70f, 20f);
        Quaternionf expectedWorld = TransformMath.rotation(-15f, 40f, 95f);
        Quaternionf local = TransformMath.localFromWorld(parent, expectedWorld);
        Quaternionf recomposed = new Quaternionf(parent).mul(local).normalize();

        assertThat(Math.abs(recomposed.dot(expectedWorld))).isCloseTo(1f, within(0.0001f));
    }

    @Test
    void quaternionEulerRoundTripPreservesOrientation() {
        Quaternionf expected = TransformMath.rotation(25f, 50f, -30f);
        Vector3f degrees = TransformMath.degrees(expected);
        Quaternionf actual = TransformMath.rotation(degrees.x, degrees.y, degrees.z);

        assertThat(Math.abs(actual.dot(expected))).isCloseTo(1f, within(0.0001f));
    }

    private static org.assertj.core.data.Offset<Float> within(float tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }
}
