package de.focusshift.zeiterfassung.timeentry;


import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

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

        final List<Interval> workIntervals = mergeIntervals(workIntervals());
        final List<Interval> breakIntervals = mergeIntervals(breakIntervals());
        final List<Interval> overlaps = mergeIntervals(breakIntervalOverlaps(workIntervals, breakIntervals));

        final Duration totalWork = summarizeDuration(workIntervals);
        final Duration breakOverlaps = summarizeDuration(overlaps);

        return new WorkDuration(totalWork.minus(breakOverlaps));
    }

    private List<Interval> workIntervals() {
        return intervals(not(TimeEntry::isBreak));
    }

    private List<Interval> breakIntervals() {
        return intervals(TimeEntry::isBreak);
    }

    private List<Interval> intervals(Predicate<TimeEntry> filter) {
        return timeEntries.stream()
            .filter(filter)
            .map(e -> new Interval(e.start(), e.end()))
            .sorted(comparing(Interval::start))
            .toList();
    }

    /**
     * Calculate overlapping break intervals.
     *
     * <p>
     * Examples:
     *
     * <ul>
     *     <li>
     *         Worked from 08:00 to 12:00, break from 11:00 to 13:00
     *         <ul>
     *             <li>overlap from 11:00 to 12:00</li>
     *         </ul>
     *     </li>
     *     <li>
     *         Worked from 08:00 to 12:00, break from 08:15 to 08:30 and from 09:00 to 09:15
     *         <ul>
     *             <li>overlap from 08:15 to 08:30</li>
     *             <li> and from 09:00 to 09:15</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param mergedWorkIntervals non overlapping work intervals
     * @param mergedBreakIntervals non overlapping break intervals
     * @return intervals of overlapping break and work, sorted by start
     */
    private List<Interval> breakIntervalOverlaps(List<Interval> mergedWorkIntervals, List<Interval> mergedBreakIntervals) {
        return mergedWorkIntervals.stream()
            .flatMap(workInterval -> mergedBreakIntervals.stream()
                .map(breakInterval -> {
                    final ZonedDateTime start = workInterval.start().isAfter(breakInterval.start()) ? workInterval.start() : breakInterval.start();
                    final ZonedDateTime end = workInterval.end().isBefore(breakInterval.end()) ? workInterval.end() : breakInterval.end();
                    return end.isAfter(start) ? new Interval(start, end) : null;
                })
                .filter(Objects::nonNull)
            )
            .sorted(comparing(Interval::start))
            .toList();
    }

    private static List<Interval> mergeIntervals(List<Interval> intervals) {

        final List<Interval> merged = new ArrayList<>();

        for (final Interval interval : intervals) {
            if (merged.isEmpty() || merged.getLast().end().isBefore(interval.start())) {
                merged.add(interval);
            } else {
                final Interval last = merged.removeLast();
                final ZonedDateTime newStart = last.start();
                final ZonedDateTime newEnd = last.end().isAfter(interval.end()) ? last.end() : interval.end();
                merged.add(new Interval(newStart, newEnd));
            }
        }

        return merged;
    }

    private Duration summarizeDuration(List<Interval> intervals) {
        return intervals.stream()
            .map(interval -> between(interval.start(), interval.end()))
            .reduce(Duration.ZERO, Duration::plus);
    }

    private record Interval(ZonedDateTime start, ZonedDateTime end) {
    }
}
