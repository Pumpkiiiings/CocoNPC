package com.pumpkings.coconpc.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NpcInputPolicyTest {
    @Test
    void acceptsBoundedIdentifiers() {
        assertThat(NpcInputPolicy.isValidNpcId("guard_01")).isTrue();
        assertThat(NpcInputPolicy.isValidNpcId("1234567890123456")).isTrue();
    }

    @Test
    void rejectsTraversalAndOversizedIdentifiers() {
        assertThat(NpcInputPolicy.isValidNpcId("../guard")).isFalse();
        assertThat(NpcInputPolicy.isValidNpcId("12345678901234567")).isFalse();
        assertThat(NpcInputPolicy.isValidNpcId("")).isFalse();
    }
}
