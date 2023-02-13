package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.WorkDuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) {

    public WorkDuration workDuration() {
        final Duration duration = reportDays
            .stream()
            .map(ReportDay::workDuration)
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }
}
