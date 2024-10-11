package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.HasWorkedHoursRatio;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

import static java.util.function.Predicate.not;

record ReportMonth(YearMonth yearMonth, List<ReportWeek> weeks) implements HasWorkedHoursRatio {

    public PlannedWorkingHours plannedWorkingHours() {
        return weeks.stream()
            .map(ReportWeek::plannedWorkingHours)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return weeks.stream()
            .map(ReportWeek::shouldWorkingHours)
            .reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    public WorkDuration averageDayWorkDuration() {

        final double averageMinutes = weeks.stream()
            .map(ReportWeek::reportDays)
            .flatMap(Collection::stream)
            .map(ReportDay::workDuration)
            .filter(not(WorkDuration.ZERO::equals))
            .map(WorkDuration::durationInMinutes)
            .mapToLong(Duration::toMinutes)
            .average()
            .orElse(0.0);// o.O

        final Duration duration = Duration.ofMinutes(Math.round(averageMinutes));

        return new WorkDuration(duration);
    }

    public WorkDuration workDuration() {
        return weeks
            .stream()
            .map(ReportWeek::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);
    }
}
