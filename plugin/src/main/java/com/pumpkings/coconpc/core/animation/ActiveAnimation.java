package com.pumpkings.coconpc.core.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.TreeMap;

public class ActiveAnimation {
    private final Animation animation;
    private float currentTime = 0f;
    private boolean finished = false;

    public ActiveAnimation(Animation animation) {
        this.animation = animation;
    }

    public void tick(float deltaSeconds) {
        if (finished) return;
        currentTime += deltaSeconds;

        if (currentTime >= animation.getLength()) {
            if (animation.isLoop()) {
                currentTime = currentTime % animation.getLength();
            } else {
                finished = true;
                currentTime = animation.getLength();
            }
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public Animation getAnimation() {
        return animation;
    }

    public Quaternionf getBoneRotation(String boneName) {
        Animation.Bone bone = animation.getBones().get(boneName);
        if (bone == null || bone.getRotationFrames().isEmpty()) return new Quaternionf();

        TreeMap<Float, Animation.Keyframe> frames = bone.getRotationFrames();
        Map.Entry<Float, Animation.Keyframe> floor = frames.floorEntry(currentTime);
        Map.Entry<Float, Animation.Keyframe> ceiling = frames.ceilingEntry(currentTime);

        if (floor == null && ceiling != null) floor = ceiling;
        if (ceiling == null && floor != null) ceiling = floor;
        if (floor == null) return new Quaternionf();

        if (floor.getKey().equals(ceiling.getKey())) {
            Vector3f val = floor.getValue().getValue();
            return new Quaternionf().rotationXYZ(
                    (float) Math.toRadians(val.x),
                    (float) Math.toRadians(val.y),
                    (float) Math.toRadians(val.z)
            );
        }

        float factor = (currentTime - floor.getKey()) / (ceiling.getKey() - floor.getKey());
        
        Vector3f floorVal = floor.getValue().getValue();
        Vector3f ceilVal = ceiling.getValue().getValue();

        Quaternionf q1 = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(floorVal.x),
                (float) Math.toRadians(floorVal.y),
                (float) Math.toRadians(floorVal.z)
        );
        Quaternionf q2 = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(ceilVal.x),
                (float) Math.toRadians(ceilVal.y),
                (float) Math.toRadians(ceilVal.z)
        );

        return q1.slerp(q2, factor);
    }

    public Vector3f getBonePosition(String boneName) {
        Animation.Bone bone = animation.getBones().get(boneName);
        if (bone == null || bone.getPositionFrames().isEmpty()) return new Vector3f();

        TreeMap<Float, Animation.Keyframe> frames = bone.getPositionFrames();
        Map.Entry<Float, Animation.Keyframe> floor = frames.floorEntry(currentTime);
        Map.Entry<Float, Animation.Keyframe> ceiling = frames.ceilingEntry(currentTime);

        if (floor == null && ceiling != null) floor = ceiling;
        if (ceiling == null && floor != null) ceiling = floor;
        if (floor == null) return new Vector3f();

        if (floor.getKey().equals(ceiling.getKey())) {
            return new Vector3f(floor.getValue().getValue());
        }

        float factor = (currentTime - floor.getKey()) / (ceiling.getKey() - floor.getKey());
        Vector3f floorVal = floor.getValue().getValue();
        Vector3f ceilVal = ceiling.getValue().getValue();

        return new Vector3f(floorVal).lerp(ceilVal, factor);
    }
}

