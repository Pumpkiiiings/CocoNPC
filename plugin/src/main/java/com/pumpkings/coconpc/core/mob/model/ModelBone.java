package com.pumpkings.coconpc.core.mob.model;

import com.pumpkings.coconpc.core.mob.BoneVisual;
import org.bukkit.entity.Display;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModelBone {
    private static final float MATRIX_EPSILON = 0.0001f;

    private final String name;
    private final String uuid;
    private final String paletteBone;
    private final ModelBone parent;
    private final List<ModelBone> children = new ArrayList<>();
    private final Vector3f restOffset;
    private final Quaternionf restRotation;
    private final Vector3f restScale;
    private final Matrix4f visual;
    private final BoneVisual visualType;

    private Display display;

    private final Vector3f animTranslation = new Vector3f();
    private final Quaternionf animRotation = new Quaternionf();
    private final Vector3f animScale = new Vector3f(1f, 1f, 1f);
    private final Quaternionf sampledRotation = new Quaternionf();

    private final Matrix4f localMatrix = new Matrix4f();
    private final Matrix4f worldMatrix = new Matrix4f();
    private final Matrix4f lastSentMatrix = new Matrix4f();
    private boolean hasSentTransform;

    ModelBone(String name, String uuid, String paletteBone, ModelBone parent,
              Vector3f restOffset, Quaternionf restRotation, Vector3f restScale,
              Matrix4f visual, BoneVisual visualType) {
        this.name = name;
        this.uuid = uuid;
        this.paletteBone = paletteBone;
        this.parent = parent;
        this.restOffset = new Vector3f(restOffset);
        this.restRotation = new Quaternionf(restRotation);
        this.restScale = new Vector3f(restScale);
        this.visual = new Matrix4f(visual);
        this.visualType = visualType;
        this.animRotation.set(restRotation);
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getPaletteBone() {
        return paletteBone;
    }

    public ModelBone getParent() {
        return parent;
    }

    public List<ModelBone> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Vector3f getRestOffset() {
        return new Vector3f(restOffset);
    }

    public Quaternionf getRestRotation() {
        return new Quaternionf(restRotation);
    }

    public Vector3f getRestScale() {
        return new Vector3f(restScale);
    }

    public Matrix4f getVisual() {
        return new Matrix4f(visual);
    }

    public BoneVisual getVisualType() {
        return visualType;
    }

    public Display getDisplay() {
        return display;
    }

    public boolean isRenderable() {
        return visualType != null && !visualType.isNone();
    }

    void addChild(ModelBone child) {
        if (child != null) children.add(child);
    }

    void setDisplay(Display display) {
        this.display = display;
        this.hasSentTransform = false;
    }

    void resetPose() {
        animTranslation.zero();
        animRotation.set(restRotation);
        animScale.set(1f, 1f, 1f);
    }

    void setAnimRotation(Quaternionf rotation, boolean additive) {
        if (additive) {
            animRotation.set(restRotation).mul(rotation).normalize();
        } else {
            animRotation.set(rotation).normalize();
        }
    }

    Quaternionf getSampledRotation() {
        return sampledRotation;
    }

    Vector3f getAnimTranslation() {
        return animTranslation;
    }

    Vector3f getAnimScale() {
        return animScale;
    }

    void update(Matrix4f parentWorld) {
        localMatrix.translation(
                        restOffset.x + animTranslation.x,
                        restOffset.y + animTranslation.y,
                        restOffset.z + animTranslation.z)
                .rotate(animRotation)
                .scale(restScale.x * animScale.x,
                        restScale.y * animScale.y,
                        restScale.z * animScale.z)
                .mul(visual);

        worldMatrix.set(parentWorld).mul(localMatrix);
        sendTransform();

        for (ModelBone child : children) {
            child.update(worldMatrix);
        }
    }

    private void sendTransform() {
        if (display == null) return;

        if (!display.isValid()) {
            display = null;
            return;
        }

        if (hasSentTransform && worldMatrix.equals(lastSentMatrix, MATRIX_EPSILON)) {
            return;
        }

        display.setTransformationMatrix(worldMatrix);
        lastSentMatrix.set(worldMatrix);
        hasSentTransform = true;
    }
}
