package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BreakViolationChecker {

    private static final StatutoryBreakRule STATUTORY_BREAK_RULE = new StatutoryBreakRule();
    private static final Duration MIN_QUALIFYING_BREAK = Duration.ofMinutes(15);
    private static final Duration MAX_CONTINUOUS_WORK = Duration.ofHours(6);

    public List<BreakViolation> check(List<TimeEntry> entriesForDay) {
        if (entriesForDay.isEmpty()) {
            return List.of();
        }

        List<BreakViolation> violations = new ArrayList<>();

        if (hasDailyViolation(entriesForDay)) {
            violations.add(new BreakViolation(BreakViolationType.DAILY));
        }

        if (hasContinuityViolation(entriesForDay)) {
            violations.add(new BreakViolation(BreakViolationType.CONTINUITY));
        }

        return List.copyOf(violations);
    }

    private boolean hasDailyViolation(List<TimeEntry> entries) {
        Duration grossWorkingTime = entries.stream()
            .map(TimeEntry::duration)
            .reduce(Duration.ZERO, Duration::plus);

        Duration requiredBreak = STATUTORY_BREAK_RULE.calculate(grossWorkingTime);
        if (requiredBreak.isZero()) {
            return false;
        }

        Duration actualBreak = entries.stream()
            .filter(TimeEntry::isBreak)
            .map(TimeEntry::duration)
            .reduce(Duration.ZERO, Duration::plus);

        return actualBreak.compareTo(requiredBreak) < 0;
    }

    private boolean hasContinuityViolation(List<TimeEntry> entries) {
        List<TimeEntry> sorted = entries.stream()
            .sorted(Comparator.comparing(TimeEntry::start))
            .toList();

        Duration currentBlock = Duration.ZERO;

        for (TimeEntry entry : sorted) {
            if (entry.isBreak() && entry.duration().compareTo(MIN_QUALIFYING_BREAK) >= 0) {
                currentBlock = Duration.ZERO;
            } else if (!entry.isBreak()) {
                currentBlock = currentBlock.plus(entry.duration());
                if (currentBlock.compareTo(MAX_CONTINUOUS_WORK) > 0) {
                    return true;
                }
            }
            // short breaks (< 15min) and gaps between entries do not interrupt the block
        }

        return false;
    }
}
