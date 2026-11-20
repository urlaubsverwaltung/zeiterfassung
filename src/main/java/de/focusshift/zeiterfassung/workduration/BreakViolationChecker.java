package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.workduration.QualifyingBreaks.Interval;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.function.Predicate.not;

/**
 * Checks the time entries of a day against the break rules of ArbZG §4.
 *
 * <ul>
 *     <li>{@link BreakViolationType#DAILY}: qualifying breaks are shorter than the mandatory break</li>
 *     <li>{@link BreakViolationType#CONTINUITY}: more than six hours of work without a qualifying break</li>
 * </ul>
 *
 * See {@link QualifyingBreaks} for which breaks count.
 */
@Component
public class BreakViolationChecker {

    private static final Duration MAX_CONTINUOUS_WORK = Duration.ofHours(6);

    private final StatutoryBreakRule statutoryBreakRule = new StatutoryBreakRule();

    /**
     * @param workDuration  work duration of the day, before any automatic break deduction
     * @param entriesForDay time entries of the day, work and break entries
     * @return violated rules, empty when there is no violation
     */
    public List<BreakViolation> check(WorkDuration workDuration, Collection<TimeEntry> entriesForDay) {

        final QualifyingBreaks qualifyingBreaks = QualifyingBreaks.of(entriesForDay);
        final List<BreakViolation> violations = new ArrayList<>();

        if (hasDailyViolation(workDuration, qualifyingBreaks)) {
            violations.add(new BreakViolation(BreakViolationType.DAILY));
        }

        if (hasContinuityViolation(entriesForDay, qualifyingBreaks)) {
            violations.add(new BreakViolation(BreakViolationType.CONTINUITY));
        }

        return List.copyOf(violations);
    }

    private boolean hasDailyViolation(WorkDuration workDuration, QualifyingBreaks qualifyingBreaks) {
        final Duration mandatoryBreak = statutoryBreakRule.calculate(workDuration.duration());
        return qualifyingBreaks.totalDuration().compareTo(mandatoryBreak) < 0;
    }

    /**
     * Qualifying breaks split the day into blocks. Work within a block counts as continuous, even when there are gaps
     * between the time entries.
     */
    private static boolean hasContinuityViolation(Collection<TimeEntry> entries, QualifyingBreaks qualifyingBreaks) {

        final List<Interval> workIntervals = entries.stream()
            .distinct()
            .filter(not(TimeEntry::isBreak))
            .map(entry -> new Interval(entry.start(), entry.end()))
            .toList();

        ZonedDateTime blockStart = null;
        for (Interval qualifyingBreak : qualifyingBreaks.intervals()) {
            if (workWithin(workIntervals, blockStart, qualifyingBreak.start()).compareTo(MAX_CONTINUOUS_WORK) > 0) {
                return true;
            }
            blockStart = qualifyingBreak.end();
        }

        return workWithin(workIntervals, blockStart, null).compareTo(MAX_CONTINUOUS_WORK) > 0;
    }

    /**
     * @param from inclusive, {@code null} for unbounded
     * @param to   exclusive, {@code null} for unbounded
     */
    private static Duration workWithin(List<Interval> workIntervals, ZonedDateTime from, ZonedDateTime to) {
        Duration sum = Duration.ZERO;
        for (Interval work : workIntervals) {
            final ZonedDateTime start = from == null || work.start().isAfter(from) ? work.start() : from;
            final ZonedDateTime end = to == null || work.end().isBefore(to) ? work.end() : to;
            if (start.isBefore(end)) {
                sum = sum.plus(Duration.between(start, end));
            }
        }
        return sum;
    }
}
