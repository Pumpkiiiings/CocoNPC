package com.pumpkings.coconpc.util;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.core.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the command-sanitising path. These are the guards that stop a crafted player
 * name from turning an admin-written {@code [console]} action into a different command,
 * so a regression here is a privilege escalation rather than a cosmetic bug.
 */
@DisplayName("Utils")
class UtilsTest {

    private Utils utils;

    @BeforeEach
    void setUp() {
        CocoNPC plugin = mock(CocoNPC.class);
        ConfigManager configManager = mock(ConfigManager.class);

        // color() asks the server whether PlaceholderAPI is enabled before expanding,
        // so the whole chain has to be stubbed even though we answer "no".
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        lenient().when(plugin.getServer()).thenReturn(server);
        lenient().when(server.getPluginManager()).thenReturn(pluginManager);
        lenient().when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(false);

        lenient().when(plugin.getConfigManager()).thenReturn(configManager);
        lenient().when(plugin.getLogger()).thenReturn(Logger.getLogger("CocoNPCTest"));
        lenient().when(configManager.getConsoleBlockedCharacters())
                .thenReturn(List.of(";", "|", "&"));

        this.utils = new Utils(plugin);
    }

    @Nested
    @DisplayName("sanitizeCommandArgument")
    class SanitizeCommandArgument {

        @Test
        @DisplayName("removes spaces so a name can only ever be one argument")
        void collapsesSpaces() {
            // A Bedrock or nicknamed player called "Bob 64" would otherwise turn
            // "give <player> diamond" into "give Bob 64 diamond".
            assertThat(utils.sanitizeCommandArgument("Bob 64")).isEqualTo("Bob64");
        }

        @Test
        @DisplayName("removes tabs and newlines too")
        void removesOtherWhitespace() {
            assertThat(utils.sanitizeCommandArgument("Bob\tSmith\nJones")).isEqualTo("BobSmithJones");
        }

        @Test
        @DisplayName("leaves an ordinary name untouched")
        void keepsPlainNames() {
            assertThat(utils.sanitizeCommandArgument("Pumpkiiings")).isEqualTo("Pumpkiiings");
            assertThat(utils.sanitizeCommandArgument("Player_123")).isEqualTo("Player_123");
        }

        @Test
        @DisplayName("turns null into an empty string rather than throwing")
        void handlesNull() {
            assertThat(utils.sanitizeCommandArgument(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("expandForCommand")
    class ExpandForCommand {

        private Player player;

        @BeforeEach
        void stubPlayer() {
            player = mock(Player.class);
            lenient().when(player.getName()).thenReturn("Tester");
        }

        @Test
        @DisplayName("strips the configured blocked characters")
        void stripsBlockedCharacters() {
            String result = utils.expandForCommand("eco give Tester 100; op Tester", player);
            assertThat(result).doesNotContain(";");
            assertThat(result).isEqualTo("eco give Tester 100 op Tester");
        }

        @Test
        @DisplayName("strips pipes and ampersands")
        void stripsPipesAndAmpersands() {
            assertThat(utils.expandForCommand("say hi | op me & op you", player))
                    .doesNotContain("|")
                    .doesNotContain("&");
        }

        @Test
        @DisplayName("strips control characters even when not in the blocked list")
        void stripsControlCharacters() {
            // Newlines are never listed in config because they are always removed.
            String result = utils.expandForCommand("say hello\nop Tester\rmore", player);
            assertThat(result).doesNotContain("\n").doesNotContain("\r");
            assertThat(result).isEqualTo("say helloop Testermore");
        }

        @Test
        @DisplayName("keeps tabs, which are legitimate inside command arguments")
        void keepsTabs() {
            assertThat(utils.expandForCommand("say a\tb", player)).isEqualTo("say a\tb");
        }

        @Test
        @DisplayName("leaves a clean command completely alone")
        void passesCleanCommandThrough() {
            String command = "mv tp Tester vip_dungeon";
            assertThat(utils.expandForCommand(command, player)).isEqualTo(command);
        }
    }

    @Nested
    @DisplayName("base64 helpers")
    class Base64Helpers {

        @Test
        @DisplayName("round-trips a texture URL")
        void roundTrips() {
            String url = "https://textures.minecraft.net/texture/abc123";
            assertThat(SkinUtils.base64ToUrl(SkinUtils.urlToBase64(url))).isEqualTo(url);
        }

        @Test
        @DisplayName("produces standard base64, which is what skins.yml keys are")
        void producesStandardBase64() {
            assertThat(SkinUtils.urlToBase64("test"))
                    .isEqualTo(Base64.getEncoder().encodeToString("test".getBytes()));
        }
    }

    @Nested
    @DisplayName("extractSkinUrl")
    class ExtractSkinUrl {

        private String encode(String json) {
            return Base64.getEncoder().encodeToString(json.getBytes());
        }

        @Test
        @DisplayName("reads the skin url out of a Mojang texture property")
        void readsSkinUrl() {
            String payload = encode("{\"textures\":{\"SKIN\":{\"url\":\"http://example.com/a.png\"}}}");
            assertThat(SkinUtils.extractSkinUrl(payload)).isEqualTo("http://example.com/a.png");
        }

        @Test
        @DisplayName("is not affected by key order or extra fields")
        void toleratesDifferentShapes() {
            // The old implementation split on the literal "SKIN" and indexed a fixed
            // position, so reordering or padding the JSON broke it.
            String payload = encode(
                    "{\"timestamp\":123,\"profileId\":\"abc\",\"textures\":{\"CAPE\":{\"url\":\"http://c\"},"
                            + "\"SKIN\":{\"metadata\":{\"model\":\"slim\"},\"url\":\"http://example.com/b.png\"}}}");
            assertThat(SkinUtils.extractSkinUrl(payload)).isEqualTo("http://example.com/b.png");
        }

        @Test
        @DisplayName("returns null for malformed input instead of throwing")
        void returnsNullOnGarbage() {
            assertThat(SkinUtils.extractSkinUrl("not base64 at all!!")).isNull();
            assertThat(SkinUtils.extractSkinUrl(encode("{\"textures\":{}}"))).isNull();
            assertThat(SkinUtils.extractSkinUrl(encode("[]"))).isNull();
        }
    }

    @Nested
    @DisplayName("parseBackgroundColor")
    class ParseBackgroundColor {

        @Test
        @DisplayName("treats transparent and none as fully transparent")
        void transparentKeywords() {
            assertThat(utils.parseBackgroundColor("transparent").asARGB()).isEqualTo(0x00000000);
            assertThat(utils.parseBackgroundColor("none").asARGB()).isEqualTo(0x00000000);
            assertThat(utils.parseBackgroundColor(null).asARGB()).isEqualTo(0x00000000);
        }

        @Test
        @DisplayName("maps default to vanilla's translucent black")
        void defaultKeyword() {
            assertThat(utils.parseBackgroundColor("default").asARGB()).isEqualTo(0x40000000);
        }

        @Test
        @DisplayName("parses 8-digit hex as ARGB")
        void parsesArgbHex() {
            assertThat(utils.parseBackgroundColor("#80FF0000").asARGB()).isEqualTo(0x80FF0000);
        }

        @Test
        @DisplayName("parses 6-digit hex as opaque RGB")
        void parsesRgbHex() {
            assertThat(utils.parseBackgroundColor("#FF0000").asARGB()).isEqualTo(0xFFFF0000);
        }

        @ParameterizedTest(name = "\"{0}\" falls back to transparent")
        @ValueSource(strings = {"nonsense", "#GGGGGG", "#12345"})
        @DisplayName("falls back to transparent rather than throwing on bad input")
        void fallsBackOnGarbage(String input) {
            assertThat(utils.parseBackgroundColor(input).asARGB()).isEqualTo(0x00000000);
        }
    }
}
