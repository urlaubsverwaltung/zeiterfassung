package de.focusshift.zeiterfassung.timeentry;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeEntryDayTest {

    @Test
    void workDurationForTimeEntriesOnly() {
        final TimeEntry e1 = entry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z", false);
        final TimeEntry e2 = entry("2025-09-26T09:00:00Z", "2025-09-26T10:00:00Z", false);
        final List<TimeEntry> timeEntries = List.of(e1, e2);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void workDurationForTimeEntriesOnlyWithOverlap() {
        final TimeEntry e1 = entry("2025-09-26T08:00:00Z", "2025-09-26T09:00:00Z", false);
        final TimeEntry e2 = entry("2025-09-26T08:30:00Z", "2025-09-26T09:30:00Z", false);
        final List<TimeEntry> timeEntries = List.of(e1, e2);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void workDurationForTimeEntryWithOverlappingBreak() {
        final TimeEntry timeEntry = entry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z", false);
        final TimeEntry breakEntry = entry("2025-09-26T12:00:00Z", "2025-09-26T13:00:00Z", true);
        final List<TimeEntry> timeEntries = List.of(timeEntry, breakEntry);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void workDurationForTimeEntryWithOverlappingBreakAtStart() {
        final TimeEntry timeEntry = entry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z", false);
        final TimeEntry breakEntry = entry("2025-09-26T07:00:00Z", "2025-09-26T09:00:00Z", true);
        final List<TimeEntry> timeEntries = List.of(timeEntry, breakEntry);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void workDurationForTimeEntryWithOverlappingBreakAtEnd() {
        final TimeEntry timeEntry = entry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z", false);
        final TimeEntry breakEntry = entry("2025-09-26T16:00:00Z", "2025-09-26T18:00:00Z", true);
        final List<TimeEntry> timeEntries = List.of(timeEntry, breakEntry);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void workDurationForTimeEntriesWithOverlappingBreak() {
        final TimeEntry e1 = entry("2025-09-26T08:00:00Z", "2025-09-26T12:15:00Z", false);
        final TimeEntry e2 = entry("2025-09-26T12:45:00Z", "2025-09-26T17:00:00Z", false);
        final TimeEntry breakEntry = entry("2025-09-26T12:00:00Z", "2025-09-26T13:00:00Z", true);
        final List<TimeEntry> timeEntries = List.of(e1, e2, breakEntry);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void workDurationForTimeEntriesWithMultipleOverlappingBreak() {
        final TimeEntry timeEntry = entry("2025-09-26T08:00:00Z", "2025-09-26T17:00:00Z", false);
        final TimeEntry b1 = entry("2025-09-26T12:00:00Z", "2025-09-26T13:00:00Z", true);
        final TimeEntry b2 = entry("2025-09-26T12:30:00Z", "2025-09-26T13:30:00Z", true);
        final List<TimeEntry> timeEntries = List.of(timeEntry, b1, b2);
        final TimeEntryDay day = new TimeEntryDay(false, LocalDate.now(), null, null, timeEntries, null);
        assertThat(day.workDuration().durationInMinutes()).isEqualTo(Duration.ofHours(7).plusMinutes(30));
    }

    private static TimeEntry entry(String start, String end, boolean isBreak) {
        return new TimeEntry(null, null, "", ZonedDateTime.parse(start), ZonedDateTime.parse(end), isBreak);
    }
}
