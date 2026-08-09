package com.pumpkings.coconpc.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CooldownManager.parseDuration")
class CooldownManagerTest {

    @ParameterizedTest(name = "\"{0}\" is {1}ms")
    @CsvSource({
            "5s,     5000",
            "30s,    30000",
            "10m,    600000",
            "1h,     3600000",
            "24h,    86400000",
            "7d,     604800000",
            "1d,     86400000"
    })
    @DisplayName("reads each supported unit suffix")
    void parsesUnitSuffixes(String input, long expectedMillis) {
        assertThat(CooldownManager.parseDuration(input)).isEqualTo(expectedMillis);
    }

    @Test
    @DisplayName("treats a bare number as seconds")
    void bareNumberIsSeconds() {
        assertThat(CooldownManager.parseDuration("45")).isEqualTo(45_000L);
    }

    @Test
    @DisplayName("is case insensitive and tolerates surrounding whitespace")
    void normalisesInput() {
        assertThat(CooldownManager.parseDuration("  10M  ")).isEqualTo(600_000L);
        assertThat(CooldownManager.parseDuration("2H")).isEqualTo(7_200_000L);
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"", "abc", "5x", "s", "-", "1.5m", "10 m"})
    @DisplayName("returns zero for anything it cannot parse, so the cooldown is skipped")
    void rejectsGarbage(String input) {
        assertThat(CooldownManager.parseDuration(input)).isZero();
    }

    @Test
    @DisplayName("a zero or negative duration means no cooldown at all")
    void zeroMeansNoCooldown() {
        // tryCooldown() returns early when this is <= 0, so garbage input must never
        // accidentally lock a player out.
        assertThat(CooldownManager.parseDuration("0s")).isZero();
        assertThat(CooldownManager.parseDuration("-5s")).isNegative();
    }
}
