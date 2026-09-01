package com.pumpkings.coconpc.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SecureSkinLoaderTest {
    @TempDir
    Path skinFolder;

    @Test
    void resolvesOnlyDirectPngFilesInsideSkinFolder() throws Exception {
        Path skin = Files.write(skinFolder.resolve("steve.png"), new byte[]{1});
        assertThat(SecureSkinLoader.resolveLocalSkin(skinFolder, "steve")).contains(skin);
        assertThat(SecureSkinLoader.resolveLocalSkin(skinFolder, "../steve.png")).isEmpty();
    }

    @Test
    void rejectsPrivateAndLoopbackAddresses() throws Exception {
        assertThat(SecureSkinLoader.isPublicAddress(InetAddress.getByName("127.0.0.1"))).isFalse();
        assertThat(SecureSkinLoader.isPublicAddress(InetAddress.getByName("10.0.0.1"))).isFalse();
        assertThat(SecureSkinLoader.isPublicAddress(InetAddress.getByName("192.168.1.1"))).isFalse();
        assertThat(SecureSkinLoader.isPublicAddress(InetAddress.getByName("8.8.8.8"))).isTrue();
    }
}
