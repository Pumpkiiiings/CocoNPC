package com.pumpkings.coconpc.core.animation.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

public class AnimationModel {

    @SerializedName("format_version")
    public String formatVersion;

    @SerializedName("animations")
    public Map<String, AnimationData> animations = new HashMap<>();

    public static class AnimationData {
        public boolean loop;
        @SerializedName("animation_length")
        public float animationLength;
        public Map<String, BoneAnimation> bones = new HashMap<>();
    }

    public static class BoneAnimation {
        public Map<String, KeyframeData> rotation = new HashMap<>();
        public Map<String, KeyframeData> position = new HashMap<>();
    }

    public static class KeyframeData {
        public float[] post;
        @SerializedName("lerp_mode")
        public String lerpMode;
    }
}

