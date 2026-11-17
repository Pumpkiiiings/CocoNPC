package com.pumpkings.coconpc.core.mob.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.joml.Vector3f;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BbModelParser {
    private static final Vector3f ZERO = new Vector3f();

    private BbModelParser() {
    }

    public static BbModel parse(File file, Logger logger) {
        return file == null ? null : parse(file.toPath(), logger);
    }

    public static BbModel parse(Path path, Logger logger) {
        Logger log = logger != null ? logger : Logger.getLogger(BbModelParser.class.getName());
        if (path == null || !Files.isRegularFile(path)) {
            log.warning("[CocoNPC] Model file not found: " + path);
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                log.warning("[CocoNPC] Invalid .bbmodel root: " + path);
                return null;
            }

            JsonObject root = rootElement.getAsJsonObject();
            String name = optString(root, "name", path.getFileName().toString());

            JsonObject resolution = root.getAsJsonObject("resolution");
            float textureWidth = resolution != null ? optFloat(resolution, "width", 16f) : 16f;
            float textureHeight = resolution != null ? optFloat(resolution, "height", 16f) : 16f;
            if (textureWidth <= 0f) textureWidth = 16f;
            if (textureHeight <= 0f) textureHeight = 16f;

            Map<String, BbModel.Texture> textures = new LinkedHashMap<>();
            readTextures(root, textures);

            Map<String, JsonObject> elementsByUuid = new LinkedHashMap<>();
            List<JsonObject> elements = new ArrayList<>();
            readElements(root, elements, elementsByUuid);

            // Blockbench 5.0 moved bone names, pivots and rest rotations out of
            // the outliner and into a separate "groups" array. Index it first so
            // outliner nodes (which only carry uuid + children) can resolve them.
            Map<String, GroupData> groupsByUuid = readGroups(root);

            Node rootNode = new Node("root", null, new Vector3f(), new Vector3f());
            Map<String, Node> bonesByUuid = new HashMap<>();
            Set<String> consumedElements = new HashSet<>();
            Set<String> visitedBones = new HashSet<>();
            parseOutliner(root, rootNode, elementsByUuid, groupsByUuid, bonesByUuid,
                    consumedElements, visitedBones, textures, textureWidth, textureHeight);

            if (rootNode.cubes.isEmpty() && rootNode.children.isEmpty()) {
                for (JsonObject element : elements) {
                    BbModel.Cube cube = parseCube(element, textures, textureWidth, textureHeight);
                    if (cube != null) rootNode.cubes.add(cube);
                }
            }

            List<BbModel.Bone> roots = new ArrayList<>();
            List<BbModel.Bone> bones = new ArrayList<>();
            if (!rootNode.cubes.isEmpty()) {
                roots.add(toBone(rootNode, bones));
            }
            for (Node child : rootNode.children) {
                roots.add(toBone(child, bones));
            }

            Map<String, BbAnimation> animations = parseAnimations(root, bonesByUuid);
            BbModel model = new BbModel(name, textureWidth, textureHeight, textures,
                    path.toAbsolutePath().normalize(), roots, bones, animations);
            log.info("[CocoNPC] Loaded " + path.getFileName() + ": " + bones.size()
                    + " bones, " + animations.size() + " animations.");
            return model;
        } catch (Exception exception) {
            log.log(Level.WARNING, "[CocoNPC] Failed to parse .bbmodel: " + path, exception);
            return null;
        }
    }

    private static void readTextures(JsonObject root, Map<String, BbModel.Texture> textures) {
        JsonArray array = root.getAsJsonArray("textures");
        if (array == null) return;

        int index = 0;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject texture = element.getAsJsonObject();
            String name = optString(texture, "name", "texture_" + index);
            String uuid = optString(texture, "uuid", null);
            String source = optString(texture, "source", optString(texture, "path", null));
            float width = optFloat(texture, "width", 0f);
            float height = optFloat(texture, "height", 0f);
            // HD textures keep the UV space (for example 64) separate from the
            // image size (for example 128). Face UVs are expressed in UV space.
            float uvWidth = optFloat(texture, "uv_width", width);
            float uvHeight = optFloat(texture, "uv_height", height);

            // Keep the first occurrence for duplicated names so face references stay stable.
            textures.putIfAbsent(name, new BbModel.Texture(name, uuid, source,
                    width, height, uvWidth, uvHeight));
            index++;
        }
    }

    private static void readElements(JsonObject root, List<JsonObject> elements,
                                     Map<String, JsonObject> elementsByUuid) {
        JsonArray array = root.getAsJsonArray("elements");
        if (array == null) return;

        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            elements.add(object);
            String uuid = optString(object, "uuid", null);
            if (uuid != null) {
                elementsByUuid.put(uuid, object);
            }
        }
    }

    private static Map<String, GroupData> readGroups(JsonObject root) {
        Map<String, GroupData> groups = new HashMap<>();
        JsonArray array = root.getAsJsonArray("groups");
        if (array == null) return groups;

        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            String uuid = optString(object, "uuid", null);
            if (uuid == null || groups.containsKey(uuid)) continue;

            groups.put(uuid, new GroupData(
                    optString(object, "name", null),
                    readVectorOrNull(object, "origin"),
                    readVectorOrNull(object, "rotation")));
        }
        return groups;
    }

    private static void parseOutliner(JsonObject root, Node parent,
                                      Map<String, JsonObject> elementsByUuid,
                                      Map<String, GroupData> groupsByUuid,
                                      Map<String, Node> bonesByUuid,
                                      Set<String> consumedElements,
                                      Set<String> visitedBones,
                                      Map<String, BbModel.Texture> textures,
                                      float textureWidth, float textureHeight) {
        JsonElement outliner = root.get("outliner");
        if (outliner == null || outliner.isJsonNull()) return;

        // Older exports always use an array; accept a single root object too.
        if (outliner.isJsonArray()) {
            for (JsonElement element : outliner.getAsJsonArray()) {
                parseOutlinerElement(element, parent, elementsByUuid, groupsByUuid,
                        bonesByUuid, consumedElements, visitedBones,
                        textures, textureWidth, textureHeight);
            }
        } else {
            parseOutlinerElement(outliner, parent, elementsByUuid, groupsByUuid,
                    bonesByUuid, consumedElements, visitedBones,
                    textures, textureWidth, textureHeight);
        }
    }

    private static void parseOutlinerElement(JsonElement element, Node parent,
                                             Map<String, JsonObject> elementsByUuid,
                                             Map<String, GroupData> groupsByUuid,
                                             Map<String, Node> bonesByUuid,
                                             Set<String> consumedElements,
                                             Set<String> visitedBones,
                                             Map<String, BbModel.Texture> textures,
                                             float textureWidth, float textureHeight) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonPrimitive()) {
            addReference(element.getAsString(), parent, elementsByUuid, groupsByUuid,
                    bonesByUuid, consumedElements, visitedBones, textures,
                    textureWidth, textureHeight);
            return;
        }

        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();

        // Inline cube definitions (rare, but accepted by Blockbench).
        if (object.has("from") && object.has("to")) {
            String uuid = optString(object, "uuid", null);
            if (uuid != null && !consumedElements.add(uuid)) return;
            BbModel.Cube cube = parseCube(object, textures, textureWidth, textureHeight);
            if (cube != null) parent.cubes.add(cube);
            return;
        }

        String uuid = optString(object, "uuid", null);
        GroupData group = uuid != null ? groupsByUuid.get(uuid) : null;

        // A bare uuid with no group metadata and no children is an element
        // reference (Blockbench 4 outliner stores children as plain strings).
        if (!object.has("children") && !object.has("name") && group == null) {
            if (uuid != null) {
                addReference(uuid, parent, elementsByUuid, groupsByUuid, bonesByUuid,
                        consumedElements, visitedBones, textures,
                        textureWidth, textureHeight);
            }
            return;
        }

        String name = group != null && group.name != null
                ? group.name
                : optString(object, "name", "bone");
        Vector3f origin = group != null && group.origin != null
                ? group.origin
                : readVector(object, "origin", ZERO);
        Vector3f rotation = group != null && group.rotation != null
                ? group.rotation
                : readVector(object, "rotation", ZERO);

        // Guard against duplicated/cyclic group references.
        if (uuid != null && !visitedBones.add(uuid)) return;

        Node bone = new Node(name, uuid, origin, rotation);
        parent.children.add(bone);
        if (uuid != null) {
            bonesByUuid.put(uuid, bone);
        }

        JsonArray children = object.getAsJsonArray("children");
        if (children == null) return;

        for (JsonElement child : children) {
            parseOutlinerElement(child, bone, elementsByUuid, groupsByUuid, bonesByUuid,
                    consumedElements, visitedBones, textures, textureWidth, textureHeight);
        }
    }

    private static void addReference(String uuid, Node parent,
                                     Map<String, JsonObject> elementsByUuid,
                                     Map<String, GroupData> groupsByUuid,
                                     Map<String, Node> bonesByUuid,
                                     Set<String> consumedElements,
                                     Set<String> visitedBones,
                                     Map<String, BbModel.Texture> textures,
                                     float textureWidth, float textureHeight) {
        if (uuid == null) return;

        JsonObject element = elementsByUuid.get(uuid);
        if (element != null) {
            if (!consumedElements.add(uuid)) return;
            BbModel.Cube cube = parseCube(element, textures, textureWidth, textureHeight);
            if (cube != null) parent.cubes.add(cube);
            return;
        }

        // Some exporters reference groups as plain uuid strings.
        GroupData group = groupsByUuid.get(uuid);
        if (group == null || !visitedBones.add(uuid)) return;

        Node bone = new Node(group.name != null ? group.name : "bone", uuid,
                group.origin != null ? group.origin : ZERO,
                group.rotation != null ? group.rotation : ZERO);
        parent.children.add(bone);
        bonesByUuid.put(uuid, bone);
    }

    private static BbModel.Cube parseCube(JsonObject element,
                                          Map<String, BbModel.Texture> textures,
                                          float textureWidth, float textureHeight) {
        Vector3f from = readVector(element, "from", ZERO);
        Vector3f to = readVector(element, "to", ZERO);
        float inflate = optFloat(element, "inflate", 0f);
        from.sub(inflate, inflate, inflate);
        to.add(inflate, inflate, inflate);

        Vector3f center = new Vector3f(from).add(to).mul(0.5f);
        Vector3f pivot = readVector(element, "origin", center);
        Vector3f rotation = readVector(element, "rotation", ZERO);
        String name = optString(element, "name", "cube");
        String uuid = optString(element, "uuid", null);

        Map<FaceDirection, BbModel.Face> faces = readFaces(element, textures,
                textureWidth, textureHeight);

        return new BbModel.Cube(name, uuid, from, to, pivot, rotation, faces);
    }

    private static Map<FaceDirection, BbModel.Face> readFaces(JsonObject element,
                                                              Map<String, BbModel.Texture> textures,
                                                              float textureWidth,
                                                              float textureHeight) {
        Map<FaceDirection, BbModel.Face> faces = new EnumMap<>(FaceDirection.class);
        JsonObject faceObject = element.getAsJsonObject("faces");
        if (faceObject != null && !faceObject.isEmpty()) {
            // The box UV layout needs width, height and depth.
            Vector3f from = readVector(element, "from", ZERO);
            Vector3f to = readVector(element, "to", ZERO);
            float inflate = optFloat(element, "inflate", 0f);
            from.sub(inflate, inflate, inflate);
            to.add(inflate, inflate, inflate);
            float width = Math.abs(to.x - from.x);
            float height = Math.abs(to.y - from.y);
            float depth = Math.abs(to.z - from.z);

            for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
                FaceDirection direction = FaceDirection.fromKey(entry.getKey());
                if (direction == null) continue;
                if (entry.getValue() == null || !entry.getValue().isJsonObject()) continue;

                JsonObject faceJson = entry.getValue().getAsJsonObject();
                String textureName = resolveTextureName(faceJson.get("texture"), textures);
                if (textureName == null) continue;

                float[] uv = readUv(faceJson.get("uv"));
                if (uv == null) {
                    uv = direction.defaultUv(width, height, depth);
                }

                int rotation = (int) optFloat(faceJson, "rotation", 0f);
                faces.put(direction, new BbModel.Face(direction, textureName, uv, rotation));
            }
        }

        // Box UV cubes (box_uv: true) do not list faces. Derive all six from
        // the cube size and the texture-space offset.
        if (faces.isEmpty() && !textures.isEmpty()) {
            Vector3f from = readVector(element, "from", ZERO);
            Vector3f to = readVector(element, "to", ZERO);
            float inflate = optFloat(element, "inflate", 0f);
            from.sub(inflate, inflate, inflate);
            to.add(inflate, inflate, inflate);
            float width = Math.abs(to.x - from.x);
            float height = Math.abs(to.y - from.y);
            float depth = Math.abs(to.z - from.z);

            float[] offset = readPair(element.get("uv_offset"));
            float offsetU = offset != null ? offset[0] : 0f;
            float offsetV = offset != null ? offset[1] : 0f;
            String textureName = textures.keySet().iterator().next();

            for (FaceDirection direction : FaceDirection.ordered()) {
                float[] uv = direction.defaultUv(width, height, depth);
                uv[0] += offsetU;
                uv[1] += offsetV;
                uv[2] += offsetU;
                uv[3] += offsetV;
                faces.put(direction, new BbModel.Face(direction, textureName, uv, 0));
            }
        }
        return faces;
    }

    private static float[] readPair(JsonElement element) {
        if (element == null || !element.isJsonArray()) return null;
        JsonArray array = element.getAsJsonArray();
        if (array.size() < 2) return null;
        try {
            return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat()};
        } catch (Exception exception) {
            return null;
        }
    }

    private static float[] readUv(JsonElement element) {        if (element == null || !element.isJsonArray()) return null;
        JsonArray array = element.getAsJsonArray();
        if (array.size() < 2) return null;

        try {
            if (array.size() >= 4) {
                return new float[]{
                        array.get(0).getAsFloat(),
                        array.get(1).getAsFloat(),
                        array.get(2).getAsFloat(),
                        array.get(3).getAsFloat()
                };
            }
            // Two-value UVs are accepted as a size, anchored at the origin.
            float u = array.get(0).getAsFloat();
            float v = array.get(1).getAsFloat();
            return new float[]{0f, 0f, u, v};
        } catch (Exception exception) {
            return null;
        }
    }

    private static String resolveTextureName(JsonElement texture,
                                             Map<String, BbModel.Texture> textures) {
        if (texture == null || texture.isJsonNull()) return null;

        if (texture.isJsonPrimitive()) {
            JsonPrimitive primitive = texture.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                int index = primitive.getAsInt();
                int current = 0;
                for (String name : textures.keySet()) {
                    if (current == index) return name;
                    current++;
                }
                return null;
            }

            String value = primitive.getAsString();
            if (value == null || value.isBlank()) return null;
            if (textures.containsKey(value)) return value;

            for (BbModel.Texture candidate : textures.values()) {
                if (value.equals(candidate.getUuid())) {
                    return candidate.getName();
                }
            }
            return value;
        }

        return null;
    }

    private static BbModel.Bone toBone(Node node, List<BbModel.Bone> bones) {
        List<BbModel.Bone> children = new ArrayList<>();
        for (Node child : node.children) {
            children.add(toBone(child, bones));
        }

        BbModel.Bone bone = new BbModel.Bone(node.name, node.uuid, node.origin, node.rotation,
                node.cubes, children);
        bones.add(bone);
        return bone;
    }

    private static Map<String, BbAnimation> parseAnimations(JsonObject root,
                                                           Map<String, Node> bonesByUuid) {
        Map<String, BbAnimation> animations = new LinkedHashMap<>();
        JsonArray array = root.getAsJsonArray("animations");
        if (array == null) return animations;

        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject animation = element.getAsJsonObject();
            String name = optString(animation, "name", "animation");
            boolean loop = readLoop(animation.get("loop"));
            float length = optFloat(animation, "length", 0f);

            List<BbAnimation.BoneTrack> tracks = new ArrayList<>();
            JsonObject animators = animation.getAsJsonObject("animators");
            if (animators != null) {
                for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                    if (!entry.getValue().isJsonObject()) continue;
                    JsonObject animator = entry.getValue().getAsJsonObject();
                    String type = optString(animator, "type", "bone");
                    if (!type.isBlank() && !type.equalsIgnoreCase("bone")) continue;

                    String boneName = optString(animator, "name", null);
                    String boneUuid = entry.getKey();
                    if (boneName == null && boneUuid != null) {
                        Node node = bonesByUuid.get(boneUuid);
                        if (node != null) boneName = node.name;
                    }
                    if (boneName == null) continue;

                    TreeMap<Float, Vector3f> rotation = new TreeMap<>();
                    TreeMap<Float, Vector3f> position = new TreeMap<>();
                    TreeMap<Float, Vector3f> scale = new TreeMap<>();

                    JsonArray keyframes = animator.getAsJsonArray("keyframes");
                    if (keyframes != null) {
                        for (JsonElement keyframeElement : keyframes) {
                            if (!keyframeElement.isJsonObject()) continue;
                            JsonObject keyframe = keyframeElement.getAsJsonObject();
                            String channel = optString(keyframe, "channel", "");
                            float time = optFloat(keyframe, "time", 0f);
                            Vector3f value = readKeyframeValue(keyframe);
                            if (value == null) continue;

                            switch (channel) {
                                case "rotation" -> rotation.put(time, value);
                                case "position" -> position.put(time, value.mul(1f / 16f));
                                case "scale" -> scale.put(time, value);
                                default -> {
                                }
                            }
                        }
                    }

                    if (rotation.isEmpty() && position.isEmpty() && scale.isEmpty()) continue;

                    BbAnimation.BoneTrack track = new BbAnimation.BoneTrack(
                            boneName, boneUuid, rotation, position, scale);
                    tracks.add(track);
                    length = Math.max(length, track.getLastTime());
                }
            }

            animations.put(name, new BbAnimation(name, loop, length, tracks));
        }
        return animations;
    }

    private static Vector3f readKeyframeValue(JsonObject keyframe) {
        JsonArray dataPoints = keyframe.getAsJsonArray("data_points");
        if (dataPoints != null && !dataPoints.isEmpty()) {
            JsonElement first = dataPoints.get(0);
            if (first != null && first.isJsonObject()) {
                JsonObject point = first.getAsJsonObject();
                return new Vector3f(
                        optFloat(point, "x", 0f),
                        optFloat(point, "y", 0f),
                        optFloat(point, "z", 0f));
            }
        }

        Vector3f post = readArray(keyframe.get("post"));
        if (post != null) return post;

        return readArray(keyframe.get("pre"));
    }

    private static Vector3f readArray(JsonElement element) {
        if (element == null || !element.isJsonArray()) return null;
        JsonArray array = element.getAsJsonArray();
        if (array.size() < 3) return null;
        return new Vector3f(arrayFloat(array, 0), arrayFloat(array, 1), arrayFloat(array, 2));
    }

    private static boolean readLoop(JsonElement element) {
        if (element == null || element.isJsonNull()) return true;
        if (!element.isJsonPrimitive()) return true;

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();

        String value = primitive.getAsString();
        return !value.equalsIgnoreCase("once")
                && !value.equalsIgnoreCase("hold")
                && !value.equalsIgnoreCase("false");
    }

    private static Vector3f readVectorOrNull(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonArray()) return null;

        JsonArray array = element.getAsJsonArray();
        if (array.size() < 3) return null;

        return new Vector3f(arrayFloat(array, 0), arrayFloat(array, 1), arrayFloat(array, 2));
    }

    private static Vector3f readVector(JsonObject object, String key, Vector3f fallback) {
        Vector3f value = readVectorOrNull(object, key);
        return value != null ? value : new Vector3f(fallback);
    }

    private static float arrayFloat(JsonArray array, int index) {
        try {
            return array.get(index).getAsFloat();
        } catch (Exception exception) {
            return 0f;
        }
    }

    private static float optFloat(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsFloat();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private static String optString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsString();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private static final class Node {
        private final String name;
        private final String uuid;
        private final Vector3f origin;
        private final Vector3f rotation;
        private final List<BbModel.Cube> cubes = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();

        private Node(String name, String uuid, Vector3f origin, Vector3f rotation) {
            this.name = name;
            this.uuid = uuid;
            this.origin = new Vector3f(origin);
            this.rotation = new Vector3f(rotation);
        }
    }

    /** Bone metadata stored in the Blockbench 5.0 {@code groups} array. */
    private static final class GroupData {
        private final String name;
        private final Vector3f origin;
        private final Vector3f rotation;

        private GroupData(String name, Vector3f origin, Vector3f rotation) {
            this.name = name;
            this.origin = origin;
            this.rotation = rotation;
        }
    }
}
