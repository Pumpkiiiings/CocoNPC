package com.pumpkings.coconpc.core.mob.model;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BbModelParserTest {
    private static final String MODEL_JSON = """
            {
              "meta": {"format_version": "4.10", "model_format": "free"},
              "name": "test_mob",
              "resolution": {"width": 64, "height": 64},
              "textures": [
                {"name": "skin", "uuid": "tex-skin"},
                {"name": "eyes", "uuid": "tex-eyes"}
              ],
              "elements": [
                {
                  "name": "torso_cube",
                  "uuid": "elem-torso",
                  "from": [-4, 8, -2],
                  "to": [4, 20, 2],
                  "origin": [0, 8, 0],
                  "rotation": [0, 0, 0],
                  "faces": {"north": {"texture": 0, "uv": [8, 8, 16, 16], "rotation": 90}}
                },
                {
                  "name": "head_cube",
                  "uuid": "elem-head",
                  "from": [-4, 20, -4],
                  "to": [4, 28, 4],
                  "origin": [0, 24, 0],
                  "faces": {"north": {"texture": 1}}
                }
              ],
              "outliner": [
                {
                  "name": "body",
                  "uuid": "bone-body",
                  "origin": [0, 0, 0],
                  "rotation": [0, 0, 0],
                  "children": [
                    "elem-torso",
                    {
                      "name": "head",
                      "uuid": "bone-head",
                      "origin": [0, 8, 0],
                      "rotation": [10, 0, 0],
                      "children": ["elem-head"]
                    }
                  ]
                }
              ],
              "animations": [
                {
                  "name": "idle",
                  "loop": true,
                  "length": 2.0,
                  "animators": {
                    "bone-head": {
                      "name": "head",
                      "type": "bone",
                      "keyframes": [
                        {"channel": "rotation", "time": 0, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                        {"channel": "rotation", "time": 1, "data_points": [{"x": 0, "y": 90, "z": 0}]}
                      ]
                    }
                  }
                },
                {
                  "name": "walk",
                  "loop": true,
                  "length": 1.0,
                  "animators": {
                    "bone-body": {
                      "name": "body",
                      "type": "bone",
                      "keyframes": [
                        {"channel": "position", "time": 0, "data_points": [{"x": 0, "y": 0, "z": 0}]},
                        {"channel": "position", "time": 1, "data_points": [{"x": 0, "y": 16, "z": 0}]}
                      ]
                    }
                  }
                }
              ]
            }
            """;

    @Test
    void parsesHierarchyAndCubes(@TempDir Path tempDir) throws Exception {
        BbModel model = parse(tempDir);

        assertThat(model.getName()).isEqualTo("test_mob");
        assertThat(model.getTextureWidth()).isEqualTo(64f);
        assertThat(model.getTextureHeight()).isEqualTo(64f);
        assertThat(model.getTextures()).containsExactly("skin", "eyes");
        assertThat(model.getBones()).hasSize(2);
        assertThat(model.getRoots()).hasSize(1);

        BbModel.Bone body = model.getRoots().get(0);
        assertThat(body.getName()).isEqualTo("body");
        assertThat(body.getUuid()).isEqualTo("bone-body");
        assertThat(body.getChildren()).hasSize(1);

        BbModel.Bone head = body.getChildren().get(0);
        assertThat(head.getName()).isEqualTo("head");
        assertThat(head.getUuid()).isEqualTo("bone-head");
        assertVector(head.getOrigin(), 0f, 8f, 0f);
        assertVector(head.getRotation(), 10f, 0f, 0f);

        assertThat(body.getCubes()).hasSize(1);
        BbModel.Cube torso = body.getCubes().get(0);
        assertThat(torso.getName()).isEqualTo("torso_cube");
        assertThat(torso.getTexture()).isEqualTo("skin");
        assertVector(torso.getFrom(), -4f, 8f, -2f);
        assertVector(torso.getTo(), 4f, 20f, 2f);
        assertVector(torso.getSize(), 8f, 12f, 4f);
        assertVector(torso.getCenter(), 0f, 14f, 0f);
        assertThat(torso.getTexture()).isEqualTo("skin");
        assertThat(torso.getFaces()).hasSize(1);
        BbModel.Face north = torso.getFace(FaceDirection.NORTH);
        assertThat(north).isNotNull();
        assertThat(north.getTexture()).isEqualTo("skin");
        assertThat(north.getRotation()).isEqualTo(90);
        assertThat(north.getUv()).containsExactly(8f, 8f, 16f, 16f);

        assertThat(head.getCubes()).hasSize(1);
        assertThat(head.getCubes().get(0).getTexture()).isEqualTo("eyes");
        BbModel.Face headNorth = head.getCubes().get(0).getFace(FaceDirection.NORTH);
        assertThat(headNorth).isNotNull();
        // 8x8x8 cube with no explicit UV -> standard box UV front region.
        assertThat(headNorth.getUv()).containsExactly(8f, 8f, 16f, 16f);
    }

    @Test
    void parsesAnimationTracksAndSamplesChannels(@TempDir Path tempDir) throws Exception {
        BbModel model = parse(tempDir);

        BbAnimation idle = model.findAnimation("idle");
        assertThat(idle).isNotNull();
        assertThat(idle.isLoop()).isTrue();
        assertThat(idle.getLength()).isCloseTo(2f, within(0.0001f));
        assertThat(idle.getTracks()).hasSize(1);

        BbAnimation.BoneTrack rotationTrack = idle.getTracks().get(0);
        assertThat(rotationTrack.getBoneName()).isEqualTo("head");
        assertThat(rotationTrack.getBoneUuid()).isEqualTo("bone-head");
        assertThat(rotationTrack.hasRotation()).isTrue();
        assertThat(rotationTrack.hasPosition()).isFalse();
        assertThat(rotationTrack.hasScale()).isFalse();

        Quaternionf halfway = new Quaternionf();
        assertThat(rotationTrack.sampleRotation(0.5f, halfway)).isTrue();
        Quaternionf expectedHalfway = new Quaternionf().rotationY((float) Math.toRadians(45));
        assertThat(Math.abs(halfway.dot(expectedHalfway))).isCloseTo(1f, within(0.0001f));

        Vector3f untouched = new Vector3f(7f, 7f, 7f);
        assertThat(rotationTrack.samplePosition(0.5f, untouched)).isFalse();
        assertThat(rotationTrack.sampleScale(0.5f, untouched)).isFalse();
        assertVector(untouched, 7f, 7f, 7f);

        BbAnimation walk = model.findAnimation("walk");
        assertThat(walk).isNotNull();
        assertThat(walk.getLength()).isCloseTo(1f, within(0.0001f));
        assertThat(walk.getTracks()).hasSize(1);

        BbAnimation.BoneTrack positionTrack = walk.getTracks().get(0);
        assertThat(positionTrack.getBoneName()).isEqualTo("body");
        assertThat(positionTrack.hasPosition()).isTrue();
        assertThat(positionTrack.hasRotation()).isFalse();

        Vector3f position = new Vector3f();
        assertThat(positionTrack.samplePosition(0.5f, position)).isTrue();
        assertVector(position, 0f, 0.5f, 0f);
        assertThat(positionTrack.samplePosition(1f, position)).isTrue();
        assertVector(position, 0f, 1f, 0f);
    }

    @Test
    void derivesBoxUvFacesFromTextureOffset(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "name": "box_uv",
                  "resolution": {"width": 64, "height": 64},
                  "textures": [{"name": "skin", "uuid": "tex-skin"}],
                  "elements": [
                    {
                      "name": "cube",
                      "uuid": "elem-cube",
                      "box_uv": true,
                      "uv_offset": [4, 5],
                      "from": [0, 0, 0],
                      "to": [8, 12, 4],
                      "faces": {}
                    }
                  ],
                  "outliner": [
                    {"name": "body", "uuid": "bone-body", "origin": [0, 0, 0],
                     "children": ["elem-cube"]}
                  ]
                }
                """;

        Path file = tempDir.resolve("box_uv.bbmodel");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        Logger logger = Logger.getLogger("BbModelParserTest");
        logger.setLevel(Level.OFF);

        BbModel model = BbModelParser.parse(file, logger);
        assertThat(model).isNotNull();

        BbModel.Cube cube = model.getBones().get(0).getCubes().get(0);
        assertThat(cube.getFaces()).hasSize(6);
        assertThat(cube.getFace(FaceDirection.NORTH).getUv())
                .containsExactly(8f, 9f, 16f, 21f);
        assertThat(cube.getFace(FaceDirection.UP).getUv())
                .containsExactly(8f, 5f, 16f, 9f);
        assertThat(cube.getFace(FaceDirection.EAST).getUv())
                .containsExactly(4f, 9f, 8f, 21f);
    }

    @Test
    void resolvesBlockbenchFiveGroupsAndOutlinerReferences(@TempDir Path tempDir) throws Exception {
        String json = """
                {
                  "meta": {"format_version": "5.0", "model_format": "free"},
                  "name": "v5_mob",
                  "resolution": {"width": 64, "height": 64},
                  "textures": [{"name": "skin", "uuid": "tex", "uv_width": 32, "uv_height": 32}],
                  "elements": [
                    {
                      "name": "torso", "uuid": "elem-torso",
                      "from": [-4, 8, -2], "to": [4, 20, 2], "faces": {}
                    },
                    {
                      "name": "head", "uuid": "elem-head",
                      "from": [-4, 20, -4], "to": [4, 28, 4], "faces": {}
                    }
                  ],
                  "groups": [
                    {"name": "root", "uuid": "g-root", "origin": [0, 0, 0], "rotation": [0, 0, 0]},
                    {"name": "body", "uuid": "g-body", "origin": [0, 10, 0], "rotation": [10, 0, 0]},
                    {"name": "head", "uuid": "g-head", "origin": [0, 24, 0], "rotation": [0, 45, 0]}
                  ],
                  "outliner": [
                    {
                      "uuid": "g-root",
                      "children": [
                        {
                          "uuid": "g-body",
                          "children": [
                            "elem-torso",
                            {"uuid": "g-head", "children": ["elem-head"]}
                          ]
                        }
                      ]
                    }
                  ],
                  "animations": [
                    {
                      "name": "idle",
                      "loop": "loop",
                      "length": 1.0,
                      "animators": {
                        "g-head": {
                          "name": "head",
                          "type": "bone",
                          "keyframes": [
                            {"channel": "rotation", "time": 0,
                             "data_points": [{"x": 0, "y": 45, "z": 0}]},
                            {"channel": "position", "time": 1,
                             "data_points": [{"x": 0, "y": 16, "z": 0}]}
                          ]
                        }
                      }
                    }
                  ]
                }
                """;

        Path file = tempDir.resolve("v5_mob.bbmodel");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        Logger logger = Logger.getLogger("BbModelParserTest");
        logger.setLevel(Level.OFF);

        BbModel model = BbModelParser.parse(file, logger);
        assertThat(model).isNotNull();

        assertThat(model.getRoots()).hasSize(1);
        BbModel.Bone root = model.getRoots().get(0);
        assertThat(root.getName()).isEqualTo("root");
        assertThat(root.getUuid()).isEqualTo("g-root");
        assertVector(root.getOrigin(), 0f, 0f, 0f);

        assertThat(root.getChildren()).hasSize(1);
        BbModel.Bone body = root.getChildren().get(0);
        assertThat(body.getName()).isEqualTo("body");
        assertVector(body.getOrigin(), 0f, 10f, 0f);
        assertVector(body.getRotation(), 10f, 0f, 0f);
        assertThat(body.getCubes()).hasSize(1);
        assertThat(body.getCubes().get(0).getName()).isEqualTo("torso");

        BbModel.Bone head = body.getChildren().get(0);
        assertThat(head.getName()).isEqualTo("head");
        assertVector(head.getOrigin(), 0f, 24f, 0f);
        assertVector(head.getRotation(), 0f, 45f, 0f);
        assertThat(head.getCubes()).hasSize(1);
        assertThat(head.getCubes().get(0).getName()).isEqualTo("head");

        BbModel.Texture texture = model.getTexture("skin");
        assertThat(texture).isNotNull();
        assertThat(texture.getUvWidth()).isEqualTo(32f);
        assertThat(texture.getUvHeight()).isEqualTo(32f);

        BbAnimation animation = model.findAnimation("idle");
        assertThat(animation).isNotNull();
        assertThat(animation.getTracks()).hasSize(1);
        BbAnimation.BoneTrack track = animation.getTracks().get(0);
        assertThat(track.getBoneUuid()).isEqualTo("g-head");
        assertThat(track.getBoneName()).isEqualTo("head");

        Vector3f position = new Vector3f();
        assertThat(track.samplePosition(1f, position)).isTrue();
        assertVector(position, 0f, 1f, 0f);
    }

    @Test
    void resolvesAnimationNamesCaseInsensitively(@TempDir Path tempDir) throws Exception {
        BbModel model = parse(tempDir);

        assertThat(model.findAnimation("IDLE")).isSameAs(model.findAnimation("idle"));
        assertThat(model.findAnimation("animation.model.idle"))
                .isSameAs(model.findAnimation("idle"));
        assertThat(model.findAnimation("missing")).isNull();
        assertThat(model.findAnimation(null)).isNull();
    }

    private static BbModel parse(Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.bbmodel");
        Files.writeString(file, MODEL_JSON, StandardCharsets.UTF_8);

        Logger logger = Logger.getLogger("BbModelParserTest");
        logger.setLevel(Level.OFF);
        BbModel model = BbModelParser.parse(file, logger);
        assertThat(model).isNotNull();
        return model;
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertThat(actual.x).isCloseTo(x, within(0.0001f));
        assertThat(actual.y).isCloseTo(y, within(0.0001f));
        assertThat(actual.z).isCloseTo(z, within(0.0001f));
    }
}
