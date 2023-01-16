package de.focusshift.zeiterfassung.timeentry;

import org.threeten.extra.YearWeek;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.Month.DECEMBER;
import static java.time.temporal.WeekFields.ISO;

record TimeEntryWeek(LocalDate firstDateOfWeek, List<TimeEntryDay> days) {

    public WorkDuration workDuration() {

        final Duration duration = days
            .stream()
            .map(TimeEntryDay::timeEntries).flatMap(Collection::stream)
            .map(TimeEntry::workDuration)
            .map(WorkDuration::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(duration);
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
                .collect(Collectors.toList());
    }
}
