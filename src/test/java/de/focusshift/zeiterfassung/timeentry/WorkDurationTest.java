package de.focusshift.zeiterfassung.timeentry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WorkDurationTest {

    @Test
    void ensureMinutesReturnsNewWorkDurationWithDurationZero() {
        final WorkDuration actual = new WorkDuration(Duration.ZERO).minutes();
        assertThat(actual.duration()).isEqualTo(Duration.ZERO);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 59, 60})
    void ensureMinutesReturnsNewWorkDurationWithAtLeastOneMinuteDuration(int givenSeconds) {
        final WorkDuration actual = new WorkDuration(Duration.ofSeconds(givenSeconds)).minutes();
        assertThat(actual.duration()).isEqualTo(Duration.ofMinutes(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {61, 120})
    void ensureMinutesReturnsNewWorkDurationRoundedUpToNextFullMinute(int givenSeconds) {
        final WorkDuration actual = new WorkDuration(Duration.ofSeconds(givenSeconds)).minutes();
        assertThat(actual.duration()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void ensureHoursDoubleValueReturnsZero() {
        assertThat(new WorkDuration(Duration.ZERO).hoursDoubleValue()).isEqualTo(0d);
    }

    @Test
    void ensureHoursDoubleValueReturnsOne() {
        assertThat(new WorkDuration(Duration.ofHours(1)).hoursDoubleValue()).isEqualTo(1d);
    }

    static Stream<Arguments> workDurationToHoursArguments() {
        return Stream.of(
            Arguments.of(15, 0.25),
            Arguments.of(30, 0.5),
            Arguments.of(90, 1.5),
            Arguments.of(110, 1.84)
        );
    }

    @ParameterizedTest
    @MethodSource("workDurationToHoursArguments")
    void ensureHoursDoubleValueReturnsRoundedToTwoDigits(int givenMinutes, double expectedHours) {
        assertThat(new WorkDuration(Duration.ofMinutes(givenMinutes)).hoursDoubleValue()).isEqualTo(expectedHours);
    }
}
