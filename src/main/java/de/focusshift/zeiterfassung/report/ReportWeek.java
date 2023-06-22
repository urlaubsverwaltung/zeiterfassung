package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.overtime.OvertimeDuration;
import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.reducing;

record ReportWeek(LocalDate firstDateOfWeek, List<ReportDay> reportDays) {

    public Map<UserLocalId, OvertimeDuration> overtimeDurationEndOfWeekByUser() {
        return reportDays.stream()
            .map(ReportDay::accumulatedOvertimeToDateByUser)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .collect(groupingBy(
                Map.Entry::getKey,
                mapping(Map.Entry::getValue, reducing(OvertimeDuration.ZERO, OvertimeDuration::plus))
            ));
    }

    public List<LocalDate> dateOfWeeks() {
        return IntStream.range(0, 7).mapToObj(d -> firstDateOfWeek().plusDays(d)).toList();
    }

    public List<DayOfWeek> dayOfWeeks() {
        return IntStream.range(0, 7).mapToObj(d -> firstDateOfWeek().plusDays(d).getDayOfWeek()).toList();
    }

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
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }

    public Map<User, List<PlannedWorkingHours>> plannedWorkingHoursByUser() {

        final Map<User, List<PlannedWorkingHours>> plannedWorkingHoursByUserLocalId = new HashMap<>();

        for (ReportDay reportDay : reportDays) {
            reportDay.plannedWorkingHoursByUser().forEach((user, plannedWorkingHours) -> {
                plannedWorkingHoursByUserLocalId.compute(user, (unused, planned) -> {
                    if (planned == null) {
                        planned = new ArrayList<>();
                    }
                    planned.add(plannedWorkingHours);
                    return planned;
                });
            });
        }

        return plannedWorkingHoursByUserLocalId;
    }
}
