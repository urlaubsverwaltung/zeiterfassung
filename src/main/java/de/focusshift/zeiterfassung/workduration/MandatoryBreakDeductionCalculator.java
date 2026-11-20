package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;

/**
 * Calculates the part of the mandatory break (ArbZG §4) that has not been recorded as a qualifying break.
 *
 * <p>
 * {@code deduction = max(0, mandatory break − qualifying breaks)}
 *
 * <p>
 * The mandatory break depends on the work duration of the day, see {@link StatutoryBreakRule}. Which breaks qualify is
 * described in {@link QualifyingBreaks}.
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

        final Duration mandatoryBreak = statutoryBreakRule.calculate(workDuration.duration());
        if (mandatoryBreak.isZero()) {
            return Duration.ZERO;
        }

        final Duration missing = mandatoryBreak.minus(QualifyingBreaks.of(entriesForDay).totalDuration());
        return missing.isNegative() ? Duration.ZERO : missing;
    }
}
