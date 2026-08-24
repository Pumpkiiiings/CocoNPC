package com.pumpkings.coconpc.core.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Map;
import java.util.TreeMap;

public class Animation {
    private final String name;
    private final boolean loop;
    private final float length;
    private final Map<String, Bone> bones;

    public Animation(String name, boolean loop, float length, Map<String, Bone> bones) {
        this.name = name;
        this.loop = loop;
        this.length = length;
        this.bones = bones;
    }

    public String getName() { return name; }
    public boolean isLoop() { return loop; }
    public float getLength() { return length; }
    public Map<String, Bone> getBones() { return bones; }

    public static class Bone {
        private final TreeMap<Float, Keyframe> rotationFrames;
        private final TreeMap<Float, Keyframe> positionFrames;

        public Bone(TreeMap<Float, Keyframe> rotationFrames, TreeMap<Float, Keyframe> positionFrames) {
            this.rotationFrames = rotationFrames;
            this.positionFrames = positionFrames;
        }

        public TreeMap<Float, Keyframe> getRotationFrames() { return rotationFrames; }
        public TreeMap<Float, Keyframe> getPositionFrames() { return positionFrames; }
    }

    public static class Keyframe {
        private final float time;
        private final Vector3f value;
        private final String lerpMode;

        public Keyframe(float time, Vector3f value, String lerpMode) {
            this.time = time;
            this.value = value;
            this.lerpMode = lerpMode;
        }

        public float getTime() { return time; }
        public Vector3f getValue() { return value; }
        public String getLerpMode() { return lerpMode; }
    }
}

