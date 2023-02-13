package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.WorkDuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

record ReportDay(LocalDate date, List<ReportDayEntry> reportDayEntries) {

    public WorkDuration workDuration() {
        final Duration duration = reportDayEntries.stream()
            .map(ReportDayEntry::workDuration)
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }
}
