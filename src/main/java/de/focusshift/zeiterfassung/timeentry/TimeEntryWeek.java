package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import org.threeten.extra.YearWeek;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.List;

import static java.time.Month.DECEMBER;
import static java.time.temporal.WeekFields.ISO;
import static java.util.function.Predicate.not;

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

    public int year() {
        return firstDateOfWeek.getYear();
    }

    public int week() {
        final int isoWeekOfYear = firstDateOfWeek.get(ISO.weekOfYear());
        if (isoWeekOfYear != 0) {
            return isoWeekOfYear;
        }

        final Year year = Year.of(firstDateOfWeek.getYear());
        final Year previousYear = year.minusYears(1);
        final LocalDate endOfDecember = previousYear.atMonth(DECEMBER).atEndOfMonth();
        final int week = endOfDecember.get(ISO.weekOfYear());
        return YearWeek.of(previousYear, week).getWeek();
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
