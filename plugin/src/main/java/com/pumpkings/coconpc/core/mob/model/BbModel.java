package com.pumpkings.coconpc.core.mob.model;

import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BbModel {
    private final String name;
    private final float textureWidth;
    private final float textureHeight;
    private final Map<String, Texture> textures;
    private final List<String> textureNames;
    private final Path sourcePath;
    private final List<Bone> roots;
    private final List<Bone> bones;
    private final Map<String, Bone> bonesByName;
    private final Map<String, BbAnimation> animations;

    BbModel(String name, float textureWidth, float textureHeight, Map<String, Texture> textures,
            Path sourcePath, List<Bone> roots, List<Bone> bones,
            Map<String, BbAnimation> animations) {
        this.name = name;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textures = Collections.unmodifiableMap(new HashMap<>(textures));
        List<String> names = new ArrayList<>(textures.keySet());
        this.textureNames = Collections.unmodifiableList(names);
        this.sourcePath = sourcePath;
        this.roots = Collections.unmodifiableList(new ArrayList<>(roots));
        this.bones = Collections.unmodifiableList(new ArrayList<>(bones));
        this.animations = Collections.unmodifiableMap(new HashMap<>(animations));

        Map<String, Bone> byName = new HashMap<>();
        for (Bone bone : bones) {
            byName.put(bone.getName(), bone);
        }
        this.bonesByName = Collections.unmodifiableMap(byName);
    }

    public String getName() {
        return name;
    }

    public float getTextureWidth() {
        return textureWidth;
    }

    public float getTextureHeight() {
        return textureHeight;
    }

    /**
     * Legacy accessor: returns the texture names in declaration order.
     */
    public List<String> getTextures() {
        return textureNames;
    }

    public Map<String, Texture> getTextureMap() {
        return textures;
    }

    public Texture getTexture(String name) {
        return name == null ? null : textures.get(name);
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public Path getBaseDirectory() {
        return sourcePath != null && sourcePath.getParent() != null
                ? sourcePath.getParent()
                : null;
    }

    public List<Bone> getRoots() {
        return roots;
    }

    public List<Bone> getBones() {
        return bones;
    }

    public Bone getBone(String name) {
        return name == null ? null : bonesByName.get(name);
    }

    public Map<String, BbAnimation> getAnimations() {
        return animations;
    }

    public BbAnimation findAnimation(String name) {
        if (name == null || name.isBlank()) return null;

        BbAnimation exact = animations.get(name);
        if (exact != null) return exact;

        String normalized = normalize(name);
        for (Map.Entry<String, BbAnimation> entry : animations.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase()
                .replace("animation.model.", "")
                .replace("animation.", "");
    }

    public static final class Texture {
        private final String name;
        private final String uuid;
        private final String source;
        private final float width;
        private final float height;
        private final float uvWidth;
        private final float uvHeight;

        Texture(String name, String uuid, String source, float width, float height,
                float uvWidth, float uvHeight) {
            this.name = name;
            this.uuid = uuid;
            this.source = source;
            this.width = width;
            this.height = height;
            this.uvWidth = uvWidth;
            this.uvHeight = uvHeight;
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getSource() {
            return source;
        }

        public float getWidth() {
            return width;
        }

        public float getHeight() {
            return height;
        }

        /**
         * Width of the UV space used by face coordinates. HD textures can use a
         * UV space smaller than the image itself (for example 64 on a 128px PNG).
         */
        public float getUvWidth() {
            return uvWidth > 0f ? uvWidth : width;
        }

        public float getUvHeight() {
            return uvHeight > 0f ? uvHeight : height;
        }

        public boolean hasSource() {
            return source != null && !source.isBlank();
        }

        public boolean isEmbedded() {
            return source != null && source.startsWith("data:");
        }
    }

    public static final class Bone {
        private final String name;
        private final String uuid;
        private final Vector3f origin;
        private final Vector3f rotation;
        private final List<Cube> cubes;
        private final List<Bone> children;

        Bone(String name, String uuid, Vector3f origin, Vector3f rotation,
             List<Cube> cubes, List<Bone> children) {
            this.name = name;
            this.uuid = uuid;
            this.origin = new Vector3f(origin);
            this.rotation = new Vector3f(rotation);
            this.cubes = Collections.unmodifiableList(new ArrayList<>(cubes));
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public Vector3f getOrigin() {
            return new Vector3f(origin);
        }

        public Vector3f getRotation() {
            return new Vector3f(rotation);
        }

        public List<Cube> getCubes() {
            return cubes;
        }

        public List<Bone> getChildren() {
            return children;
        }
    }

    public static final class Cube {
        private final String name;
        private final String uuid;
        private final Vector3f from;
        private final Vector3f to;
        private final Vector3f pivot;
        private final Vector3f rotation;
        private final Map<FaceDirection, Face> faces;

        Cube(String name, String uuid, Vector3f from, Vector3f to, Vector3f pivot,
             Vector3f rotation, Map<FaceDirection, Face> faces) {
            this.name = name;
            this.uuid = uuid;
            this.from = new Vector3f(from);
            this.to = new Vector3f(to);
            this.pivot = new Vector3f(pivot);
            this.rotation = new Vector3f(rotation);
            this.faces = Collections.unmodifiableMap(new EnumMap<>(faces));
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public Vector3f getFrom() {
            return new Vector3f(from);
        }

        public Vector3f getTo() {
            return new Vector3f(to);
        }

        public Vector3f getCenter() {
            return new Vector3f(from).add(to).mul(0.5f);
        }

        public Vector3f getSize() {
            return new Vector3f(
                    Math.abs(to.x - from.x),
                    Math.abs(to.y - from.y),
                    Math.abs(to.z - from.z));
        }

        public Vector3f getPivot() {
            return new Vector3f(pivot);
        }

        public Vector3f getRotation() {
            return new Vector3f(rotation);
        }

        public Map<FaceDirection, Face> getFaces() {
            return faces;
        }

        public Face getFace(FaceDirection direction) {
            return direction == null ? null : faces.get(direction);
        }

        /**
         * Legacy accessor: returns the first texture name referenced by any face.
         */
        public String getTexture() {
            for (FaceDirection direction : FaceDirection.values()) {
                Face face = faces.get(direction);
                if (face != null && face.getTexture() != null) {
                    return face.getTexture();
                }
            }
            return null;
        }

        public boolean hasFaces() {
            return !faces.isEmpty();
        }
    }

    public static final class Face {
        private final FaceDirection direction;
        private final String texture;
        private final float[] uv;
        private final int rotation;

        Face(FaceDirection direction, String texture, float[] uv, int rotation) {
            this.direction = direction;
            this.texture = texture;
            this.uv = uv != null ? uv.clone() : null;
            this.rotation = normalizeRotation(rotation);
        }

        private static int normalizeRotation(int rotation) {
            int value = rotation % 360;
            if (value < 0) value += 360;
            return switch (value) {
                case 90, 180, 270 -> value;
                default -> 0;
            };
        }

        public FaceDirection getDirection() {
            return direction;
        }

        public String getTexture() {
            return texture;
        }

        /**
         * Texture rectangle in pixels: {u1, v1, u2, v2}. May be null when the
         * model relies on the automatic Minecraft box UV layout.
         */
        public float[] getUv() {
            return uv != null ? uv.clone() : null;
        }

        public boolean hasUv() {
            return uv != null;
        }

        public int getRotation() {
            return rotation;
        }
    }
}
