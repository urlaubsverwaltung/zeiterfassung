package de.focusshift.zeiterfassung.overtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OvertimeHoursTest {

    @Test
    void ensureIsNegativeReturnsTrueWhenDurationIsNegative() {
        final OvertimeHours sut = new OvertimeHours(Duration.ofHours(-1));
        assertThat(sut.isNegative()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void ensureIsNegativeReturnsTrue(int hours) {
        final OvertimeHours sut = new OvertimeHours(Duration.ofHours(hours));
        assertThat(sut.isNegative()).isFalse();
    }

    @Test
    void ensurePlus() {
        final OvertimeHours first = new OvertimeHours(Duration.ofHours(1));
        final OvertimeHours second = new OvertimeHours(Duration.ofMinutes(30));

        assertThat(first.plus(second)).isEqualTo(new OvertimeHours(Duration.ofMinutes(90)));

        // original not mutated
        assertThat(first.duration()).isEqualTo(Duration.ofHours(1));
        assertThat(second.duration()).isEqualTo(Duration.ofMinutes(30));
    }
}
