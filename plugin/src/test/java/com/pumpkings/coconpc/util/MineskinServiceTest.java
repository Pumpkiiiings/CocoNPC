package com.pumpkings.coconpc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.Map;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Covers the skin geometry, which is pure image maths and the part most likely to break
 * silently — a wrong offset produces a subtly mangled NPC rather than an exception.
 */
@DisplayName("MineskinService skin geometry")
class MineskinServiceTest {

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;
    private static final int TRANSPARENT = 0x00000000;

    /** A 64x64 skin with every pixel opaque, i.e. the classic four-pixel-arm model. */
    private static BufferedImage classicSkin() {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                skin.setRGB(x, y, OPAQUE_WHITE);
            }
        }
        return skin;
    }

    /**
     * A slim skin: identical to classic, except the fourth arm column is transparent —
     * which is exactly what {@code isSlim} samples for.
     */
    private static BufferedImage slimSkin() {
        BufferedImage skin = classicSkin();
        skin.setRGB(54, 20, TRANSPARENT);
        skin.setRGB(50, 16, TRANSPARENT);
        return skin;
    }

    @Nested
    @DisplayName("isSlim")
    class IsSlim {

        @Test
        @DisplayName("detects a slim skin by its transparent fourth arm column")
        void detectsSlim() {
            assertThat(MineskinService.isSlim(slimSkin())).isTrue();
        }

        @Test
        @DisplayName("does not flag a classic skin")
        void ignoresClassic() {
            assertThat(MineskinService.isSlim(classicSkin())).isFalse();
        }

        @Test
        @DisplayName("returns false rather than throwing on a null image")
        void handlesNull() {
            assertThat(MineskinService.isSlim(null)).isFalse();
        }

        @Test
        @DisplayName("returns false for an image too small to sample")
        void handlesUndersizedImage() {
            // A legacy 64x32 skin is fine to sample, but anything narrower is not.
            BufferedImage tiny = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            assertThat(MineskinService.isSlim(tiny)).isFalse();
        }

        @Test
        @DisplayName("accepts a legacy 64x32 skin without an out-of-bounds read")
        void handlesLegacyHeight() {
            BufferedImage legacy = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            assertThatCode(() -> MineskinService.isSlim(legacy)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("fixSlim")
    class FixSlim {

        /**
         * Paints the six faces of the right arm, base layer, with one distinct colour per
         * source column so the widening can be traced pixel by pixel.
         */
        private BufferedImage skinWithMarkedRightArm() {
            BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

            // front face of a slim arm: x 44..46, y 20..31
            for (int col = 0; col < 3; col++) {
                for (int y = 20; y < 32; y++) {
                    skin.setRGB(44 + col, y, 0xFF000010 + col);
                }
            }
            // inner face, already 4 wide: x 47..50
            for (int col = 0; col < 4; col++) {
                for (int y = 20; y < 32; y++) {
                    skin.setRGB(47 + col, y, 0xFF000020 + col);
                }
            }
            // back face: x 51..53
            for (int col = 0; col < 3; col++) {
                for (int y = 20; y < 32; y++) {
                    skin.setRGB(51 + col, y, 0xFF000030 + col);
                }
            }
            // top cap: x 44..46, y 16..19
            for (int col = 0; col < 3; col++) {
                for (int y = 16; y < 20; y++) {
                    skin.setRGB(44 + col, y, 0xFF000040 + col);
                }
            }
            return skin;
        }

        @Test
        @DisplayName("widens the front face and repeats its last column into the fourth")
        void widensFrontFace() {
            BufferedImage skin = skinWithMarkedRightArm();
            MineskinService.fixSlim(skin);

            // Classic front face sits at x 44..47.
            assertThat(skin.getRGB(44, 25)).isEqualTo(0xFF000010);
            assertThat(skin.getRGB(45, 25)).isEqualTo(0xFF000011);
            assertThat(skin.getRGB(46, 25)).isEqualTo(0xFF000012);
            // The fourth column repeats the third, so the arm has no transparent seam.
            assertThat(skin.getRGB(47, 25)).isEqualTo(0xFF000012);
        }

        @Test
        @DisplayName("shifts the already-4-wide inner face without distorting it")
        void movesInnerFace() {
            BufferedImage skin = skinWithMarkedRightArm();
            MineskinService.fixSlim(skin);

            // Classic inner face moves from x 47..50 to x 48..51, unchanged in content.
            assertThat(skin.getRGB(48, 25)).isEqualTo(0xFF000020);
            assertThat(skin.getRGB(49, 25)).isEqualTo(0xFF000021);
            assertThat(skin.getRGB(50, 25)).isEqualTo(0xFF000022);
            assertThat(skin.getRGB(51, 25)).isEqualTo(0xFF000023);
        }

        @Test
        @DisplayName("widens the back face into its classic position")
        void widensBackFace() {
            BufferedImage skin = skinWithMarkedRightArm();
            MineskinService.fixSlim(skin);

            // Classic back face sits at x 52..55.
            assertThat(skin.getRGB(52, 25)).isEqualTo(0xFF000030);
            assertThat(skin.getRGB(53, 25)).isEqualTo(0xFF000031);
            assertThat(skin.getRGB(54, 25)).isEqualTo(0xFF000032);
            assertThat(skin.getRGB(55, 25)).isEqualTo(0xFF000032);
        }

        @Test
        @DisplayName("widens the top cap as well as the side faces")
        void widensTopCap() {
            BufferedImage skin = skinWithMarkedRightArm();
            MineskinService.fixSlim(skin);

            assertThat(skin.getRGB(44, 17)).isEqualTo(0xFF000040);
            assertThat(skin.getRGB(45, 17)).isEqualTo(0xFF000041);
            assertThat(skin.getRGB(46, 17)).isEqualTo(0xFF000042);
            assertThat(skin.getRGB(47, 17)).isEqualTo(0xFF000042);
        }

        @Test
        @DisplayName("leaves the outer face where it already is")
        void leavesOuterFaceAlone() {
            BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            for (int col = 0; col < 4; col++) {
                skin.setRGB(40 + col, 25, 0xFF000050 + col);
            }
            MineskinService.fixSlim(skin);

            // The outer face is already 4 wide in a slim skin and must not move.
            assertThat(skin.getRGB(40, 25)).isEqualTo(0xFF000050);
            assertThat(skin.getRGB(41, 25)).isEqualTo(0xFF000051);
            assertThat(skin.getRGB(42, 25)).isEqualTo(0xFF000052);
            assertThat(skin.getRGB(43, 25)).isEqualTo(0xFF000053);
        }

        @Test
        @DisplayName("does nothing to a null or undersized image instead of throwing")
        void toleratesBadInput() {
            assertThatCode(() -> MineskinService.fixSlim(null)).doesNotThrowAnyException();
            assertThatCode(() -> MineskinService.fixSlim(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("only touches the arm regions, leaving head and legs untouched")
        void doesNotTouchOtherParts() {
            BufferedImage skin = classicSkin();
            skin.setRGB(8, 8, 0xFFABCDEF);    // head
            skin.setRGB(4, 20, 0xFF123456);   // right leg
            skin.setRGB(20, 20, 0xFF654321);  // body

            MineskinService.fixSlim(skin);

            assertThat(skin.getRGB(8, 8)).isEqualTo(0xFFABCDEF);
            assertThat(skin.getRGB(4, 20)).isEqualTo(0xFF123456);
            assertThat(skin.getRGB(20, 20)).isEqualTo(0xFF654321);
        }

        @Test
        @DisplayName("handles a 64x32 legacy skin without reading past its height")
        void handlesLegacyHeight() {
            BufferedImage legacy = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            assertThatCode(() -> MineskinService.fixSlim(legacy)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("per-limb cropping")
    class Cropping {

        @Test
        @DisplayName("cropAllParts returns all parts correctly")
        void cropAllPartsReturnsAll() {
            BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Map<String, BufferedImage> parts = MineskinService.cropAllParts(skin);

            assertThat(parts).containsOnlyKeys(
                "head", "torsoUpper", "torsoLower",
                "rightArmUpper", "rightArmLower", "leftArmUpper", "leftArmLower",
                "rightLegUpper", "rightLegLower", "leftLegUpper", "leftLegLower"
            );
            
            assertThat(parts.get("head")).isSameAs(skin);
            assertThat(parts.get("torsoUpper").getWidth()).isEqualTo(64);
            assertThat(parts.get("torsoUpper").getHeight()).isEqualTo(64);
            assertThat(parts.get("leftArmLower").getHeight()).isEqualTo(64);
        }

        @Test
        @DisplayName("resizeImage resizes to the requested dimensions")
        void resizeImageResizes() {
            BufferedImage source = new BufferedImage(4, 8, BufferedImage.TYPE_INT_ARGB);
            BufferedImage scaled = MineskinService.resizeImage(source, 16, 32);

            assertThat(scaled.getWidth()).isEqualTo(16);
            assertThat(scaled.getHeight()).isEqualTo(32);
        }
    }
}
