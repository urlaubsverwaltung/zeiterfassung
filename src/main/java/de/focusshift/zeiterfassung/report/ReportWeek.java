package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static java.math.RoundingMode.CEILING;
import static java.util.function.Predicate.not;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) {

    public PlannedWorkingHours plannedWorkingHours() {
        return reportDays.stream()
            .map(ReportDay::plannedWorkingHours)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return reportDays.stream()
            .map(ReportDay::shouldWorkingHours)
            .reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    public WorkDuration averageDayWorkDuration() {

        final double averageMinutes = reportDays().stream()
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
        return reportDays
            .stream()
            .map(ReportDay::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }

    public BigDecimal workedHoursRatio() {

        final double planned = shouldWorkingHours().durationInMinutes().toMinutes();
        final double worked = workDuration().durationInMinutes().toMinutes();

        if (worked == 0) {
            return BigDecimal.ZERO;
        }

        if (planned == 0) {
            return BigDecimal.ONE;
        }

        final BigDecimal ratio = BigDecimal.valueOf(worked).divide(BigDecimal.valueOf(planned), 2, CEILING);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio;
    }
}
