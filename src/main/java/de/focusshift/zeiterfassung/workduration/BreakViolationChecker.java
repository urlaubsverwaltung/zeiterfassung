package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.workduration.QualifyingBreaks.Interval;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;

/**
 * Checks the time entries of a day against the break rules of ArbZG §4.
 *
 * <ul>
 *     <li>{@link BreakViolationType#DAILY}: qualifying breaks are shorter than the mandatory break,
 *     see {@link QualifyingBreaks} for which breaks count</li>
 *     <li>{@link BreakViolationType#CONTINUITY}: more than six hours of work without an interruption of at least
 *     15 minutes. The interruption can be a recorded break or a gap between time entries.</li>
 * </ul>
 */
@Component
public class BreakViolationChecker {

    private static final Duration MAX_CONTINUOUS_WORK = Duration.ofHours(6);
    private static final Duration MIN_INTERRUPTION = QualifyingBreaks.MIN_DURATION;

    private final StatutoryBreakRule statutoryBreakRule = new StatutoryBreakRule();

    /**
     * @param workDuration  work duration of the day, before any automatic break deduction
     * @param entriesForDay time entries of the day, work and break entries
     * @return violated rules, empty when there is none
     */
    public List<BreakViolation> check(WorkDuration workDuration, Collection<TimeEntry> entriesForDay) {

        final List<BreakViolation> violations = new ArrayList<>();

        if (hasDailyViolation(workDuration, entriesForDay)) {
            violations.add(new BreakViolation(BreakViolationType.DAILY));
        }

        if (hasContinuityViolation(entriesForDay)) {
            violations.add(new BreakViolation(BreakViolationType.CONTINUITY));
        }

        return List.copyOf(violations);
    }

    private boolean hasDailyViolation(WorkDuration workDuration, Collection<TimeEntry> entries) {
        final Duration mandatoryBreak = statutoryBreakRule.calculate(workDuration.duration());
        return QualifyingBreaks.of(entries).totalDuration().compareTo(mandatoryBreak) < 0;
    }

    private static boolean hasContinuityViolation(Collection<TimeEntry> entries) {

        Duration block = Duration.ZERO;
        ZonedDateTime previousEnd = null;

        for (Interval work : workWithoutBreaks(entries)) {
            final boolean interrupted = previousEnd != null
                && Duration.between(previousEnd, work.start()).compareTo(MIN_INTERRUPTION) >= 0;

            block = interrupted ? work.duration() : block.plus(work.duration());
            if (block.compareTo(MAX_CONTINUOUS_WORK) > 0) {
                return true;
            }
            previousEnd = work.end();
        }

        return false;
    }

    /**
     * @return the time actually worked: work entries without the time covered by break entries, merged and sorted
     */
    private static List<Interval> workWithoutBreaks(Collection<TimeEntry> entries) {

        final List<Interval> breaks = QualifyingBreaks.merge(intervals(entries, true));

        final List<Interval> work = new ArrayList<>();
        for (Interval workInterval : intervals(entries, false)) {
            ZonedDateTime start = workInterval.start();
            for (Interval breakInterval : breaks) {
                if (!breakInterval.end().isAfter(start) || !breakInterval.start().isBefore(workInterval.end())) {
                    continue;
                }
                if (breakInterval.start().isAfter(start)) {
                    work.add(new Interval(start, breakInterval.start()));
                }
                start = breakInterval.end();
            }
            if (start.isBefore(workInterval.end())) {
                work.add(new Interval(start, workInterval.end()));
            }
        }

        return QualifyingBreaks.merge(work.stream().sorted(comparing(Interval::start)).toList());
    }

    private static List<Interval> intervals(Collection<TimeEntry> entries, boolean breaks) {
        return entries.stream()
            .distinct()
            .filter(breaks ? TimeEntry::isBreak : not(TimeEntry::isBreak))
            .map(entry -> new Interval(entry.start(), entry.end()))
            .sorted(comparing(Interval::start))
            .toList();
    }
}
