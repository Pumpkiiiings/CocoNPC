package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.core.mob.model.BbAnimation;
import com.pumpkings.coconpc.core.mob.model.BbModel;
import com.pumpkings.coconpc.core.mob.model.ModelRig;
import com.pumpkings.coconpc.core.mob.model.RigAnimation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Drives the pose of a {@link ModelRig} from the clips of a .bbmodel.
 *
 * <p>Besides the fixed locomotion states ({@link MobAnimationState}), every
 * animation present in the model is available by name so plugins or commands can
 * play one manually, either as a loop or as a one-shot that falls back to the
 * normal idle/walk flow.</p>
 */
public final class AnimationController {
    private final ModelRig rig;
    private final MobAnimationSet animationSet;
    private final Map<MobAnimationState, RigAnimation> animations;
    private final Map<String, RigAnimation> customAnimations;
    private final Map<String, String> customNames;

    private MobAnimationState state = MobAnimationState.IDLE;
    private RigAnimation current;
    private float time;
    private boolean oneShotFinished;
    private boolean deathComplete;

    private RigAnimation customCurrent;
    private String customName;
    private boolean customLooping;
    private float lastHorizontalSpeed;

    public AnimationController(ModelRig rig, MobDefinition definition) {
        this.rig = rig;
        this.animationSet = definition.getAnimationSet();

        boolean additive = animationSet.isAdditiveRotation();

        Map<MobAnimationState, RigAnimation> loaded = new EnumMap<>(MobAnimationState.class);
        for (MobAnimationState animationState : MobAnimationState.values()) {
            if (definition.getAnimation(animationState) == null) continue;
            loaded.put(animationState, rig.createAnimation(
                    definition.getAnimation(animationState), additive));
        }
        this.animations = Collections.unmodifiableMap(loaded);

        Map<String, RigAnimation> custom = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        BbModel model = definition.getModel();
        if (model != null) {
            for (Map.Entry<String, BbAnimation> entry : model.getAnimations().entrySet()) {
                BbAnimation clip = entry.getValue();
                if (clip == null) continue;

                String rawName = entry.getKey() != null ? entry.getKey() : clip.getName();
                String key = normalize(rawName);
                if (key.isEmpty() || custom.containsKey(key)) continue;

                custom.put(key, rig.createAnimation(clip, additive));
                names.put(key, clip.getName() != null ? clip.getName() : rawName);
            }
        }
        this.customAnimations = Collections.unmodifiableMap(custom);
        this.customNames = Collections.unmodifiableMap(names);

        play(MobAnimationState.IDLE);
    }

    public void tick(float deltaSeconds, float horizontalSpeed) {
        float delta = Math.max(0f, Math.min(deltaSeconds, 0.25f));
        lastHorizontalSpeed = Math.max(0f, horizontalSpeed);

        if (isCustomActive()) {
            tickCustom(delta);
            return;
        }

        if (state == MobAnimationState.DEATH) {
            if (current == null) {
                deathComplete = true;
                return;
            }
            boolean finished = advance(delta, 1f);
            current.sample(time);
            if (finished) {
                deathComplete = true;
            }
            return;
        }

        if (state == MobAnimationState.ATTACK || state == MobAnimationState.HURT) {
            if (current == null || oneShotFinished) {
                transitionToLocomotion(horizontalSpeed);
                return;
            }

            boolean finished = advance(delta, 1f);
            current.sample(time);
            if (finished) {
                oneShotFinished = true;
            }
            return;
        }

        MobAnimationState nextState = locomotionState(horizontalSpeed);
        if (nextState != state) {
            play(nextState);
        }

        float playback = 1f;
        if (state == MobAnimationState.WALK) {
            playback = horizontalSpeed / animationSet.getWalkReferenceSpeed();
            playback = clamp(playback,
                    animationSet.getMinWalkPlayback(),
                    animationSet.getMaxWalkPlayback());
        }

        advance(delta, playback);
        if (current != null) {
            current.sample(time);
        }
    }

    private void tickCustom(float delta) {
        if (customCurrent == null) {
            stopCustomAnimation();
            return;
        }

        float length = customCurrent.getLength();
        time += delta;

        if (customLooping) {
            if (length > 0f && time >= length) {
                time %= length;
            }
            customCurrent.sample(time);
            return;
        }

        if (time >= length) {
            time = length;
            customCurrent.sample(time);
            stopCustomAnimation();
            return;
        }
        customCurrent.sample(time);
    }

    /**
     * Plays any animation of the model by name (case-insensitive).
     *
     * @param name animation name
     * @param loop {@code true} to keep looping until stopped or replaced,
     *             {@code false} to fall back to idle/walk when it finishes
     * @return {@code true} when the animation exists and started playing
     */
    public boolean playCustomAnimation(String name, boolean loop) {
        if (state == MobAnimationState.DEATH) return false;

        RigAnimation animation = findCustomAnimation(name);
        if (animation == null) return false;

        customCurrent = animation;
        customName = animation.getName();
        customLooping = loop;
        time = 0f;
        oneShotFinished = false;

        rig.resetPose();
        animation.sample(0f);
        return true;
    }

    /**
     * Stops a manually played animation and returns to idle/walk.
     *
     * @return {@code true} if a custom animation was active
     */
    public boolean stopCustomAnimation() {
        if (customCurrent == null) return false;

        play(locomotionState(lastHorizontalSpeed));
        return true;
    }

    public boolean isCustomActive() {
        return customCurrent != null;
    }

    public boolean isCustomLooping() {
        return customCurrent != null && customLooping;
    }

    public String getCustomAnimationName() {
        return customCurrent != null ? customName : null;
    }

    public boolean hasCustomAnimation(String name) {
        return findCustomAnimation(name) != null;
    }

    public Set<String> getCustomAnimationNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(customNames.values()));
    }

    private RigAnimation findCustomAnimation(String name) {
        String key = normalize(name);
        if (key.isEmpty()) return null;

        RigAnimation exact = customAnimations.get(key);
        if (exact != null) return exact;

        for (Map.Entry<String, RigAnimation> entry : customAnimations.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String normalize(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT)
                .replace("animation.model.", "")
                .replace("animation.", "")
                .trim();
    }

    private boolean advance(float deltaSeconds, float playbackSpeed) {
        if (current == null) return true;

        time += deltaSeconds * playbackSpeed;
        float length = current.getLength();

        if (isLooping(state)) {
            if (time >= length) {
                time %= length;
            }
            return false;
        }

        if (time >= length) {
            time = length;
            return true;
        }
        return false;
    }

    private boolean isLooping(MobAnimationState animationState) {
        return animationState == MobAnimationState.IDLE
                || animationState == MobAnimationState.WALK;
    }

    private MobAnimationState locomotionState(float horizontalSpeed) {
        if (horizontalSpeed < animationSet.getIdleThreshold()) {
            return MobAnimationState.IDLE;
        }
        return animations.containsKey(MobAnimationState.WALK)
                ? MobAnimationState.WALK
                : MobAnimationState.IDLE;
    }

    private void transitionToLocomotion(float horizontalSpeed) {
        play(locomotionState(horizontalSpeed));
    }

    private void play(MobAnimationState nextState) {
        state = nextState;
        current = animations.get(nextState);
        time = 0f;
        oneShotFinished = false;

        customCurrent = null;
        customName = null;
        customLooping = false;

        rig.resetPose();
        if (current != null) {
            current.sample(0f);
        }
    }

    public boolean triggerAttack() {
        if (state == MobAnimationState.DEATH || state == MobAnimationState.HURT) {
            return false;
        }
        if (state == MobAnimationState.ATTACK) {
            return false;
        }
        if (!animations.containsKey(MobAnimationState.ATTACK)) {
            return false;
        }

        play(MobAnimationState.ATTACK);
        return true;
    }

    public boolean triggerHurt() {
        if (state == MobAnimationState.DEATH) {
            return false;
        }
        if (!animations.containsKey(MobAnimationState.HURT)) {
            return false;
        }

        play(MobAnimationState.HURT);
        return true;
    }

    public boolean triggerDeath() {
        if (state == MobAnimationState.DEATH) {
            return false;
        }

        deathComplete = false;
        play(MobAnimationState.DEATH);
        if (current == null) {
            deathComplete = true;
            return false;
        }
        return true;
    }

    public boolean isDeathComplete() {
        return deathComplete;
    }

    public MobAnimationState getState() {
        return state;
    }

    public float getTime() {
        return time;
    }

    public String getCurrentAnimationName() {
        if (customCurrent != null) {
            return customName;
        }
        return current != null ? current.getName() : null;
    }

    public int getAnimationCount() {
        return Math.max(animations.size(), customAnimations.size());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
