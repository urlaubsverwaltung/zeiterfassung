package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Comparator.comparing;

/**
 * Breaks of a day that count as rest breaks according to ArbZG §4 Satz 2.
 *
 * <p>
 * Only recorded break entries count, gaps between time entries do not. Overlapping or adjacent break entries are merged
 * first, afterwards every resulting break must last at least 15 minutes to count.
 */
final class QualifyingBreaks {

    static final Duration MIN_DURATION = Duration.ofMinutes(15);

    private final List<Interval> intervals;

    private QualifyingBreaks(List<Interval> intervals) {
        this.intervals = intervals;
    }

    static QualifyingBreaks of(Collection<TimeEntry> timeEntries) {
        final List<Interval> breakIntervals = timeEntries.stream()
            .distinct()
            .filter(TimeEntry::isBreak)
            .map(entry -> new Interval(entry.start(), entry.end()))
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
     * @return qualifying breaks sorted by start, not overlapping each other
     */
    List<Interval> intervals() {
        return intervals;
    }

    private static List<Interval> merge(List<Interval> sortedIntervals) {
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
    }
}
