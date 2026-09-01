package com.pumpkings.coconpc.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class SecureSkinLoader {
    static final int MAX_DOWNLOAD_BYTES = 2 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private SecureSkinLoader() {
    }

    public static BufferedImage load(Path skinFolder, String source) throws IOException {
        Optional<Path> local = resolveLocalSkin(skinFolder, source);
        BufferedImage image = local.isPresent()
                ? ImageIO.read(local.get().toFile())
                : loadRemote(URI.create(source));
        if (image == null || image.getWidth() != 64 || (image.getHeight() != 32 && image.getHeight() != 64)) {
            throw new IOException("Skin images must be 64x32 or 64x64 PNG files");
        }
        return image;
    }

    static Optional<Path> resolveLocalSkin(Path skinFolder, String source) {
        if (source == null || source.isBlank() || source.contains("/") || source.contains("\\")) {
            return Optional.empty();
        }
        String fileName = source.toLowerCase(Locale.ROOT).endsWith(".png") ? source : source + ".png";
        Path root = skinFolder.toAbsolutePath().normalize();
        Path candidate = root.resolve(fileName).normalize();
        return candidate.getParent().equals(root) && Files.isRegularFile(candidate)
                ? Optional.of(candidate)
                : Optional.empty();
    }

    private static BufferedImage loadRemote(URI initialUri) throws IOException {
        URI uri = initialUri;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            validateRemoteUri(uri);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "image/png");
            connection.setRequestProperty("User-Agent", "CocoNPC/1.0");

            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || redirects == MAX_REDIRECTS) {
                    throw new IOException("Invalid skin redirect");
                }
                uri = uri.resolve(location);
                continue;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                throw new IOException("Skin server returned HTTP " + status);
            }
            long declaredSize = connection.getContentLengthLong();
            if (declaredSize > MAX_DOWNLOAD_BYTES) {
                connection.disconnect();
                throw new IOException("Skin response is too large");
            }
            try (InputStream input = connection.getInputStream()) {
                byte[] bytes = readLimited(input);
                return ImageIO.read(new ByteArrayInputStream(bytes));
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many skin redirects");
    }

    static void validateRemoteUri(URI uri) throws IOException {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IOException("Only public HTTPS skin URLs are allowed");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (!isPublicAddress(address)) {
                throw new IOException("Private or local skin hosts are not allowed");
            }
        }
    }

    static boolean isPublicAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocalIpv6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        return !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()
                && !address.isMulticastAddress()
                && !uniqueLocalIpv6;
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_DOWNLOAD_BYTES) throw new IOException("Skin response is too large");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
