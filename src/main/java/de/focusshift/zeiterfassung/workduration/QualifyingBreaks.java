package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.function.Predicate.not;

/**
 * Breaks of a day that count as rest breaks according to ArbZG §4.
 *
 * <ul>
 *     <li>Recorded break entries and gaps between time entries count, nobody works during a gap.</li>
 *     <li>A rest break interrupts the work, so only the part between the start of the first and the end of the last
 *     work entry counts.</li>
 *     <li>Overlapping or adjacent breaks and gaps are merged, afterwards every interruption must last at least
 *     15 minutes (ArbZG §4 Satz 2).</li>
 * </ul>
 */
final class QualifyingBreaks {

    static final Duration MIN_DURATION = Duration.ofMinutes(15);

    private final List<Interval> intervals;

    private QualifyingBreaks(List<Interval> intervals) {
        this.intervals = intervals;
    }

    static QualifyingBreaks of(Collection<TimeEntry> timeEntries) {

        final Optional<ZonedDateTime> workStart = timeEntries.stream().filter(not(TimeEntry::isBreak)).map(TimeEntry::start).min(naturalOrder());
        final Optional<ZonedDateTime> workEnd = timeEntries.stream().filter(not(TimeEntry::isBreak)).map(TimeEntry::end).max(naturalOrder());
        if (workStart.isEmpty()) {
            return new QualifyingBreaks(List.of());
        }

        final List<Interval> breakIntervals = clippedIntervals(timeEntries.stream().filter(TimeEntry::isBreak).toList(), workStart.get(), workEnd.get());
        final List<Interval> gaps = gaps(clippedIntervals(timeEntries, workStart.get(), workEnd.get()), workStart.get());

        final List<Interval> interruptions = Stream.concat(breakIntervals.stream(), gaps.stream())
            .sorted(comparing(Interval::start))
            .toList();

        final List<Interval> qualifying = merge(interruptions).stream()
            .filter(interval -> interval.duration().compareTo(MIN_DURATION) >= 0)
            .toList();

        return new QualifyingBreaks(qualifying);
    }

    private static List<Interval> clippedIntervals(Collection<TimeEntry> timeEntries, ZonedDateTime from, ZonedDateTime to) {
        return timeEntries.stream()
            .distinct()
            .map(entry -> new Interval(entry.start(), entry.end()).clip(from, to))
            .flatMap(Optional::stream)
            .sorted(comparing(Interval::start))
            .toList();
    }

    /**
     * @param sortedIntervals intervals of all time entries within the work, sorted by start
     * @param workStart       start of the first work entry
     * @return the time between the given intervals that is covered by no time entry
     */
    private static List<Interval> gaps(List<Interval> sortedIntervals, ZonedDateTime workStart) {
        final List<Interval> gaps = new ArrayList<>();
        ZonedDateTime coveredUntil = workStart;
        for (Interval covered : merge(sortedIntervals)) {
            if (covered.start().isAfter(coveredUntil)) {
                gaps.add(new Interval(coveredUntil, covered.start()));
            }
            coveredUntil = covered.end();
        }
        return gaps;
    }

    Duration totalDuration() {
        return intervals.stream()
            .map(Interval::duration)
            .reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * Merges overlapping or adjacent intervals.
     *
     * @param sortedIntervals intervals sorted by start
     * @return merged intervals sorted by start
     */
    static List<Interval> merge(List<Interval> sortedIntervals) {
        final List<Interval> merged = new ArrayList<>();
        for (Interval interval : sortedIntervals) {
            if (merged.isEmpty() || merged.getLast().end().isBefore(interval.start())) {
                merged.add(interval);
            } else {
                final Interval last = merged.removeLast();
                final ZonedDateTime end = last.end().isAfter(interval.end()) ? last.end() : interval.end();
                merged.add(new Interval(last.start(), end));
            }
        }
        return merged;
    }

    record Interval(ZonedDateTime start, ZonedDateTime end) {

        Duration duration() {
            return Duration.between(start, end);
        }

        /**
         * @return the part of this interval within the given bounds, empty when there is none
         */
        Optional<Interval> clip(ZonedDateTime from, ZonedDateTime to) {
            final ZonedDateTime clippedStart = start.isAfter(from) ? start : from;
            final ZonedDateTime clippedEnd = end.isBefore(to) ? end : to;
            return clippedStart.isBefore(clippedEnd) ? Optional.of(new Interval(clippedStart, clippedEnd)) : Optional.empty();
        }
    }
}
