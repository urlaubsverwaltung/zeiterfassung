package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Test
    void ensureShouldHoursForOvertimeReductionWhenAllPlannedWorkingHoursAreZero() {
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

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(
            Map.of(today, PlannedWorkingHours.ZERO),
            Map.of(today, List.of(applicationOvertimeReduction))
        );

        assertThat(sut.shouldWorkingHours(today)).hasValue(new ShouldWorkingHours(Duration.ZERO));
    }

    @Test
    void ensureOvertimeReductionExcludesDaysWithFullAbsence() {
        // Test case: 3-day overtime period with company vacation on one day
        // Overtime hours should only be distributed over working days without full absences
        final LocalDate wednesday = LocalDate.of(2025, 1, 14);
        final LocalDate tuesday = wednesday.minusDays(1);
        final LocalDate monday = tuesday.minusDays(1);

        final Instant mondayInstant = monday.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant wednesdayInstant = wednesday.atStartOfDay().toInstant(ZoneOffset.UTC);

        // Overtime reduction for 3 days (Mon-Wed) with 6 hours total
        final Absence overtimeReduction = new Absence(
            new UserId("user"),
            mondayInstant,
            wednesdayInstant,
            FULL,
            locale -> "Overtime reduction",
            PINK,
            OVERTIME,
            Duration.ofHours(6L)
        );

        // Company vacation on Wednesday (full day)
        final Absence companyVacation = new Absence(
            new UserId("user"),
            wednesdayInstant,
            wednesdayInstant,
            FULL,
            locale -> "Company vacation",
            PINK,
            HOLIDAY
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(
            Map.of(
                monday, PlannedWorkingHours.EIGHT,
                tuesday, PlannedWorkingHours.EIGHT,
                wednesday, PlannedWorkingHours.EIGHT
            ),
            Map.of(
                monday, List.of(overtimeReduction),
                tuesday, List.of(overtimeReduction),
                wednesday, List.of(overtimeReduction, companyVacation)
            )
        );

        // 8 hour planned workingHours - 3h overtime reduction per day = 5h
        assertThat(sut.shouldWorkingHours(monday)).hasValue(new ShouldWorkingHours(Duration.ofHours(5)));
        assertThat(sut.shouldWorkingHours(tuesday)).hasValue(new ShouldWorkingHours(Duration.ofHours(5)));
        assertThat(sut.shouldWorkingHours(wednesday)).hasValue(new ShouldWorkingHours(Duration.ZERO));
    }

    @Test
    void ensureOvertimeReductionConsidersHalfDayAbsences() {
        // Test case: 2-day overtime period with half-day absence on first day
        final LocalDate wednesday = LocalDate.of(2025, 1, 14);
        final LocalDate tuesday = wednesday.minusDays(1);

        final Instant tuesdayInstant = tuesday.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant wednesdayInstant = wednesday.atStartOfDay().toInstant(ZoneOffset.UTC);

        final Absence overtimeReduction = new Absence(
            new UserId("user"),
            tuesdayInstant,
            wednesdayInstant,
            FULL,
            locale -> "Overtime reduction",
            PINK,
            OVERTIME,
            Duration.ofHours(6L)
        );

        final Absence halfDayVacation = new Absence(
            new UserId("user"),
            wednesdayInstant,
            wednesdayInstant,
            MORNING,
            locale -> "Half day vacation",
            PINK,
            HOLIDAY
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(
            Map.of(
                tuesday, PlannedWorkingHours.EIGHT,
                wednesday, PlannedWorkingHours.EIGHT
            ),
            Map.of(
                tuesday, List.of(overtimeReduction),
                wednesday, List.of(overtimeReduction, halfDayVacation)
            )
        );

        assertThat(sut.shouldWorkingHours(tuesday)).hasValue(new ShouldWorkingHours(Duration.ofHours(4)));
        assertThat(sut.shouldWorkingHours(wednesday)).hasValue(new ShouldWorkingHours(Duration.ofHours(2)));
    }

    @Test
    void ensureOvertimeReductionWhenAllDaysHaveFullAbsences() {
        // Test case: Overtime period completely covered by other absences
        final LocalDate wednesday = LocalDate.of(2025, 1, 14);
        final LocalDate tuesday = wednesday.minusDays(1);

        final Instant tuesdayInstant = tuesday.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant wednesdayInstant = wednesday.atStartOfDay().toInstant(ZoneOffset.UTC);

        // Overtime reduction for 2 days with 4 hours total
        final Absence overtimeReduction = new Absence(
            new UserId("user"),
            tuesdayInstant,
            wednesdayInstant,
            FULL,
            locale -> "Overtime reduction",
            PINK,
            OVERTIME,
            Duration.ofHours(4L)
        );

        // Full day absences on both days
        final Absence tuesdayVacation = new Absence(
            new UserId("user"),
            tuesdayInstant,
            tuesdayInstant,
            FULL,
            locale -> "Vacation",
            PINK,
            HOLIDAY
        );

        final Absence wednesdayVacation = new Absence(
            new UserId("user"),
            wednesdayInstant,
            wednesdayInstant,
            FULL,
            locale -> "Vacation",
            PINK,
            HOLIDAY
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(
            Map.of(
                tuesday, PlannedWorkingHours.EIGHT,
                wednesday, PlannedWorkingHours.EIGHT
            ),
            Map.of(
                tuesday, List.of(overtimeReduction, tuesdayVacation),
                wednesday, List.of(overtimeReduction, wednesdayVacation)
            )
        );

        assertThat(sut.shouldWorkingHours(tuesday)).hasValue(new ShouldWorkingHours(Duration.ZERO));
        assertThat(sut.shouldWorkingHours(wednesday)).hasValue(new ShouldWorkingHours(Duration.ZERO));
    }

    @Test
    void ensureOvertimeReductionWithMixedAbsences() {
        // Complex test case: Multiple types of absences during overtime period
        final LocalDate monday = LocalDate.of(2025, 1, 12);
        final LocalDate tuesday = monday.plusDays(1);
        final LocalDate wednesday = monday.plusDays(2);
        final LocalDate thursday = monday.plusDays(3);

        final Instant mondayInstant = monday.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant tuesdayInstant = tuesday.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant wednesdayInstant = wednesday.atStartOfDay().toInstant(ZoneOffset.UTC);
        final Instant thursdayInstant = thursday.atStartOfDay().toInstant(ZoneOffset.UTC);

        // Overtime reduction for 4 days with 8 hours total
        final Absence overtimeReduction = new Absence(
            new UserId("user"),
            mondayInstant,
            thursdayInstant,
            FULL,
            locale -> "Overtime reduction",
            PINK,
            OVERTIME,
            Duration.ofHours(8L)
        );

        // Various absences:
        // Monday: no other absence (full day available)
        // Tuesday: morning absence (half day available)
        // Wednesday: full day absence (not available)
        // Thursday: noon absence (half day available)

        final Absence tuesdayMorning = new Absence(
            new UserId("user"),
            tuesdayInstant,
            tuesdayInstant,
            MORNING,
            locale -> "Morning absence",
            PINK,
            HOLIDAY
        );

        final Absence wednesdayFull = new Absence(
            new UserId("user"),
            wednesdayInstant,
            wednesdayInstant,
            FULL,
            locale -> "Full day absence",
            PINK,
            HOLIDAY
        );

        final Absence thursdayNoon = new Absence(
            new UserId("user"),
            thursdayInstant,
            thursdayInstant,
            NOON,
            locale -> "Noon absence",
            PINK,
            HOLIDAY
        );

        final WorkingTimeCalendar sut = new WorkingTimeCalendar(
            Map.of(
                monday, PlannedWorkingHours.EIGHT,
                tuesday, PlannedWorkingHours.EIGHT,
                wednesday, PlannedWorkingHours.EIGHT,
                thursday, PlannedWorkingHours.EIGHT
            ),
            Map.of(
                monday, List.of(overtimeReduction),
                tuesday, List.of(overtimeReduction, tuesdayMorning),
                wednesday, List.of(overtimeReduction, wednesdayFull),
                thursday, List.of(overtimeReduction, thursdayNoon)
            )
        );

        // Effective working days: 1 (Mon) + 0.5 (Tue) + 0 (Wed) + 0.5 (Thu) = 2.0 days
        // Overtime per effective day: 8 hours / 2 = 4 hours
        // Monday: 8 - 4 = 4 hours
        // Tuesday: 4 (half day) - 2 (half of 4) = 2 hours
        // Wednesday: 0 hours (full absence)
        // Thursday: 4 (half day) - 2 (half of 4) = 2 hours
        assertThat(sut.shouldWorkingHours(monday)).hasValue(new ShouldWorkingHours(Duration.ofHours(4)));
        assertThat(sut.shouldWorkingHours(tuesday)).hasValue(new ShouldWorkingHours(Duration.ofHours(2)));
        assertThat(sut.shouldWorkingHours(wednesday)).hasValue(new ShouldWorkingHours(Duration.ZERO));
        assertThat(sut.shouldWorkingHours(thursday)).hasValue(new ShouldWorkingHours(Duration.ofHours(2)));
    }

    @Nested
    class AbsenceGetter {

        @Test
        void ensureAbsenceReturnsEmptyWhenDateNotInCalendar() {
            final LocalDate today = LocalDate.now();
            final WorkingTimeCalendar sut = new WorkingTimeCalendar(Map.of(), Map.of());

            final Optional<List<Absence>> actual = sut.absence(today);
            assertThat(actual).isEmpty();
        }

        @Test
        void ensureAbsenceReturnsOvertimeWhenHasToWork() {
            final LocalDate today = LocalDate.now();
            final Instant todayInstant = today.atStartOfDay().toInstant(ZoneOffset.UTC);

            final Absence overtimeReduction = new Absence(
                new UserId("user"),
                todayInstant,
                todayInstant,
                FULL,
                locale -> "Overtime reduction",
                PINK,
                OVERTIME,
                Duration.ofHours(8L)
            );

            final WorkingTimeCalendar sut = new WorkingTimeCalendar(
                Map.of(today, PlannedWorkingHours.EIGHT),
                Map.of(today, List.of(overtimeReduction))
            );

            final Optional<List<Absence>> actual = sut.absence(today);
            assertThat(actual).contains(List.of(overtimeReduction));
        }

        @Test
        void ensureAbsenceFiltersOutOvertimeWhenDoesNotHaveToWork() {
            final LocalDate today = LocalDate.now();
            final Instant todayInstant = today.atStartOfDay().toInstant(ZoneOffset.UTC);

            final Absence overtimeReduction = new Absence(
                new UserId("user"),
                todayInstant,
                todayInstant,
                FULL,
                locale -> "Overtime reduction",
                PINK,
                OVERTIME,
                Duration.ofHours(4L)
            );

            final WorkingTimeCalendar sut = new WorkingTimeCalendar(
                Map.of(today, PlannedWorkingHours.ZERO),
                Map.of(today, List.of(overtimeReduction))
            );

            final Optional<List<Absence>> actual = sut.absence(today);
            assertThat(actual).contains(List.of());
        }

        @Test
        void ensureAbsenceReturnsAllAbsencesIncludingOvertimeWhenHasToWorkHalfDay() {
            final LocalDate today = LocalDate.now();
            final Instant todayInstant = today.atStartOfDay().toInstant(ZoneOffset.UTC);

            final Absence overtimeReduction = new Absence(
                new UserId("user"),
                todayInstant,
                todayInstant,
                FULL,
                locale -> "Overtime reduction",
                PINK,
                OVERTIME,
                Duration.ofHours(4L)
            );

            final Absence halfDayApplicationForLeave = new Absence(
                new UserId("user"),
                todayInstant,
                todayInstant,
                MORNING,
                locale -> "Holiday",
                PINK,
                HOLIDAY
            );

            final WorkingTimeCalendar sut = new WorkingTimeCalendar(
                Map.of(today, PlannedWorkingHours.EIGHT),
                Map.of(today, List.of(overtimeReduction, halfDayApplicationForLeave))
            );

            final Optional<List<Absence>> actual = sut.absence(today);
            assertThat(actual).contains(List.of(overtimeReduction, halfDayApplicationForLeave));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 8})
        void ensureAbsenceFiltersOutOvertimeButKeepsFullDayHoliday(int plannedWorkingHoursValue) {
            final LocalDate today = LocalDate.now();
            final Instant todayInstant = today.atStartOfDay().toInstant(ZoneOffset.UTC);

            final Absence overtimeReduction = new Absence(
                new UserId("user"),
                todayInstant,
                todayInstant,
                FULL,
                locale -> "Overtime reduction",
                PINK,
                OVERTIME,
                Duration.ofHours(4L)
            );

            final Absence fullDayApplicationForLeave = new Absence(
                new UserId("user"),
                todayInstant,
                todayInstant,
                FULL,
                locale -> "Holiday",
                PINK,
                HOLIDAY
            );

            final WorkingTimeCalendar sut = new WorkingTimeCalendar(
                Map.of(today, new PlannedWorkingHours(Duration.ofHours(plannedWorkingHoursValue))),
                Map.of(today, List.of(overtimeReduction, fullDayApplicationForLeave))
            );

            final Optional<List<Absence>> actual = sut.absence(today);
            assertThat(actual).contains(List.of(fullDayApplicationForLeave));
        }
    }
}
