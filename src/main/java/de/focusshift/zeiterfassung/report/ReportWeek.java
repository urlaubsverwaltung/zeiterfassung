package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntryDuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) {

    public TimeEntryDuration workDuration() {
        final Duration duration = reportDays
            .stream()
            .map(ReportDay::workDuration)
            .map(TimeEntryDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new TimeEntryDuration(duration);
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }
}
