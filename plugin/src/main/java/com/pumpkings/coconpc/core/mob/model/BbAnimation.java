package com.pumpkings.coconpc.core.mob.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BbAnimation {
    private final String name;
    private final boolean loop;
    private final float length;
    private final List<BoneTrack> tracks;

    BbAnimation(String name, boolean loop, float length, List<BoneTrack> tracks) {
        this.name = name;
        this.loop = loop;
        this.length = Math.max(0.05f, length);
        this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
    }

    public String getName() {
        return name;
    }

    public boolean isLoop() {
        return loop;
    }

    public float getLength() {
        return length;
    }

    public List<BoneTrack> getTracks() {
        return tracks;
    }

    public static final class BoneTrack {
        private final String boneName;
        private final String boneUuid;
        private final float[] rotationTimes;
        private final Quaternionf[] rotationValues;
        private final float[] positionTimes;
        private final Vector3f[] positionValues;
        private final float[] scaleTimes;
        private final Vector3f[] scaleValues;

        BoneTrack(String boneName, String boneUuid,
                  TreeMap<Float, Vector3f> rotation,
                  TreeMap<Float, Vector3f> position,
                  TreeMap<Float, Vector3f> scale) {
            this.boneName = boneName;
            this.boneUuid = boneUuid;

            this.rotationTimes = new float[rotation.size()];
            this.rotationValues = new Quaternionf[rotation.size()];
            int rotationIndex = 0;
            for (Map.Entry<Float, Vector3f> entry : rotation.entrySet()) {
                Vector3f value = entry.getValue();
                rotationTimes[rotationIndex] = entry.getKey();
                rotationValues[rotationIndex] = new Quaternionf().rotationXYZ(
                        (float) Math.toRadians(value.x),
                        (float) Math.toRadians(value.y),
                        (float) Math.toRadians(value.z));
                rotationIndex++;
            }

            this.positionTimes = new float[position.size()];
            this.positionValues = new Vector3f[position.size()];
            int positionIndex = 0;
            for (Map.Entry<Float, Vector3f> entry : position.entrySet()) {
                positionTimes[positionIndex] = entry.getKey();
                positionValues[positionIndex] = new Vector3f(entry.getValue());
                positionIndex++;
            }

            this.scaleTimes = new float[scale.size()];
            this.scaleValues = new Vector3f[scale.size()];
            int scaleIndex = 0;
            for (Map.Entry<Float, Vector3f> entry : scale.entrySet()) {
                scaleTimes[scaleIndex] = entry.getKey();
                scaleValues[scaleIndex] = new Vector3f(entry.getValue());
                scaleIndex++;
            }
        }

        public String getBoneName() {
            return boneName;
        }

        public String getBoneUuid() {
            return boneUuid;
        }

        public boolean hasRotation() {
            return rotationTimes.length > 0;
        }

        public boolean hasPosition() {
            return positionTimes.length > 0;
        }

        public boolean hasScale() {
            return scaleTimes.length > 0;
        }

        public float getLastTime() {
            float last = 0f;
            if (rotationTimes.length > 0) {
                last = Math.max(last, rotationTimes[rotationTimes.length - 1]);
            }
            if (positionTimes.length > 0) {
                last = Math.max(last, positionTimes[positionTimes.length - 1]);
            }
            if (scaleTimes.length > 0) {
                last = Math.max(last, scaleTimes[scaleTimes.length - 1]);
            }
            return last;
        }

        public boolean sampleRotation(float time, Quaternionf out) {
            if (rotationTimes.length == 0) return false;

            int next = insertionPoint(rotationTimes, time);
            if (next == 0) {
                out.set(rotationValues[0]);
                return true;
            }
            if (next >= rotationTimes.length) {
                out.set(rotationValues[rotationTimes.length - 1]);
                return true;
            }

            int previous = next - 1;
            float span = rotationTimes[next] - rotationTimes[previous];
            float factor = span <= 0.00001f ? 1f : (time - rotationTimes[previous]) / span;
            out.set(rotationValues[previous]).slerp(rotationValues[next], factor);
            return true;
        }

        public boolean samplePosition(float time, Vector3f out) {
            if (positionTimes.length == 0) return false;

            int next = insertionPoint(positionTimes, time);
            if (next == 0) {
                out.set(positionValues[0]);
                return true;
            }
            if (next >= positionTimes.length) {
                out.set(positionValues[positionTimes.length - 1]);
                return true;
            }

            int previous = next - 1;
            float span = positionTimes[next] - positionTimes[previous];
            float factor = span <= 0.00001f ? 1f : (time - positionTimes[previous]) / span;
            out.set(positionValues[previous]).lerp(positionValues[next], factor);
            return true;
        }

        public boolean sampleScale(float time, Vector3f out) {
            if (scaleTimes.length == 0) return false;

            int next = insertionPoint(scaleTimes, time);
            if (next == 0) {
                out.set(scaleValues[0]);
                return true;
            }
            if (next >= scaleTimes.length) {
                out.set(scaleValues[scaleValues.length - 1]);
                return true;
            }

            int previous = next - 1;
            float span = scaleTimes[next] - scaleTimes[previous];
            float factor = span <= 0.00001f ? 1f : (time - scaleTimes[previous]) / span;
            out.set(scaleValues[previous]).lerp(scaleValues[next], factor);
            return true;
        }

        private static int insertionPoint(float[] times, float time) {
            int low = 0;
            int high = times.length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (times[middle] < time) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }
    }
}
