package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

record TimeEntryDay(LocalDate date, List<TimeEntry> timeEntries) {

    public TimeEntryDuration workDuration() {

        final Duration duration = timeEntries
            .stream()
            .map(TimeEntry::workDuration)
            .map(TimeEntryDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new TimeEntryDuration(duration);
    }
}
