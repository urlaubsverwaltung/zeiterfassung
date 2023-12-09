package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingTimeCalendarTest {

    @Test
    void ensurePlannedWorkingHoursBetweenDatesIsZeroForEmptyCalendar() {
        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of());
        final PlannedWorkingHours actual = sut.plannedWorkingHours(LocalDate.now(), LocalDate.now().plusDays(1));
        assertThat(actual).isEqualTo(PlannedWorkingHours.ZERO);
    }

    static Stream<Arguments> datesOutOfRange() {
        final LocalDate now = LocalDate.now();
        return Stream.of(
            Arguments.of(now, now.minusDays(7), now.minusDays(6)),
            Arguments.of(now, now.plusDays(1), now.plusDays(2))
        );
    }

    @ParameterizedTest
    @MethodSource("datesOutOfRange")
    void ensurePlannedWorkingHoursBetweenDatesIsZeroWhenGivenDatesAreOutOfRange(LocalDate pivot, LocalDate from, LocalDate toExclusive) {
        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            pivot, PlannedWorkingHours.EIGHT
        ));
        final PlannedWorkingHours actual = sut.plannedWorkingHours(from, toExclusive);
        assertThat(actual).isEqualTo(PlannedWorkingHours.ZERO);
    }

    @Test
    void ensurePlannedWorkingHoursBetweenDates() {

        final LocalDate now = LocalDate.now();

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            now, PlannedWorkingHours.EIGHT,
            now.plusDays(1), PlannedWorkingHours.EIGHT,
            now.plusDays(2), PlannedWorkingHours.EIGHT,
            now.plusDays(3), PlannedWorkingHours.EIGHT
        ));

        final PlannedWorkingHours actual = sut.plannedWorkingHours(now, now.plusDays(2));
        assertThat(actual).isEqualTo(new PlannedWorkingHours(Duration.ofHours(16)));
    }
}
