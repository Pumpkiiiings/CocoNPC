package com.pumpkings.coconpc.core.animation;

import com.google.gson.Gson;
import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.animation.model.AnimationModel;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class AnimationManager {
    private final CocoNPC plugin;
    private final Map<String, Animation> loadedAnimations = new HashMap<>();
    private final Gson gson = new Gson();

    public AnimationManager(CocoNPC plugin) {
        this.plugin = plugin;
    }

    public void loadAnimations() {
        loadedAnimations.clear();
        File animFolder = new File(plugin.getDataFolder(), "animations");
        if (!animFolder.exists()) {
            animFolder.mkdirs();
            return;
        }

        File[] files = animFolder.listFiles((dir, name) -> name.endsWith(".json") || name.endsWith(".bbmodel"));
        if (files == null) return;

        for (File file : files) {
            if (file.getName().endsWith(".bbmodel")) {
                Map<String, Animation> parsed = com.pumpkings.coconpc.core.animation.model.BbModelParser.parse(file, plugin.getLogger());
                loadedAnimations.putAll(parsed);
                continue;
            }

            try (FileReader reader = new FileReader(file)) {
                AnimationModel model = gson.fromJson(reader, AnimationModel.class);
                if (model == null || model.animations == null) continue;

                for (Map.Entry<String, AnimationModel.AnimationData> entry : model.animations.entrySet()) {
                    String animId = entry.getKey();
                    AnimationModel.AnimationData data = entry.getValue();

                    Map<String, Animation.Bone> bones = new HashMap<>();
                    for (Map.Entry<String, AnimationModel.BoneAnimation> boneEntry : data.bones.entrySet()) {
                        TreeMap<Float, Animation.Keyframe> rotFrames = parseKeyframes(boneEntry.getValue().rotation);
                        TreeMap<Float, Animation.Keyframe> posFrames = parseKeyframes(boneEntry.getValue().position);
                        bones.put(boneEntry.getKey(), new Animation.Bone(rotFrames, posFrames));
                    }

                    loadedAnimations.put(animId, new Animation(animId, data.loop, data.animationLength, bones));
                    
                    String shortName = animId.replace("animation.model.", "").replace("animation.", "");
                    if (!shortName.equals(animId)) {
                        loadedAnimations.put(shortName, new Animation(shortName, data.loop, data.animationLength, bones));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load animation file " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + loadedAnimations.size() + " animations.");
    }

    private TreeMap<Float, Animation.Keyframe> parseKeyframes(Map<String, AnimationModel.KeyframeData> rawFrames) {
        TreeMap<Float, Animation.Keyframe> tree = new TreeMap<>();
        if (rawFrames == null) return tree;

        for (Map.Entry<String, AnimationModel.KeyframeData> entry : rawFrames.entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                float[] post = entry.getValue().post;
                if (post != null && post.length >= 3) {
                    Vector3f val = new Vector3f(post[0], post[1], post[2]);
                    tree.put(time, new Animation.Keyframe(time, val, entry.getValue().lerpMode));
                }
            } catch (NumberFormatException ignored) {}
        }
        return tree;
    }

    public Animation getAnimation(String id) {
        return loadedAnimations.get(id);
    }
}

