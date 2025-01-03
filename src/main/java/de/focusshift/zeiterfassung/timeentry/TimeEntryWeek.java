package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Collection;
import java.util.List;

import static java.util.Locale.GERMANY;
import static java.util.function.Predicate.not;

/**
 * Represents a week of time entries.
 *
 * @param firstDateOfWeek the first date of the week, maybe monday or sunday.
 * @param plannedWorkingHours {@link PlannedWorkingHours} for this week
 * @param days sorted list of days
 */
record TimeEntryWeek(
    LocalDate firstDateOfWeek,
    PlannedWorkingHours plannedWorkingHours,
    List<TimeEntryDay> days
) implements HasWorkedHoursRatio {

    public ShouldWorkingHours shouldWorkingHours() {
        return days.stream().map(TimeEntryDay::shouldWorkingHours).reduce(ShouldWorkingHours.ZERO, ShouldWorkingHours::plus);
    }

    /**
     * @return overtime {@linkplain Duration}. can be negative.
     */
    public Duration overtime() {

        final Duration worked = days.stream()
            .map(TimeEntryDay::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus)
            .durationInMinutes();

        return worked.minus(shouldWorkingHours().durationInMinutes());
    }

    public WorkDuration workDuration() {
        return days
            .stream()
            .map(TimeEntryDay::timeEntries)
            .flatMap(Collection::stream)
            .filter(not(TimeEntry::isBreak))
            .map(TimeEntry::workDuration)
            .reduce(WorkDuration.ZERO, WorkDuration::plus);
    }

    public int week() {
        final int minimalDaysInFirstWeek = WeekFields.of(GERMANY).getMinimalDaysInFirstWeek();
        final WeekFields weekFields = WeekFields.of(firstDateOfWeek.getDayOfWeek(), minimalDaysInFirstWeek);
        return firstDateOfWeek.get(weekFields.weekOfWeekBasedYear());
    }

    public LocalDate lastDateOfWeek() {
        return firstDateOfWeek.plusDays(6);
    }

    public List<TimeEntry> timeEntries() {
        return days.stream().map(TimeEntryDay::timeEntries)
            .flatMap(Collection::stream)
            .toList();
    }
}
