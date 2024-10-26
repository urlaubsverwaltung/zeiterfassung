package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.HasWorkedHoursRatio;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Predicate.not;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) implements HasWorkDurationByUser, HasWorkedHoursRatio {

    public PlannedWorkingHours plannedWorkingHours() {
        return reportDays.stream()
            .map(ReportDay::plannedWorkingHours)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser() {
        final HashMap<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser = new HashMap<>();

        for (ReportDay reportDay : reportDays) {
            reportDay.plannedWorkingHoursByUser().forEach((userIdComposite, plannedWorkingHours) -> {
                final PlannedWorkingHours hours = plannedWorkingHoursByUser.getOrDefault(userIdComposite, PlannedWorkingHours.ZERO);
                plannedWorkingHoursByUser.put(userIdComposite, hours.plus(plannedWorkingHours));
            });
        }

        return plannedWorkingHoursByUser;
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return reportDays.stream()
            .map(ReportDay::shouldWorkingHours)
            .reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    public Map<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser() {
        final HashMap<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser = new HashMap<>();

        for (ReportDay reportDay : reportDays) {
            reportDay.shouldWorkingHoursByUser().forEach((userIdComposite, shouldWorkingHours) -> {
                final ShouldWorkingHours hours = shouldWorkingHoursByUser.getOrDefault(userIdComposite, ShouldWorkingHours.ZERO);
                shouldWorkingHoursByUser.put(userIdComposite, hours.plus(shouldWorkingHours));
            });
        }

        return shouldWorkingHoursByUser;
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

    public Map<UserIdComposite, WorkDuration> workDurationByUser() {
        final HashMap<UserIdComposite, WorkDuration> byUser = new HashMap<>();

        for (ReportDay reportDay : reportDays) {
            reportDay.workDurationByUser().forEach((userIdComposite, dayDuration) -> {
                final WorkDuration summedDuration = byUser.getOrDefault(userIdComposite, WorkDuration.ZERO);
                byUser.put(userIdComposite, summedDuration.plus(dayDuration));
            });
        }

        return byUser;
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }
}
