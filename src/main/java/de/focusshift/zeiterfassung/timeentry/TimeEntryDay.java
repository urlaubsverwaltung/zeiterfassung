package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.util.function.Predicate.not;

record TimeEntryDay(LocalDate date, List<TimeEntry> timeEntries) {

    public WorkDuration workDuration() {

        final Duration duration = timeEntries
            .stream()
            .filter(not(TimeEntry::isBreak))
            .map(TimeEntry::workDuration)
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }
}
