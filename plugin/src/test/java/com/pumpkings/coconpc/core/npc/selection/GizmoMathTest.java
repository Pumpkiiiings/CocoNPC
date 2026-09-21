package com.pumpkings.coconpc.core.npc.selection;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class GizmoMathTest {
    @Test
    void detectsRayCrossingAnAxisSegment() {
        double distance = GizmoMath.distanceRayToSegment(
                new Vector3f(0.5f, 1f, 0f),
                new Vector3f(0f, -1f, 0f),
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f));

        assertThat(distance).isCloseTo(0.0, offset(0.0001));
    }

    @Test
    void keepsBehindCameraSegmentsOutOfReach() {
        double distance = GizmoMath.distanceRayToSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(-2f, 1f, 0f),
                new Vector3f(-1f, 1f, 0f));

        assertThat(distance).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void detectsRayCrossingRotationRing() {
        double distance = GizmoMath.distanceRayToRing(
                new Vector3f(1f, 2f, 0f),
                new Vector3f(0f, -1f, 0f),
                new Vector3f(),
                new Vector3f(0f, 1f, 0f),
                1f,
                32);

        assertThat(distance).isCloseTo(0.0, offset(0.01));
    }
}
