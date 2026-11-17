package com.pumpkings.coconpc.core.mob.model;

import java.util.ArrayList;
import java.util.List;

public final class RigAnimation {
    private final BbAnimation clip;
    private final ModelBone[] bones;
    private final BbAnimation.BoneTrack[] tracks;
    private final boolean additiveRotation;

    RigAnimation(ModelRig rig, BbAnimation clip, boolean additiveRotation) {
        this.clip = clip;
        this.additiveRotation = additiveRotation;

        List<ModelBone> matchedBones = new ArrayList<>();
        List<BbAnimation.BoneTrack> matchedTracks = new ArrayList<>();

        for (BbAnimation.BoneTrack track : clip.getTracks()) {
            ModelBone bone = rig.findBone(track.getBoneUuid(), track.getBoneName());
            if (bone == null) continue;
            matchedBones.add(bone);
            matchedTracks.add(track);
        }

        this.bones = matchedBones.toArray(new ModelBone[0]);
        this.tracks = matchedTracks.toArray(new BbAnimation.BoneTrack[0]);
    }

    public void sample(float time) {
        for (int index = 0; index < tracks.length; index++) {
            ModelBone bone = bones[index];
            BbAnimation.BoneTrack track = tracks[index];

            if (track.hasRotation()
                    && track.sampleRotation(time, bone.getSampledRotation())) {
                bone.setAnimRotation(bone.getSampledRotation(), additiveRotation);
            }
            if (track.hasPosition()) {
                track.samplePosition(time, bone.getAnimTranslation());
            }
            if (track.hasScale()) {
                track.sampleScale(time, bone.getAnimScale());
            }
        }
    }

    public String getName() {
        return clip.getName();
    }

    public float getLength() {
        return clip.getLength();
    }

    public boolean isLoop() {
        return clip.isLoop();
    }

    public int getTrackCount() {
        return tracks.length;
    }
}
