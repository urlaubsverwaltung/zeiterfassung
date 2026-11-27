package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import static de.focusshift.zeiterfassung.workduration.StatutoryBreakRule.NINE_HOURS;
import static de.focusshift.zeiterfassung.workduration.StatutoryBreakRule.SIX_HOURS;

/**
 * Calculates the part of the mandatory break (ArbZG §4) that has not been taken as a qualifying break.
 *
 * <p>
 * The deduction turns working time into break time, so the remaining working time is what the mandatory break depends
 * on. The deduction is the smallest duration {@code d} with
 * {@code qualifying breaks + d ≥ mandatory break(work duration − d)}.
 *
 * <p>
 * Close above a threshold the deduction is therefore capped at the threshold: 6h15 without a break are credited 6h,
 * 9h22 without a break are credited 8h52. Working longer never results in less credited time.
 *
 * <p>
 * The mandatory break is described in {@link StatutoryBreakRule}, which breaks qualify in {@link QualifyingBreaks}.
 */
@Component
public class MandatoryBreakDeductionCalculator {

    private final StatutoryBreakRule statutoryBreakRule = new StatutoryBreakRule();

    /**
     * @param workDuration  work duration of the day, before the deduction
     * @param entriesForDay time entries of the day, work and break entries
     * @return the duration to deduct from the work duration, never negative and never more than the work duration
     */
    public Duration calculateDeduction(WorkDuration workDuration, Collection<TimeEntry> entriesForDay) {

        final Duration work = workDuration.duration();
        final Duration qualifyingBreaks = QualifyingBreaks.of(entriesForDay).totalDuration();

        Duration deduction = statutoryBreakRule.calculate(work).minus(qualifyingBreaks);
        if (!deduction.isPositive()) {
            return Duration.ZERO;
        }

        // alternatively deduct just enough to fall back to a threshold, the mandatory break below it must be met nevertheless
        for (Duration threshold : List.of(NINE_HOURS, SIX_HOURS)) {
            if (work.compareTo(threshold) > 0) {
                final Duration toThreshold = work.minus(threshold);
                final Duration missingBelowThreshold = statutoryBreakRule.calculate(threshold).minus(qualifyingBreaks);
                deduction = min(deduction, max(toThreshold, missingBelowThreshold));
            }
        }

        return deduction;
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }
}
