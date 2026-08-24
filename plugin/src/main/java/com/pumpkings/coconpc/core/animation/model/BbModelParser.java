package com.pumpkings.coconpc.core.animation.model;

import com.google.gson.Gson;
import com.pumpkings.coconpc.core.animation.Animation;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

public class BbModelParser {
    private static final Gson GSON = new Gson();

    public static Map<String, Animation> parse(File file, Logger logger) {
        Map<String, Animation> parsedAnimations = new HashMap<>();
        try (FileReader reader = new FileReader(file)) {
            BlockbenchModel model = GSON.fromJson(reader, BlockbenchModel.class);
            if (model == null || model.animations == null) return parsedAnimations;

            for (BlockbenchModel.Animation bbAnim : model.animations) {
                if (bbAnim.name == null || bbAnim.animators == null) continue;

                boolean loop = bbAnim.loop != null && bbAnim.loop.equalsIgnoreCase("loop");
                Map<String, Animation.Bone> bones = new HashMap<>();

                for (Map.Entry<String, BlockbenchModel.Animator> entry : bbAnim.animators.entrySet()) {
                    BlockbenchModel.Animator animator = entry.getValue();
                    if (animator.keyframes == null) continue;

                    TreeMap<Float, Animation.Keyframe> rotFrames = new TreeMap<>();
                    TreeMap<Float, Animation.Keyframe> posFrames = new TreeMap<>();

                    for (BlockbenchModel.Keyframe kf : animator.keyframes) {
                        if (kf.data_points == null || kf.data_points.isEmpty()) continue;
                        BlockbenchModel.DataPoint dp = kf.data_points.get(0);

                        Vector3f val = new Vector3f(dp.getX(), dp.getY(), dp.getZ());
                        Animation.Keyframe animKf = new Animation.Keyframe(kf.time, val, "linear");

                        if ("rotation".equals(kf.channel)) {
                            rotFrames.put(kf.time, animKf);
                        } else if ("position".equals(kf.channel)) {
                            posFrames.put(kf.time, animKf);
                        }
                    }

                    if (!rotFrames.isEmpty() || !posFrames.isEmpty()) {
                        bones.put(animator.name, new Animation.Bone(rotFrames, posFrames));
                    }
                }

                parsedAnimations.put(bbAnim.name, new Animation(bbAnim.name, loop, bbAnim.length, bones));
            }

            logger.info("[CocoNPC] Parsed .bbmodel: " + file.getName() + " with " + parsedAnimations.size() + " animations.");
        } catch (Exception e) {
            logger.severe("[CocoNPC] Failed to parse .bbmodel: " + file.getName());
            e.printStackTrace();
        }
        return parsedAnimations;
    }
}
