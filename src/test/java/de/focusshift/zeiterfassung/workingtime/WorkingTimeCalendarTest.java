package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.absence.AbsenceColor.PINK;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.HOLIDAY;
import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.OVERTIME;
import static de.focusshift.zeiterfassung.absence.DayLength.FULL;
import static de.focusshift.zeiterfassung.absence.DayLength.MORNING;
import static de.focusshift.zeiterfassung.absence.DayLength.NOON;
import static org.assertj.core.api.Assertions.assertThat;

class WorkingTimeCalendarTest {

    static Stream<Arguments> datesOutOfRange() {
        final LocalDate now = LocalDate.now();
        return Stream.of(
            Arguments.of(now, now.minusDays(7), now.minusDays(6)),
            Arguments.of(now, now.plusDays(1), now.plusDays(2))
        );
    }

    @Test
    void ensurePlannedWorkingHoursBetweenDatesIsZeroForEmptyCalendar() {
        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(), Map.of());
        final PlannedWorkingHours actual = sut.plannedWorkingHours(LocalDate.now(), LocalDate.now().plusDays(1));
        assertThat(actual).isEqualTo(PlannedWorkingHours.ZERO);
    }

    @ParameterizedTest
    @MethodSource("datesOutOfRange")
    void ensurePlannedWorkingHoursBetweenDatesIsZeroWhenGivenDatesAreOutOfRange(LocalDate pivot, LocalDate from, LocalDate toExclusive) {
        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            pivot, PlannedWorkingHours.EIGHT
        ), Map.of());
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
        ), Map.of());

        final PlannedWorkingHours actual = sut.plannedWorkingHours(now, now.plusDays(2));
        assertThat(actual).isEqualTo(new PlannedWorkingHours(Duration.ofHours(16)));
    }

    @Test
    void ensureShouldHoursForOvertimeReductionWithMultipleDays() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence applicationOvertimeReduction = new Absence(
            new UserId("user"),
            date,
            date.plus(Duration.ofDays(2)),
            FULL,
            locale -> "de",
            PINK,
            OVERTIME,
            Duration.ofHours(6L)
        );

        final LocalDate tomorrow = today.plusDays(1);
        final LocalDate dayAfterTomorrow = tomorrow.plusDays(1);
        final WorkingTimeCalendar sut = new WorkingTimeCalendar(
            Map.of(
                today, PlannedWorkingHours.EIGHT,
                tomorrow, PlannedWorkingHours.ZERO,
                dayAfterTomorrow, PlannedWorkingHours.EIGHT
            ),
            Map.of(
                today, List.of(applicationOvertimeReduction),
                tomorrow, List.of(applicationOvertimeReduction),
                dayAfterTomorrow, List.of(applicationOvertimeReduction)
            )
        );

        assertThat(sut.shouldWorkingHours(today)).hasValue(new ShouldWorkingHours(Duration.ofHours(5)));
        assertThat(sut.shouldWorkingHours(tomorrow)).hasValue(new ShouldWorkingHours(Duration.ZERO));
        assertThat(sut.shouldWorkingHours(dayAfterTomorrow)).hasValue(new ShouldWorkingHours(Duration.ofHours(5)));
    }

    @Test
    void ensureShouldHoursForOvertimeReduction() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence applicationOvertimeReduction = new Absence(
            new UserId("user"),
            date,
            date,
            FULL,
            locale -> "de",
            PINK,
            OVERTIME,
            Duration.ofHours(6L)
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            today, PlannedWorkingHours.EIGHT
        ), Map.of(today, List.of(applicationOvertimeReduction)));

        assertThat(sut.shouldWorkingHours(today)).hasValue(new ShouldWorkingHours(Duration.ofHours(2)));
    }

    @Test
    void ensureShouldHoursForOvertimeReductionAtMorningAndOvertimeReductionAtNoon() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence applicationOvertimeReductionMorning = new Absence(
            new UserId("user"),
            date,
            date,
            MORNING,
            locale -> "de",
            PINK,
            OVERTIME,
            Duration.ofHours(4L)
        );

        final Absence applicationOvertimeReductionNoon = new Absence(
            new UserId("user"),
            date,
            date,
            NOON,
            locale -> "de",
            PINK,
            OVERTIME,
            Duration.ofHours(4L)
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            today, PlannedWorkingHours.EIGHT
        ), Map.of(today, List.of(applicationOvertimeReductionMorning, applicationOvertimeReductionNoon)));

        assertThat(sut.shouldWorkingHours(today)).hasValue(ShouldWorkingHours.ZERO);
    }

    @Test
    void ensureShouldHoursForOvertimeReductionAtMorningAndApplicationForLeaveAtNoon() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence applicationOvertimeReduction = new Absence(
            new UserId("user"),
            date,
            date,
            MORNING,
            locale -> "de",
            PINK,
            OVERTIME,
            Duration.ofHours(4L)
        );

        final Absence applicationForLeave = new Absence(
            new UserId("user"),
            date,
            date,
            NOON,
            locale -> "de",
            PINK,
            HOLIDAY
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            today, PlannedWorkingHours.EIGHT
        ), Map.of(today, List.of(applicationOvertimeReduction, applicationForLeave)));

        assertThat(sut.shouldWorkingHours(today)).hasValue(ShouldWorkingHours.ZERO);
    }

    @Test
    void ensureShouldHoursZeroForTwoApplicationForLeave() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence morning = new Absence(
            new UserId("user"),
            date,
            date,
            MORNING,
            locale -> "de",
            PINK,
            HOLIDAY
        );

        final Absence noon = new Absence(
            new UserId("user"),
            date,
            date,
            NOON,
            locale -> "de",
            PINK,
            HOLIDAY
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            today, PlannedWorkingHours.EIGHT
        ), Map.of(today, List.of(morning, noon)));

        assertThat(sut.shouldWorkingHours(today)).hasValue(ShouldWorkingHours.ZERO);
    }

    @Test
    void ensureShouldHoursAreZeroForOvertimeReductionWithMoreThanPlannedWorkingHours() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence applicationOvertimeReduction = new Absence(
            new UserId("user"),
            date,
            date,
            MORNING,
            locale -> "de",
            PINK,
            OVERTIME,
            Duration.ofHours(10L)
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            today, PlannedWorkingHours.EIGHT
        ), Map.of(today, List.of(applicationOvertimeReduction)));

        assertThat(sut.shouldWorkingHours(today)).hasValue(ShouldWorkingHours.ZERO);
    }

    @Test
    void ensureShouldHoursAreZeroForFullDayApplicationForLeave() {
        final LocalDate today = LocalDate.now();
        final Instant date = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence applicationOvertimeReduction = new Absence(
            new UserId("user"),
            date,
            date,
            FULL,
            locale -> "de",
            PINK,
            HOLIDAY,
            Duration.ofHours(10L)
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(
            today, PlannedWorkingHours.EIGHT
        ), Map.of(today, List.of(applicationOvertimeReduction)));

        assertThat(sut.shouldWorkingHours(today)).hasValue(ShouldWorkingHours.ZERO);
    }
}
