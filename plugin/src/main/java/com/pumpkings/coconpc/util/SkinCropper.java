package com.pumpkings.coconpc.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class SkinCropper {

    public enum Limb {
        HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG
    }

    public static BufferedImage generateLimbSkin(BufferedImage original, Limb limb) {
        BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = skin.createGraphics();
        // Use nearest neighbor to preserve pixel art look as much as possible when scaling
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        switch (limb) {
            case HEAD:
                // Head is already correct, just copy the head and hat layers
                g.drawImage(original.getSubimage(0, 0, 64, 16), 0, 0, null); // Base head
                g.drawImage(original.getSubimage(32, 0, 32, 16), 32, 0, null); // Hat layer
                break;
            case TORSO:
                // Torso: 8x12x4 mapped to 8x8x8
                // Front: 20,20 8x12 -> 8,8 8x8
                g.drawImage(original.getSubimage(20, 20, 8, 12), 8, 8, 8, 8, null);
                // Right: 16,20 4x12 -> 0,8 8x8
                g.drawImage(original.getSubimage(16, 20, 4, 12), 0, 8, 8, 8, null);
                // Back: 32,20 8x12 -> 24,8 8x8
                g.drawImage(original.getSubimage(32, 20, 8, 12), 24, 8, 8, 8, null);
                // Left: 28,20 4x12 -> 16,8 8x8
                g.drawImage(original.getSubimage(28, 20, 4, 12), 16, 8, 8, 8, null);
                // Top: 20,16 8x4 -> 8,0 8x8
                g.drawImage(original.getSubimage(20, 16, 8, 4), 8, 0, 8, 8, null);
                // Bottom: 28,16 8x4 -> 16,0 8x8
                g.drawImage(original.getSubimage(28, 16, 8, 4), 16, 0, 8, 8, null);
                break;
            case RIGHT_ARM:
                // Right Arm: 4x12x4 mapped to 8x8x8
                g.drawImage(original.getSubimage(44, 20, 4, 12), 8, 8, 8, 8, null); // Front
                g.drawImage(original.getSubimage(40, 20, 4, 12), 0, 8, 8, 8, null); // Right
                g.drawImage(original.getSubimage(52, 20, 4, 12), 24, 8, 8, 8, null); // Back
                g.drawImage(original.getSubimage(48, 20, 4, 12), 16, 8, 8, 8, null); // Left
                g.drawImage(original.getSubimage(44, 16, 4, 4), 8, 0, 8, 8, null); // Top
                g.drawImage(original.getSubimage(48, 16, 4, 4), 16, 0, 8, 8, null); // Bottom
                break;
            case LEFT_ARM:
                g.drawImage(original.getSubimage(36, 52, 4, 12), 8, 8, 8, 8, null); // Front
                g.drawImage(original.getSubimage(32, 52, 4, 12), 0, 8, 8, 8, null); // Right
                g.drawImage(original.getSubimage(44, 52, 4, 12), 24, 8, 8, 8, null); // Back
                g.drawImage(original.getSubimage(40, 52, 4, 12), 16, 8, 8, 8, null); // Left
                g.drawImage(original.getSubimage(36, 48, 4, 4), 8, 0, 8, 8, null); // Top
                g.drawImage(original.getSubimage(40, 48, 4, 4), 16, 0, 8, 8, null); // Bottom
                break;
            case RIGHT_LEG:
                g.drawImage(original.getSubimage(4, 20, 4, 12), 8, 8, 8, 8, null); // Front
                g.drawImage(original.getSubimage(0, 20, 4, 12), 0, 8, 8, 8, null); // Right
                g.drawImage(original.getSubimage(12, 20, 4, 12), 24, 8, 8, 8, null); // Back
                g.drawImage(original.getSubimage(8, 20, 4, 12), 16, 8, 8, 8, null); // Left
                g.drawImage(original.getSubimage(4, 16, 4, 4), 8, 0, 8, 8, null); // Top
                g.drawImage(original.getSubimage(8, 16, 4, 4), 16, 0, 8, 8, null); // Bottom
                break;
            case LEFT_LEG:
                g.drawImage(original.getSubimage(20, 52, 4, 12), 8, 8, 8, 8, null); // Front
                g.drawImage(original.getSubimage(16, 52, 4, 12), 0, 8, 8, 8, null); // Right
                g.drawImage(original.getSubimage(28, 52, 4, 12), 24, 8, 8, 8, null); // Back
                g.drawImage(original.getSubimage(24, 52, 4, 12), 16, 8, 8, 8, null); // Left
                g.drawImage(original.getSubimage(20, 48, 4, 4), 8, 0, 8, 8, null); // Top
                g.drawImage(original.getSubimage(24, 48, 4, 4), 16, 0, 8, 8, null); // Bottom
                break;
        }

        g.dispose();
        return skin;
    }
}
