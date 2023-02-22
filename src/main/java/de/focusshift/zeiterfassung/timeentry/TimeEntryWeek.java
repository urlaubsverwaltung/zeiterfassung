package de.focusshift.zeiterfassung.timeentry;

import org.threeten.extra.YearWeek;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.List;

import static java.math.RoundingMode.CEILING;
import static java.time.Month.DECEMBER;
import static java.time.temporal.WeekFields.ISO;
import static java.util.function.Predicate.not;

record TimeEntryWeek(LocalDate firstDateOfWeek, PlannedWorkingHours plannedWorkingHours, List<TimeEntryDay> days) {

    /**
     * @return overtime {@linkplain Duration}. can be negative.
     */
    public Duration overtime() {

        final Duration worked = days.stream()
            .map(TimeEntryDay::workDuration)
            .map(WorkDuration::minutes)
            .reduce(Duration.ZERO, Duration::plus);

        return worked.minus(plannedWorkingHours.minutes());
    }

    public WorkDuration workDuration() {

        final Duration duration = days
            .stream()
            .map(TimeEntryDay::timeEntries)
            .flatMap(Collection::stream)
            .filter(not(TimeEntry::isBreak))
            .map(TimeEntry::workDuration)
            .map(WorkDuration::value)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
    }

    /**
     * Ratio of worked hours to planned hours. Does not include absences like public holidays.
     *
     * @return value between 0 and 1
     */
    public BigDecimal workedHoursRatio() {

        final double planned = plannedWorkingHours.minutes().toMinutes();
        final double worked = workDuration().minutes().toMinutes();

        if (worked == 0) {
            return BigDecimal.ZERO;
        }

        if (planned == 0) {
            return BigDecimal.ONE;
        }

        final BigDecimal ratio = BigDecimal.valueOf(worked).divide(BigDecimal.valueOf(planned), 2, CEILING);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio;
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
