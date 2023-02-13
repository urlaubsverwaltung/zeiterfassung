package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntryDuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.util.function.Predicate.not;

record ReportDay(LocalDate date, List<ReportDayEntry> reportDayEntries) {

    public TimeEntryDuration workDuration() {
        final Duration duration = reportDayEntries.stream()
            .filter(not(ReportDayEntry::isBreak))
            .map(ReportDayEntry::workDuration)
            .map(TimeEntryDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new TimeEntryDuration(duration);
    }
}
