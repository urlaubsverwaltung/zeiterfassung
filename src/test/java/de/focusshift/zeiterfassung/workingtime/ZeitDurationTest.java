package de.focusshift.zeiterfassung.workingtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ZeitDurationTest {

    @Test
    void ensureMinutesReturnsNewInstanceWithDurationZero() {
        final Duration actual = ZeitDuration.of(Duration.ZERO).durationInMinutes();
        assertThat(actual).isEqualTo(Duration.ZERO);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 59, 60})
    void ensureMinutesReturnsNewInstanceWithAtLeastOneMinuteDuration(int givenSeconds) {
        final Duration actual = ZeitDuration.of(Duration.ofSeconds(givenSeconds)).durationInMinutes();
        assertThat(actual).isEqualTo(Duration.ofMinutes(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {61, 120})
    void ensureMinutesReturnsNewInstanceRoundedUpToNextFullMinute(int givenSeconds) {
        final Duration actual = ZeitDuration.of(Duration.ofSeconds(givenSeconds)).durationInMinutes();
        assertThat(actual).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void ensureHoursDoubleValueReturnsZero() {
        assertThat(ZeitDuration.of(Duration.ZERO).hoursDoubleValue()).isEqualTo(0d);
    }

    @Test
    void ensureHoursDoubleValueReturnsOne() {
        assertThat(ZeitDuration.of(Duration.ofHours(1)).hoursDoubleValue()).isEqualTo(1d);
    }

    static Stream<Arguments> workDurationToHoursArguments() {
        return Stream.of(
            Arguments.of(7, 0.117),
            Arguments.of(10, 0.167),
            Arguments.of(15, 0.25),
            Arguments.of(30, 0.5),
            Arguments.of(90, 1.5),
            Arguments.of(110, 1.833)
        );
    }

    @ParameterizedTest
    @MethodSource("workDurationToHoursArguments")
    void ensureHoursDoubleValueReturnsRoundedToThreeDigits(int givenMinutes, double expectedHours) {
        assertThat(ZeitDuration.of(Duration.ofMinutes(givenMinutes)).hoursDoubleValue()).isEqualTo(expectedHours);
    }
}
