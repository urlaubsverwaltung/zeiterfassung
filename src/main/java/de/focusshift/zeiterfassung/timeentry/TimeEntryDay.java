package de.focusshift.zeiterfassung.timeentry;


import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.time.Duration.between;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;

/**
 * @param locked              whether this day is locked or not.
 *                            Note that this does not include whether it can be bypassed or not by a privileged person!
 * @param date                of the time entry day
 * @param plannedWorkingHours planned working hours
 * @param shouldWorkingHours  should working hours
 * @param timeEntries         list of time entries
 * @param absences            list of absences. could be one FULL absence or two absences MORNING and NOON
 */
record TimeEntryDay(
    boolean locked,
    LocalDate date,
    PlannedWorkingHours plannedWorkingHours,
    ShouldWorkingHours shouldWorkingHours,
    List<TimeEntry> timeEntries,
    List<Absence> absences
) implements HasWorkedHoursRatio {

    /**
     * @return overtime {@linkplain Duration}. can be negative.
     */
    public Duration overtime() {
        return workDuration().durationInMinutes().minus(shouldWorkingHours.durationInMinutes());
    }

    public WorkDuration workDuration() {
        // Merge work intervals
        final List<TimeRange> workIntervals = timeEntries.stream()
            .filter(not(TimeEntry::isBreak))
            .map(e -> new TimeRange(e.start(), e.end()))
            .sorted(comparing(TimeRange::start))
            .toList();
        final List<TimeRange> mergedWork = mergeIntervals(workIntervals);

        // Merge break intervals
        final List<TimeRange> breakIntervals = timeEntries.stream()
            .filter(TimeEntry::isBreak)
            .map(e -> new TimeRange(e.start(), e.end()))
            .sorted(comparing(TimeRange::start))
            .toList();
        final List<TimeRange> mergedBreaks = mergeIntervals(breakIntervals);

        // Collect overlaps between work and break using streams
        final List<TimeRange> overlaps = mergedWork.stream()
            .flatMap(work -> mergedBreaks.stream()
                .map(brk -> {
                    final ZonedDateTime start = work.start().isAfter(brk.start()) ? work.start() : brk.start();
                    final ZonedDateTime end = work.end().isBefore(brk.end()) ? work.end() : brk.end();
                    return end.isAfter(start) ? new TimeRange(start, end) : null;
                })
                .filter(Objects::nonNull)
            )
            .sorted(comparing(TimeRange::start))
            .toList();

        // Merge overlaps
        final List<TimeRange> mergedOverlaps = mergeIntervals(overlaps);

        // Calculate work time using streams
        final Duration totalWork = mergedWork.stream()
            .map(work -> between(work.start(), work.end()))
            .reduce(Duration.ZERO, Duration::plus);

        // Subtract break time using streams
        final Duration totalBreak = mergedOverlaps.stream()
            .map(interval -> between(interval.start(), interval.end()))
            .reduce(Duration.ZERO, Duration::plus);

        return new WorkDuration(totalWork.minus(totalBreak));
    }

    private static List<TimeRange> mergeIntervals(List<TimeRange> intervals) {
        final List<TimeRange> merged = new ArrayList<>();
        for (final TimeRange interval : intervals) {
            if (merged.isEmpty() || merged.getLast().end().isBefore(interval.start())) {
                merged.add(interval);
            } else {
                final TimeRange last = merged.removeLast();
                final ZonedDateTime newStart = last.start();
                final ZonedDateTime newEnd = last.end().isAfter(interval.end()) ? last.end() : interval.end();
                merged.add(new TimeRange(newStart, newEnd));
            }
        }
        return merged;
    }

    private record TimeRange(ZonedDateTime start, ZonedDateTime end) {
    }
}
