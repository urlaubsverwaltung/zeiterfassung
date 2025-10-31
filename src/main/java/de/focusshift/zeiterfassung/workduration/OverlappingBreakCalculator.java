package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static java.time.Duration.between;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;

/**
 * Specific strategy to calculate {@link WorkDuration}.
 *
 * <p>
 * Calculates the {@link WorkDuration} for the given list of {@link TimeEntry} while handling overlapping break
 * entries.
 *
 * <p>
 * Examples:
 *
 * <ul>
 *   <li>worked from 08:00 to 10:00 with break from 09:00 to 10:00 --> 1 hour WorkDuration</li>
 * </ul>
 */
@Component
class OverlappingBreakCalculator implements WorkDurationCalculator {

    @Override
    public WorkDuration calculateWorkDuration(Collection<TimeEntry> timeEntries) {

        final List<Interval> notMergedWorkIntervals = workIntervals(timeEntries);
        final List<Interval> mergedBreakIntervals = mergeIntervals(breakIntervals(timeEntries));
        final List<Interval> notMergedBreakIntervalWithWorkIntervalOverlaps = breakIntervalOverlaps(notMergedWorkIntervals, mergedBreakIntervals);

        final Duration totalWork = summarizeDuration(notMergedWorkIntervals);
        final Duration totalBreak = summarizeDuration(notMergedBreakIntervalWithWorkIntervalOverlaps);

        return new WorkDuration(totalWork.minus(totalBreak));
    }

    private List<Interval> workIntervals(Collection<TimeEntry> timeEntries) {
        return intervals(timeEntries, not(TimeEntry::isBreak));
    }

    private List<Interval> breakIntervals(Collection<TimeEntry> timeEntries) {
        return intervals(timeEntries, TimeEntry::isBreak);
    }

    private List<Interval> intervals(Collection<TimeEntry> timeEntries, Predicate<TimeEntry> filter) {
        return timeEntries.stream()
            .distinct()
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
     * @param workIntervals non overlapping work intervals
     * @param mergedBreakIntervals non overlapping break intervals
     * @return intervals of overlapping break and work, sorted by start
     */
    private List<Interval> breakIntervalOverlaps(Collection<Interval> workIntervals, Collection<Interval> mergedBreakIntervals) {
        return workIntervals.stream()
            .flatMap(workInterval -> mergedBreakIntervals.stream()
                .map(breakInterval -> {
                    final ZonedDateTime start = workInterval.start().isAfter(breakInterval.start()) ? workInterval.start() : breakInterval.start();
                    final ZonedDateTime end = workInterval.end().isBefore(breakInterval.end()) ? workInterval.end() : breakInterval.end();
                    final Interval overlapInterval = new Interval(start, end);
                    return end.isAfter(start) ? overlapInterval : null;
                })
                .filter(Objects::nonNull)
            )
            .sorted(comparing(Interval::start))
            .toList();
    }

    private static List<Interval> mergeIntervals(Collection<Interval> intervals) {

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

    private Duration summarizeDuration(Collection<Interval> intervals) {
        return intervals.stream()
            .map(interval -> between(interval.start(), interval.end()))
            .reduce(Duration.ZERO, Duration::plus);
    }

    private record Interval(ZonedDateTime start, ZonedDateTime end) {
    }
}
