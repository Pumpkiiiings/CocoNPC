package com.pumpkings.coconpc.core.animation.model;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;

public class BlockbenchModel {
    public Meta meta;
    public String name;
    public List<OutlinerNode> outliner;
    public List<Animation> animations;

    public static class Meta {
        public String format_version;
    }

    public static class OutlinerNode {
        public String name;
        public String uuid;
        public float[] origin;
        public float[] rotation;
        public List<JsonElement> children;
    }

    public static class Animation {
        public String name;
        public String uuid;
        public String loop; // "loop", "once", "hold"
        public float length;
        public Map<String, Animator> animators; // Key is bone UUID
    }

    public static class Animator {
        public String name;
        public List<Keyframe> keyframes;
    }

    public static class Keyframe {
        public String channel; // "rotation", "position"
        public float time;
        public List<DataPoint> data_points;
    }

    public static class DataPoint {
        public JsonElement x;
        public JsonElement y;
        public JsonElement z;

        public float getX() {
            try { return x != null ? x.getAsFloat() : 0f; } catch (Exception e) { return 0f; }
        }
        public float getY() {
            try { return y != null ? y.getAsFloat() : 0f; } catch (Exception e) { return 0f; }
        }
        public float getZ() {
            try { return z != null ? z.getAsFloat() : 0f; } catch (Exception e) { return 0f; }
        }
    }
}
