package de.focusshift.zeiterfassung.workduration;

/**
 * Classifies a break-law violation detected by {@link BreakViolationChecker}.
 */
public enum BreakViolationType {
    /** Recorded break time is below the statutory minimum (ArbZG §4 Satz 1). */
    DAILY,
    /** An uninterrupted work block exceeds six hours (ArbZG §4 Satz 3). */
    CONTINUITY
}
