package com.pumpkings.coconpc.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.kyori.adventure.text.minimessage.MiniMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * messages.yml is populated from these defaults on first start, so a malformed default
 * ships broken text to every install.
 */
@DisplayName("Message defaults")
class MessageTest {

    @ParameterizedTest
    @EnumSource(Message.class)
    @DisplayName("every message has a non-blank default")
    void defaultsArePresent(Message message) {
        assertThat(message.getDefault()).isNotNull().isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Message.class)
    @DisplayName("every default parses as valid MiniMessage")
    void defaultsAreValidMiniMessage(Message message) {
        // Messages are sent through MiniMessage.deserialize, so an unbalanced tag would
        // throw at runtime on whichever code path happens to send it.
        assertThatCode(() -> MiniMessage.miniMessage().deserialize(message.getDefault()))
                .as("default for %s", message.name())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the prefix does not reference itself")
    void prefixIsNotRecursive() {
        // get() skips prefix substitution for PREFIX itself; a {prefix} inside it would
        // otherwise survive into the output verbatim.
        assertThat(Message.PREFIX.getDefault()).doesNotContain("{prefix}");
    }

    @Test
    @DisplayName("the Bedrock notice exists, since config can switch it on")
    void bedrockMessageExists() {
        assertThat(Message.BEDROCK_UNSUPPORTED.getDefault()).contains("{prefix}");
    }

    @ParameterizedTest
    @EnumSource(Message.class)
    @DisplayName("no default carries a stray placeholder brace left over from editing")
    void noUnbalancedPlaceholderBraces(Message message) {
        String text = message.getDefault();
        long open = text.chars().filter(c -> c == '{').count();
        long close = text.chars().filter(c -> c == '}').count();
        assertThat(open)
                .as("unbalanced { } in default for %s: %s", message.name(), text)
                .isEqualTo(close);
    }
}
