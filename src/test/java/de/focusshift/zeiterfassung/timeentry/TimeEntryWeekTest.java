package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
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

        final ZonedDateTime firstStart = ZonedDateTime.of(LocalDateTime.of(2022, 1, 4, 9, 0, 0), ZONE_ID_BERLIN);
        final ZonedDateTime firstEnd = ZonedDateTime.of(LocalDateTime.of(2022, 1, 4, 11, 30, 0), ZONE_ID_BERLIN);
        final TimeEntry firstTimeEntry = new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work first day", firstStart, firstEnd, false);

        final ZonedDateTime secondStart = ZonedDateTime.of(LocalDateTime.of(2022, 1, 5, 17, 15, 0), ZONE_ID_BERLIN);
        final ZonedDateTime secondEnd = ZonedDateTime.of(LocalDateTime.of(2022, 1, 5, 17, 30, 0), ZONE_ID_BERLIN);
        final TimeEntry secondTimeEntry = new TimeEntry(new TimeEntryId(2L), new UserId("batman"), "hard work second day", secondStart, secondEnd, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(startOfWeek, List.of(new TimeEntryDay(startOfWeek, List.of(firstTimeEntry, secondTimeEntry))));

        final Duration actualDuration = timeEntryWeek.workDuration().value();
        assertThat(actualDuration).isEqualTo(Duration.ofHours(2).plusMinutes(45));
    }

    @Test
    void ensureWorkDurationSumsUpTouchingNextWeek() {

        final LocalDate startOfWeek = LocalDate.of(2022, 1, 4);

        final ZonedDateTime start = ZonedDateTime.of(LocalDateTime.of(2022, 1, 9, 23, 0, 0), ZONE_ID_BERLIN);
        final ZonedDateTime end = ZonedDateTime.of(LocalDateTime.of(2022, 1, 10, 2, 0, 0), ZONE_ID_BERLIN);

        final TimeEntry timeEntry = new TimeEntry(new TimeEntryId(1L), new UserId("batman"), "hard work in the night", start, end, false);

        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(startOfWeek, List.of(new TimeEntryDay(startOfWeek, List.of(timeEntry))));

        final Duration actualDuration = timeEntryWeek.workDuration().value();
        assertThat(actualDuration).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void ensureYear() {

        final LocalDate date = LocalDate.of(2022, 1, 1);
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(date, List.of());

        assertThat(timeEntryWeek.year()).isEqualTo(2022);
    }

    @Test
    void ensureWeek() {
        // 2021-12-27 would be correct
        final LocalDate actuallyWrongFirstDayOfWeek = LocalDate.of(2022, 1, 1);
        final TimeEntryWeek timeEntryWeek = new TimeEntryWeek(actuallyWrongFirstDayOfWeek, List.of());

        assertThat(timeEntryWeek.week()).isEqualTo(52);
    }
}
