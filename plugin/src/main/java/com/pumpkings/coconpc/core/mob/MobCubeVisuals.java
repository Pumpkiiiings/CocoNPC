package com.pumpkings.coconpc.core.mob;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe map of per-cube visuals. The texture service fills this in the
 * background; the model rig reads it when displays are (re)created.
 */
public final class MobCubeVisuals {
    private final Map<String, BoneVisual> visuals = new ConcurrentHashMap<>();

    public void put(String cubeKey, BoneVisual visual) {
        if (cubeKey == null || visual == null || visual.isNone()) return;
        visuals.put(cubeKey, visual);
    }

    public BoneVisual get(String cubeKey) {
        return cubeKey == null ? null : visuals.get(cubeKey);
    }

    public boolean contains(String cubeKey) {
        return cubeKey != null && visuals.containsKey(cubeKey);
    }

    public Map<String, BoneVisual> snapshot() {
        return Map.copyOf(visuals);
    }

    public int size() {
        return visuals.size();
    }

    public void clear() {
        visuals.clear();
    }
}
