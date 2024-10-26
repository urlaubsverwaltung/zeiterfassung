package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.HasWorkedHoursRatio;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.YearMonth;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Predicate.not;

record ReportMonth(YearMonth yearMonth, List<ReportWeek> weeks) implements HasWorkDurationByUser, HasWorkedHoursRatio {

    public PlannedWorkingHours plannedWorkingHours() {
        return weeks.stream()
            .map(ReportWeek::plannedWorkingHours)
            .reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    public Map<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser() {
        final HashMap<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser = new HashMap<>();

        for (ReportWeek week : weeks) {
            week.plannedWorkingHoursByUser().forEach((userIdComposite, plannedWorkingHours) -> {
                final PlannedWorkingHours hours = plannedWorkingHoursByUser.getOrDefault(userIdComposite, PlannedWorkingHours.ZERO);
                plannedWorkingHoursByUser.put(userIdComposite, hours.plus(plannedWorkingHours));
            });
        }

        return plannedWorkingHoursByUser;
    }

    public ShouldWorkingHours shouldWorkingHours() {
        return weeks.stream()
            .map(ReportWeek::shouldWorkingHours)
            .reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    public Map<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser() {
        final HashMap<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser = new HashMap<>();

        for (ReportWeek week : weeks) {
            week.shouldWorkingHoursByUser().forEach((userIdComposite, shouldWorkingHours) -> {
                final ShouldWorkingHours hours = shouldWorkingHoursByUser.getOrDefault(userIdComposite, ShouldWorkingHours.ZERO);
                shouldWorkingHoursByUser.put(userIdComposite, hours.plus(shouldWorkingHours));
            });
        }

        return shouldWorkingHoursByUser;
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

    public Map<UserIdComposite, WorkDuration> workDurationByUser() {
        final HashMap<UserIdComposite, WorkDuration> byUser = new HashMap<>();

        for (ReportWeek week : weeks) {
            week.workDurationByUser().forEach((userIdComposite, dayDuration) -> {
                final WorkDuration summedDuration = byUser.getOrDefault(userIdComposite, WorkDuration.ZERO);
                byUser.put(userIdComposite, summedDuration.plus(dayDuration));
            });
        }

        return byUser;
    }
}
