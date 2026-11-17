package com.pumpkings.coconpc.core.mob;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class MobAnimationSet {
    private final Map<MobAnimationState, String> names;
    private final float idleThreshold;
    private final float walkReferenceSpeed;
    private final float minWalkPlayback;
    private final float maxWalkPlayback;
    private final int hurtFlashTicks;
    private final boolean additiveRotation;
    private final boolean attackOnAggro;

    private MobAnimationSet(Map<MobAnimationState, String> names,
                            float idleThreshold,
                            float walkReferenceSpeed,
                            float minWalkPlayback,
                            float maxWalkPlayback,
                            int hurtFlashTicks,
                            boolean additiveRotation,
                            boolean attackOnAggro) {
        this.names = Collections.unmodifiableMap(new EnumMap<>(names));
        this.idleThreshold = idleThreshold;
        this.walkReferenceSpeed = walkReferenceSpeed;
        this.minWalkPlayback = minWalkPlayback;
        this.maxWalkPlayback = maxWalkPlayback;
        this.hurtFlashTicks = hurtFlashTicks;
        this.additiveRotation = additiveRotation;
        this.attackOnAggro = attackOnAggro;
    }

    public static MobAnimationSet defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName(MobAnimationState state) {
        return names.get(state);
    }

    public Map<MobAnimationState, String> getNames() {
        return names;
    }

    public float getIdleThreshold() {
        return idleThreshold;
    }

    public float getWalkReferenceSpeed() {
        return walkReferenceSpeed;
    }

    public float getMinWalkPlayback() {
        return minWalkPlayback;
    }

    public float getMaxWalkPlayback() {
        return maxWalkPlayback;
    }

    public int getHurtFlashTicks() {
        return hurtFlashTicks;
    }

    public boolean isAdditiveRotation() {
        return additiveRotation;
    }

    public boolean isAttackOnAggro() {
        return attackOnAggro;
    }

    public static final class Builder {
        private final Map<MobAnimationState, String> names = new EnumMap<>(MobAnimationState.class);
        private float idleThreshold = 0.02f;
        private float walkReferenceSpeed = 0.2f;
        private float minWalkPlayback = 0.35f;
        private float maxWalkPlayback = 2.5f;
        private int hurtFlashTicks = 6;
        private boolean additiveRotation;
        private boolean attackOnAggro = true;

        private Builder() {
            names.put(MobAnimationState.IDLE, "idle");
            names.put(MobAnimationState.WALK, "walk");
            names.put(MobAnimationState.ATTACK, "attack");
            names.put(MobAnimationState.HURT, "hurt");
            names.put(MobAnimationState.DEATH, "death");
        }

        public Builder idle(String animationName) {
            names.put(MobAnimationState.IDLE, animationName);
            return this;
        }

        public Builder walk(String animationName) {
            names.put(MobAnimationState.WALK, animationName);
            return this;
        }

        public Builder attack(String animationName) {
            names.put(MobAnimationState.ATTACK, animationName);
            return this;
        }

        public Builder hurt(String animationName) {
            names.put(MobAnimationState.HURT, animationName);
            return this;
        }

        public Builder death(String animationName) {
            names.put(MobAnimationState.DEATH, animationName);
            return this;
        }

        public Builder idleThreshold(float threshold) {
            this.idleThreshold = Math.max(0f, threshold);
            return this;
        }

        public Builder walkReferenceSpeed(float speed) {
            this.walkReferenceSpeed = Math.max(0.01f, speed);
            return this;
        }

        public Builder walkPlaybackRange(float min, float max) {
            this.minWalkPlayback = Math.max(0.01f, min);
            this.maxWalkPlayback = Math.max(this.minWalkPlayback, max);
            return this;
        }

        public Builder hurtFlashTicks(int ticks) {
            this.hurtFlashTicks = Math.max(0, ticks);
            return this;
        }

        public Builder additiveRotation(boolean additive) {
            this.additiveRotation = additive;
            return this;
        }

        public Builder attackOnAggro(boolean enabled) {
            this.attackOnAggro = enabled;
            return this;
        }

        public MobAnimationSet build() {
            return new MobAnimationSet(names, idleThreshold, walkReferenceSpeed,
                    minWalkPlayback, maxWalkPlayback, hurtFlashTicks,
                    additiveRotation, attackOnAggro);
        }
    }
}
