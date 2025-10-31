package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.function.Predicate.not;

/**
 * Functions used by Report domain objects to calculate stuff regarding durations.
 */
class ReportFunctions {

    private ReportFunctions() {
    }

    static <T> PlannedWorkingHours summarizePlannedWorkingHours(List<T> elements, Function<T, PlannedWorkingHours> supplier) {
        return elements.stream().map(supplier).reduce(PlannedWorkingHours.ZERO, PlannedWorkingHours::plus);
    }

    static Map<UserIdComposite, PlannedWorkingHours> summarizePlannedWorkingHoursByUser(List<? extends HasWorkDurationByUser> elements) {
        final HashMap<UserIdComposite, PlannedWorkingHours> plannedWorkingHoursByUser = new HashMap<>();

        for (HasWorkDurationByUser element : elements) {
            element.plannedWorkingHoursByUser().forEach((userIdComposite, plannedWorkingHours) -> {
                final PlannedWorkingHours hours = plannedWorkingHoursByUser.getOrDefault(userIdComposite, PlannedWorkingHours.ZERO);
                plannedWorkingHoursByUser.put(userIdComposite, hours.plus(plannedWorkingHours));
            });
        }

        return plannedWorkingHoursByUser;
    }

    static <T> ShouldWorkingHours summarizeShouldWorkingHours(List<T> elements, Function<T, ShouldWorkingHours> supplier) {
        return elements.stream().map(supplier).reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    static Map<UserIdComposite, ShouldWorkingHours> summarizeShouldWorkingHoursByUser(List<? extends HasWorkDurationByUser> elements) {
        final HashMap<UserIdComposite, ShouldWorkingHours> shouldWorkingHoursByUser = new HashMap<>();

        for (HasWorkDurationByUser element : elements) {
            element.shouldWorkingHoursByUser().forEach((userIdComposite, shouldWorkingHours) -> {
                final ShouldWorkingHours hours = shouldWorkingHoursByUser.getOrDefault(userIdComposite, ShouldWorkingHours.ZERO);
                shouldWorkingHoursByUser.put(userIdComposite, hours.plus(shouldWorkingHours));
            });
        }

        return shouldWorkingHoursByUser;
    }

    static <T> WorkDuration calculateAverageDayWorkDuration(List<T> elements, Function<T, WorkDuration> supplier) {

        final double averageMinutes = elements.stream()
            .map(supplier)
            .filter(not(WorkDuration.ZERO::equals))
            .map(WorkDuration::durationInMinutes)
            .mapToLong(Duration::toMinutes)
            .average()
            .orElse(0.0);// o.O

        final Duration duration = Duration.ofMinutes(Math.round(averageMinutes));

        return new WorkDuration(duration);
    }

    static <T> WorkDuration summarizeWorkDuration(List<T> elements, Function<T, WorkDuration> supplier) {
        return elements.stream().map(supplier).reduce(WorkDuration.ZERO, WorkDuration::plus);
    }

    static Map<UserIdComposite, WorkDuration> summarizeWorkDurationByUser(List<? extends HasWorkDurationByUser> elements) {
        final HashMap<UserIdComposite, WorkDuration> byUser = new HashMap<>();

        for (HasWorkDurationByUser element : elements) {
            element.workDurationByUser().forEach((userIdComposite, dayDuration) -> {
                final WorkDuration summedDuration = byUser.getOrDefault(userIdComposite, WorkDuration.ZERO);
                byUser.put(userIdComposite, summedDuration.plus(dayDuration));
            });
        }

        return byUser;
    }
}
