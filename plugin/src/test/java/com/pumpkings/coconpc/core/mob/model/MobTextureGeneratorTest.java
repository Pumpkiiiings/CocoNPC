package com.pumpkings.coconpc.core.mob.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class MobTextureGeneratorTest {
    @Test
    void mapsEveryCubeFaceToItsPlayerHeadRegion(@TempDir Path folder) throws Exception {
        BufferedImage atlas = new BufferedImage(48, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        Color[] colors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN
        };
        for (int index = 0; index < colors.length; index++) {
            graphics.setColor(colors[index]);
            graphics.fillRect(index * 8, 0, 8, 8);
        }
        graphics.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(atlas, "png", png);
        String source = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(png.toByteArray());

        String json = """
                {
                  "meta": {"format_version": "4.10", "model_format": "free"},
                  "name": "face_test",
                  "resolution": {"width": 48, "height": 8},
                  "textures": [{"name": "atlas", "uuid": "tex", "source": "%s"}],
                  "elements": [
                    {
                      "name": "cube",
                      "uuid": "cube",
                      "from": [0, 0, 0],
                      "to": [8, 8, 8],
                      "faces": {
                        "north": {"texture": 0, "uv": [0, 0, 8, 8]},
                        "east":  {"texture": 0, "uv": [8, 0, 16, 8]},
                        "south": {"texture": 0, "uv": [16, 0, 24, 8]},
                        "west":  {"texture": 0, "uv": [24, 0, 32, 8]},
                        "up":    {"texture": 0, "uv": [32, 0, 40, 8]},
                        "down":  {"texture": 0, "uv": [40, 0, 48, 8]}
                      }
                    }
                  ]
                }
                """.formatted(source);

        Path modelFile = folder.resolve("face_test.bbmodel");
        Files.writeString(modelFile, json, StandardCharsets.UTF_8);

        BbModel model = BbModelParser.parse(modelFile, Logger.getLogger("test"));
        assertThat(model).isNotNull();

        Map<String, BufferedImage> atlases = MobTextureGenerator.loadAtlases(
                model, Logger.getLogger("test"));
        assertThat(atlases).containsKey("atlas");

        BbModel.Cube cube = model.getBones().get(0).getCubes().get(0);
        BufferedImage skin = MobTextureGenerator.composeSkin(model, cube, atlases);

        assertThat(skin.getWidth()).isEqualTo(MobTextureGenerator.SKIN_SIZE);
        assertThat(skin.getHeight()).isEqualTo(MobTextureGenerator.SKIN_SIZE);

        assertFace(skin, FaceDirection.NORTH, Color.RED);
        assertFace(skin, FaceDirection.EAST, Color.GREEN);
        assertFace(skin, FaceDirection.SOUTH, Color.BLUE);
        assertFace(skin, FaceDirection.WEST, Color.YELLOW);
        assertFace(skin, FaceDirection.UP, Color.MAGENTA);
        assertFace(skin, FaceDirection.DOWN, Color.CYAN);
    }

    @Test
    void averagesVisiblePixelsAndPicksRedConcrete() {
        BufferedImage atlas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 8, 8);
        graphics.dispose();

        String json = """
                {
                  "name": "avg",
                  "resolution": {"width": 8, "height": 8},
                  "textures": [{"name": "atlas", "uuid": "tex"}],
                  "elements": [
                    {
                      "name": "cube",
                      "from": [0, 0, 0],
                      "to": [8, 8, 8],
                      "faces": {"north": {"texture": 0, "uv": [0, 0, 8, 8]}}
                    }
                  ]
                }
                """;
        Path modelFile = null;
        try {
            modelFile = Files.createTempFile("avg", ".bbmodel");
            Files.writeString(modelFile, json, StandardCharsets.UTF_8);
            BbModel model = BbModelParser.parse(modelFile, Logger.getLogger("test"));
            BbModel.Cube cube = model.getBones().get(0).getCubes().get(0);

            int average = MobTextureGenerator.averageColor(model, cube,
                    Map.of("atlas", atlas));
            assertThat(average).isEqualTo(0xFF0000);
            assertThat(MobTextureGenerator.closestConcrete(average))
                    .isEqualTo(org.bukkit.Material.RED_CONCRETE);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        } finally {
            if (modelFile != null) {
                try {
                    Files.deleteIfExists(modelFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void scalesHdUvSpaceToTheAtlasSize(@TempDir Path folder) throws Exception {
        BufferedImage atlas = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 64, 128);
        graphics.setColor(Color.GREEN);
        graphics.fillRect(64, 0, 64, 128);
        graphics.dispose();

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(atlas, "png", png);
        String source = "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(png.toByteArray());

        String json = """
                {
                  "name": "hd",
                  "resolution": {"width": 128, "height": 128},
                  "textures": [{"name": "atlas", "uuid": "tex", "source": "%s",
                                "width": 128, "height": 128,
                                "uv_width": 64, "uv_height": 64}],
                  "elements": [
                    {
                      "name": "cube",
                      "uuid": "cube",
                      "from": [0, 0, 0],
                      "to": [8, 8, 8],
                      "faces": {
                        "north": {"texture": 0, "uv": [32, 0, 64, 32]},
                        "east":  {"texture": 0, "uv": [32, 0, 64, 32]},
                        "south": {"texture": 0, "uv": [32, 0, 64, 32]},
                        "west":  {"texture": 0, "uv": [32, 0, 64, 32]},
                        "up":    {"texture": 0, "uv": [32, 0, 64, 32]},
                        "down":  {"texture": 0, "uv": [32, 0, 64, 32]}
                      }
                    }
                  ]
                }
                """.formatted(source);

        Path modelFile = folder.resolve("hd.bbmodel");
        Files.writeString(modelFile, json, StandardCharsets.UTF_8);

        BbModel model = BbModelParser.parse(modelFile, Logger.getLogger("test"));
        assertThat(model).isNotNull();
        assertThat(model.getTexture("atlas").getUvWidth()).isEqualTo(64f);

        Map<String, BufferedImage> atlases = MobTextureGenerator.loadAtlases(
                model, Logger.getLogger("test"));
        BbModel.Cube cube = model.getBones().get(0).getCubes().get(0);
        BufferedImage skin = MobTextureGenerator.composeSkin(model, cube, atlases);

        // UV 32..64 in a 64-wide UV space maps to pixels 64..128 (green half).
        assertFace(skin, FaceDirection.NORTH, Color.GREEN);
    }

    @Test
    void headRegionsAccountForTheHeadItemFrame() {
        // The head item frame is rotated 180 degrees around Y relative to a
        // Blockbench model, so east/west and north/south are swapped.
        assertThat(FaceDirection.EAST.getSkinX()).isEqualTo(16);
        assertThat(FaceDirection.WEST.getSkinX()).isZero();
        assertThat(FaceDirection.NORTH.getSkinX()).isEqualTo(8);
        assertThat(FaceDirection.SOUTH.getSkinX()).isEqualTo(24);
        assertThat(FaceDirection.UP.getSkinY()).isZero();
        assertThat(FaceDirection.DOWN.getSkinY()).isZero();
    }

    private static void assertFace(BufferedImage skin, FaceDirection direction, Color expected) {
        int rgb = skin.getRGB(direction.getSkinX() + 4, direction.getSkinY() + 4) & 0xFFFFFF;
        assertThat(rgb)
                .as("face %s", direction)
                .isEqualTo(expected.getRGB() & 0xFFFFFF);
    }
}
