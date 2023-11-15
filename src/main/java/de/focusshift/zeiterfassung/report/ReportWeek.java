package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.util.function.Predicate.not;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) {

    public PlannedWorkingHours plannedWorkingHours() {
        return reportDays.stream()
            .map(ReportDay::plannedWorkingHours)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public WorkDuration averageDayWorkDuration() {

        final double averageMinutes = reportDays().stream()
            .map(ReportDay::workDuration)
            .filter(not(WorkDuration.ZERO::equals))
            .map(WorkDuration::minutes)
            .mapToLong(Duration::toMinutes)
            .average()
            .orElse(0.0);// o.O

        final Duration duration = Duration.ofMinutes(Math.round(averageMinutes));

        return new WorkDuration(duration);
    }

    public WorkDuration workDuration() {
        final Duration duration = reportDays
            .stream()
            .map(ReportDay::workDuration)
            .map(WorkDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }
}
