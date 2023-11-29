package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Test;

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

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(startOfWeek, PlannedWorkingHours.EIGHT, List.of(new TimeEntryDay(startOfWeek, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(firstTimeEntry, secondTimeEntry), List.of())));

        final Duration actualDuration = timeEntryWeek.workDuration().duration();
        assertThat(actualDuration).isEqualTo(Duration.ofHours(2).plusMinutes(45));
    }

    @Test
    void ensureWorkDurationSumsUpTouchingNextWeek() {

        final LocalDate startOfWeek = LocalDate.of(2022, 1, 4);

        final UserId userId = new UserId("batman");
        final UserLocalId userLocalId = new UserLocalId(42L);
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        final ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(2022, 1, 9, 23, 0, 0), ZONE_ID_BERLIN);
        final ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(2022, 1, 10, 2, 0, 0), ZONE_ID_BERLIN);

        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), userIdComposite, "hard work in the night", start, end, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(startOfWeek, PlannedWorkingHours.EIGHT, List.of(new TimeEntryDay(startOfWeek, PlannedWorkingHours.EIGHT, ShouldWorkingHours.EIGHT, List.of(timeEntry), List.of())));

        final Duration actualDuration = timeEntryWeek.workDuration().duration();
        assertThat(actualDuration).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void ensureYear() {

        final LocalDate date = LocalDate.of(2022, 1, 1);
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(date, PlannedWorkingHours.ZERO, List.of());

        assertThat(timeEntryWeek.year()).isEqualTo(2022);
    }

    @Test
    void ensureWeek() {
        // 2021-12-27 would be correct
        final LocalDate actuallyWrongFirstDayOfWeek = LocalDate.of(2022, 1, 1);
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(actuallyWrongFirstDayOfWeek, PlannedWorkingHours.ZERO, List.of());

        assertThat(timeEntryWeek.week()).isEqualTo(52);
    }
}
