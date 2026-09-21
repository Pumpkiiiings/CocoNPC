package com.pumpkings.coconpc.core.npc.selection;

import org.joml.Vector3f;

public class GizmoMath {

    /**
     * Calculates the shortest distance between a ray and a line segment.
     * @param rayStart Origin of the ray (e.g. player eye location)
     * @param rayDir Direction of the ray (normalized)
     * @param segStart Start point of the segment
     * @param segEnd End point of the segment
     * @return Shortest distance between the ray and the segment.
     */
    public static double distanceRayToSegment(Vector3f rayStart, Vector3f rayDir, Vector3f segStart, Vector3f segEnd) {
        Vector3f segDir = new Vector3f(segEnd).sub(segStart);
        Vector3f w0 = new Vector3f(rayStart).sub(segStart);

        float a = rayDir.dot(rayDir);
        float b = rayDir.dot(segDir);
        float c = segDir.dot(segDir);
        float d = rayDir.dot(w0);
        float e = segDir.dot(w0);

        float denominator = a * c - b * b;
        float sc, tc;

        if (denominator < 1e-5f) {
            sc = 0.0f;
            tc = (b > c ? d / b : e / c);
        } else {
            sc = (b * e - c * d) / denominator;
            tc = (a * e - b * d) / denominator;
        }

        if (tc < 0.0f) {
            tc = 0.0f;
            sc = -d / a;
        } else if (tc > 1.0f) {
            tc = 1.0f;
            sc = (-d + b) / a;
        }

        if (sc < 0.0f) {
            sc = 0.0f;
        }

        Vector3f rayPoint = new Vector3f(rayDir).mul(sc).add(rayStart);
        Vector3f segPoint = new Vector3f(segDir).mul(tc).add(segStart);
        
        return rayPoint.distance(segPoint);
    }

    /**
     * Calculates the shortest distance between a ray and a 3D ring.
     * Approximates the ring with multiple segments.
     * @param rayStart Origin of the ray
     * @param rayDir Direction of the ray
     * @param center Center of the ring
     * @param normal Normal vector of the ring plane
     * @param radius Radius of the ring
     * @param segments Number of segments to approximate the ring
     * @return Shortest distance
     */
    public static double distanceRayToRing(Vector3f rayStart, Vector3f rayDir, Vector3f center, Vector3f normal, float radius, int segments) {
        Vector3f u = new Vector3f();
        if (Math.abs(normal.x) > Math.abs(normal.y)) {
            u.set(-normal.z, 0, normal.x).normalize();
        } else {
            u.set(0, normal.z, -normal.y).normalize();
        }
        Vector3f v = new Vector3f(normal).cross(u).normalize();

        double minDistance = Double.MAX_VALUE;
        Vector3f prevPoint = null;
        Vector3f firstPoint = null;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2.0 * Math.PI / segments);
            Vector3f point = new Vector3f(center)
                    .add(new Vector3f(u).mul((float) (Math.cos(angle) * radius)))
                    .add(new Vector3f(v).mul((float) (Math.sin(angle) * radius)));

            if (i == 0) {
                firstPoint = point;
                prevPoint = point;
            } else {
                double d = distanceRayToSegment(rayStart, rayDir, prevPoint, point);
                if (d < minDistance) {
                    minDistance = d;
                }
                prevPoint = point;
            }
        }
        return minDistance;
    }
}

