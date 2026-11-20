package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.function.Predicate.not;

/**
 * Breaks of a day that count as rest breaks according to ArbZG §4.
 *
 * <ul>
 *     <li>Only recorded break entries count, gaps between time entries do not.</li>
 *     <li>A rest break interrupts the work, so only the part between the start of the first and the end of the last
 *     work entry counts.</li>
 *     <li>Overlapping or adjacent break entries are merged, afterwards every break must last at least 15 minutes
 *     (ArbZG §4 Satz 2).</li>
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

        final List<Interval> breakIntervals = timeEntries.stream()
            .distinct()
            .filter(TimeEntry::isBreak)
            .map(entry -> new Interval(entry.start(), entry.end()).clip(workStart.get(), workEnd.get()))
            .flatMap(Optional::stream)
            .sorted(comparing(Interval::start))
            .toList();

        final List<Interval> qualifying = merge(breakIntervals).stream()
            .filter(interval -> interval.duration().compareTo(MIN_DURATION) >= 0)
            .toList();

        return new QualifyingBreaks(qualifying);
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
