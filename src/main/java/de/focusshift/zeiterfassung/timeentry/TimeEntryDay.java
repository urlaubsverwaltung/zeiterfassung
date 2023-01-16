package de.focusshift.zeiterfassung.timeentry;

import org.threeten.extra.YearWeek;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import static java.time.Month.DECEMBER;
import static java.time.temporal.WeekFields.ISO;

record TimeEntryDay(LocalDate date,
                    List<TimeEntry> timeEntries) {

    public WorkDuration workDuration() {

        final Duration duration = timeEntries
            .stream()
            .map(TimeEntry::workDuration)
            .map(WorkDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }
}
