package com.pumpkings.coconpc.core.mob.model;

import com.pumpkings.coconpc.core.mob.model.BbModel.Face;
import com.pumpkings.coconpc.core.mob.model.BbModel.Texture;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Turns .bbmodel texture atlases into one standard 64x64 player skin per cube.
 * The player-head model samples six 8x8 regions, so each cube face is cropped,
 * resized to 8x8 and written into the matching region.
 */
public final class MobTextureGenerator {
    public static final int SKIN_SIZE = 64;
    public static final int FACE_SIZE = 8;

    private static final int MAX_TEXTURE_BYTES = 16 * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static final Material[] CONCRETE = {
            Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
            Material.PINK_CONCRETE, Material.GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE,
            Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE, Material.BLUE_CONCRETE,
            Material.BROWN_CONCRETE, Material.GREEN_CONCRETE, Material.RED_CONCRETE,
            Material.BLACK_CONCRETE
    };

    private static final int[] CONCRETE_RGB = {
            0xCFD5D6, 0xE06101, 0xA9309F, 0x2489C7,
            0xF1AF15, 0x5EA918, 0xD6658F, 0x373A3E,
            0x7D7D73, 0x157788, 0x6420A0, 0x2C2E8F,
            0x603C20, 0x495B24, 0x8E2121, 0x080A0F
    };

    private MobTextureGenerator() {
    }

    public static Map<String, BufferedImage> loadAtlases(BbModel model, Logger logger) {
        Map<String, BufferedImage> atlases = new LinkedHashMap<>();
        if (model == null) return atlases;

        for (Texture texture : model.getTextureMap().values()) {
            BufferedImage image = loadAtlas(model, texture, logger);
            if (image != null) {
                atlases.put(texture.getName(), image);
            }
        }
        return atlases;
    }

    public static BufferedImage loadAtlas(BbModel model, Texture texture, Logger logger) {
        if (texture == null || !texture.hasSource()) return null;

        String source = texture.getSource().trim();
        try {
            if (source.startsWith("data:")) {
                return decodeDataUri(source);
            }
            if (source.startsWith("http://") || source.startsWith("https://")) {
                return readRemote(URI.create(source));
            }

            Path base = model.getBaseDirectory();
            if (base == null) return null;

            String relative = source.replace('\\', '/');
            while (relative.startsWith("./")) {
                relative = relative.substring(2);
            }
            Path path = base.resolve(relative).normalize().toAbsolutePath();
            Path root = base.toAbsolutePath().normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                if (logger != null) {
                    logger.warning("[CocoNPC] Texture not found or outside the model folder: "
                            + source);
                }
                return null;
            }
            return ImageIO.read(path.toFile());
        } catch (Exception exception) {
            if (logger != null) {
                logger.warning("[CocoNPC] Could not read texture '" + texture.getName()
                        + "': " + exception.getMessage());
            }
            return null;
        }
    }

    private static BufferedImage decodeDataUri(String dataUri) throws IOException {
        int comma = dataUri.indexOf(',');
        if (comma < 0) throw new IOException("Malformed data URI");

        String header = dataUri.substring(0, comma);
        String payload = dataUri.substring(comma + 1);
        byte[] bytes;
        if (header.toLowerCase(Locale.ROOT).contains(";base64")) {
            bytes = Base64.getMimeDecoder().decode(payload);
        } else {
            bytes = java.net.URLDecoder.decode(payload, java.nio.charset.StandardCharsets.UTF_8)
                    .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        if (bytes.length > MAX_TEXTURE_BYTES) {
            throw new IOException("Embedded texture is too large");
        }
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static BufferedImage readRemote(URI initialUri) throws IOException {
        URI uri = initialUri;
        for (int redirects = 0; redirects <= 3; redirects++) {
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IOException("Invalid texture URL");
            }
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "image/png,image/*");
            connection.setRequestProperty("User-Agent", "CocoNPC/1.0");

            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) throw new IOException("Invalid texture redirect");
                uri = uri.resolve(location);
                continue;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                throw new IOException("Texture server returned HTTP " + status);
            }

            long declared = connection.getContentLengthLong();
            if (declared > MAX_TEXTURE_BYTES) {
                connection.disconnect();
                throw new IOException("Remote texture is too large");
            }
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = readLimited(input);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many texture redirects");
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_TEXTURE_BYTES) throw new IOException("Texture is too large");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    public static BufferedImage composeSkin(BbModel model, BbModel.Cube cube,
                                            Map<String, BufferedImage> atlases) {
        BufferedImage skin = new BufferedImage(SKIN_SIZE, SKIN_SIZE, BufferedImage.TYPE_INT_ARGB);
        if (model == null || cube == null || atlases == null || atlases.isEmpty()) {
            return skin;
        }

        Graphics2D graphics = skin.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        try {
            for (FaceDirection direction : FaceDirection.values()) {
                Face face = cube.getFace(direction);
                if (face == null || face.getTexture() == null) continue;

                BufferedImage atlas = atlases.get(face.getTexture());
                if (atlas == null) continue;

                BufferedImage image = cropFace(model, cube, direction, face, atlas);
                if (image == null) continue;

                graphics.drawImage(image, direction.getSkinX(), direction.getSkinY(),
                        FACE_SIZE, FACE_SIZE, null);
            }
        } finally {
            graphics.dispose();
        }
        return skin;
    }

    private static BufferedImage cropFace(BbModel model, BbModel.Cube cube,
                                          FaceDirection direction, Face face,
                                          BufferedImage atlas) {
        float[] uv = face.getUv();
        if (uv == null) {
            org.joml.Vector3f size = cube.getSize();
            uv = direction.defaultUv(size.x, size.y, size.z);
        }
        if (uv == null || uv.length < 4) return null;

        float scaleX;
        float scaleY;
        Texture texture = model.getTexture(face.getTexture());
        if (texture != null && texture.getUvWidth() > 0f && texture.getUvHeight() > 0f) {
            scaleX = atlas.getWidth() / texture.getUvWidth();
            scaleY = atlas.getHeight() / texture.getUvHeight();
        } else {
            scaleX = model.getTextureWidth() > 0f
                    ? atlas.getWidth() / model.getTextureWidth() : 1f;
            scaleY = model.getTextureHeight() > 0f
                    ? atlas.getHeight() / model.getTextureHeight() : 1f;
        }

        float u1 = uv[0] * scaleX;
        float v1 = uv[1] * scaleY;
        float u2 = uv[2] * scaleX;
        float v2 = uv[3] * scaleY;

        boolean flipX = u1 > u2;
        boolean flipY = v1 > v2;
        if (flipX) {
            float swap = u1;
            u1 = u2;
            u2 = swap;
        }
        if (flipY) {
            float swap = v1;
            v1 = v2;
            v2 = swap;
        }

        int x = clamp((int) Math.floor(u1), 0, atlas.getWidth());
        int y = clamp((int) Math.floor(v1), 0, atlas.getHeight());
        int width = clamp((int) Math.ceil(u2) - x, 0, atlas.getWidth() - x);
        int height = clamp((int) Math.ceil(v2) - y, 0, atlas.getHeight() - y);
        if (width <= 0 || height <= 0) return null;

        BufferedImage cropped = copy(atlas.getSubimage(x, y, width, height));
        if (flipX) cropped = flipHorizontal(cropped);
        if (flipY) cropped = flipVertical(cropped);

        BufferedImage resized = resizeNearest(cropped, FACE_SIZE, FACE_SIZE);
        return rotate(resized, face.getRotation());
    }

    public static BufferedImage resizeNearest(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    private static BufferedImage rotate(BufferedImage source, int rotation) {
        int normalized = ((rotation % 360) + 360) % 360;
        if (normalized == 0) return source;

        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int targetX;
                int targetY;
                switch (normalized) {
                    case 90 -> {
                        targetX = height - 1 - y;
                        targetY = x;
                    }
                    case 180 -> {
                        targetX = width - 1 - x;
                        targetY = height - 1 - y;
                    }
                    case 270 -> {
                        targetX = y;
                        targetY = width - 1 - x;
                    }
                    default -> {
                        targetX = x;
                        targetY = y;
                    }
                }
                if (targetX >= 0 && targetX < width && targetY >= 0 && targetY < height) {
                    result.setRGB(targetX, targetY, source.getRGB(x, y));
                }
            }
        }
        return result;
    }

    private static BufferedImage flipHorizontal(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setRGB(width - 1 - x, y, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static BufferedImage flipVertical(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setRGB(x, height - 1 - y, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return result;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Averages the visible pixels of every textured face of the cube.
     * Returns 0xRRGGBB, or white when there is nothing to average.
     */
    public static int averageColor(BbModel model, BbModel.Cube cube,
                                   Map<String, BufferedImage> atlases) {
        if (model == null || cube == null || atlases == null) return 0xFFFFFF;

        long red = 0;
        long green = 0;
        long blue = 0;
        long weight = 0;

        for (FaceDirection direction : FaceDirection.values()) {
            Face face = cube.getFace(direction);
            if (face == null || face.getTexture() == null) continue;

            BufferedImage atlas = atlases.get(face.getTexture());
            if (atlas == null) continue;

            BufferedImage image = cropFace(model, cube, direction, face, atlas);
            if (image == null) continue;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha < 16) continue;

                    red += ((argb >> 16) & 0xFF) * alpha;
                    green += ((argb >> 8) & 0xFF) * alpha;
                    blue += (argb & 0xFF) * alpha;
                    weight += alpha;
                }
            }
        }

        if (weight == 0) return 0xFFFFFF;
        int r = (int) (red / weight);
        int g = (int) (green / weight);
        int b = (int) (blue / weight);
        return (r << 16) | (g << 8) | b;
    }

    public static Material closestConcrete(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int index = 0; index < CONCRETE_RGB.length; index++) {
            int color = CONCRETE_RGB[index];
            int cr = (color >> 16) & 0xFF;
            int cg = (color >> 8) & 0xFF;
            int cb = color & 0xFF;

            double dr = r - cr;
            double dg = g - cg;
            double db = b - cb;
            double distance = dr * dr * 2.0 + dg * dg * 4.0 + db * db * 3.0;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return CONCRETE[bestIndex];
    }

    public static String sha256(BufferedImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int width = image.getWidth();
            int height = image.getHeight();
            byte[] row = new byte[width * 4];
            for (int y = 0; y < height; y++) {
                int index = 0;
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    row[index++] = (byte) ((argb >> 16) & 0xFF);
                    row[index++] = (byte) ((argb >> 8) & 0xFF);
                    row[index++] = (byte) (argb & 0xFF);
                    row[index++] = (byte) ((argb >>> 24) & 0xFF);
                }
                digest.update(row);
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash cube skin", exception);
        }
    }
}
