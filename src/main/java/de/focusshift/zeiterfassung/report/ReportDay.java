package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.WorkDuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.util.function.Predicate.not;

record ReportDay(LocalDate date, List<ReportDayEntry> reportDayEntries) {

    public WorkDuration workDuration() {
        final Duration duration = reportDayEntries.stream()
            .filter(not(ReportDayEntry::isBreak))
            .map(ReportDayEntry::workDuration)
            .map(WorkDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }
}
