package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEntryWeekTest {

    private static final ZoneId ZONE_ID_BERLIN = ZoneId.of("Europe/Berlin");

    @Test
    void ensureWorkDuration() {

        final LocalDate startOfWeek = LocalDate.of(2022, 1, 4);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZonedDateTime firstStart = ZonedDateTime.of(LocalDateTime.of(2022, 1, 4, 9, 0, 0), ZONE_ID_BERLIN);
        final ZonedDateTime firstEnd = ZonedDateTime.of(LocalDateTime.of(2022, 1, 4, 11, 30, 0), ZONE_ID_BERLIN);
        final TimeEntry firstTimeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work first day", firstStart, firstEnd, false);

        final ZonedDateTime secondStart = ZonedDateTime.of(LocalDateTime.of(2022, 1, 5, 17, 15, 0), ZONE_ID_BERLIN);
        final ZonedDateTime secondEnd = ZonedDateTime.of(LocalDateTime.of(2022, 1, 5, 17, 30, 0), ZONE_ID_BERLIN);
        final TimeEntry secondTimeEntry = new TimeEntry(new TimeEntryId(2L), userIdComposite, "hard work second day", secondStart, secondEnd, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(startOfWeek, PlannedWorkingHours.EIGHT, List.of(new TimeEntryDay(false, startOfWeek, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(firstTimeEntry, secondTimeEntry), List.of())));

        final Duration actualDuration = timeEntryWeek.workDuration().duration();
        assertThat(actualDuration).isEqualTo(Duration.ofHours(2).plusMinutes(45));
    }

    @Test
    void ensureWorkDurationSumsUpTouchingNextCalendarWeek() {

        final LocalDate startOfWeek = LocalDate.of(2022, 1, 4);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(2022, 1, 9, 23, 0, 0), ZONE_ID_BERLIN);
        final ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(2022, 1, 10, 2, 0, 0), ZONE_ID_BERLIN);

        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work in the night", start, end, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(startOfWeek, PlannedWorkingHours.EIGHT, List.of(new TimeEntryDay(false, startOfWeek, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of())));

        final Duration actualDuration = timeEntryWeek.workDuration().duration();
        assertThat(actualDuration).isEqualTo(Duration.ofHours(3));
    }

    @ParameterizedTest
    @CsvSource({
        "2021-12-27,52",
        "2022-01-03,1",
        "2022-09-26,39",
        "2022-12-26,52",
        "2023-01-02,1",
        "2024-12-23,52",
        "2024-12-30,1",
        "2025-01-06,2",
        "2025-12-22,52",
        "2025-12-29,1",
        "2026-01-05,2",
        "2026-12-28,53",
        "2027-01-04,1",
        "2027-12-27,52",
        "2028-01-03,1",
    })
    void ensureCalendarWeek(String date, int week) {
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(LocalDate.parse(date), PlannedWorkingHours.ZERO, List.of());
        assertThat(timeEntryWeek.calendarWeek()).isEqualTo(week);
    }
}
