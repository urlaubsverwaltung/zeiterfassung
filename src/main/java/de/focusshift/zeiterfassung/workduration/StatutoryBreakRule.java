package de.focusshift.zeiterfassung.workduration;

import java.time.Duration;

/**
 * Mandatory break according to ArbZG §4: 30 minutes for more than six hours of work, 45 minutes for more than nine.
 */
class StatutoryBreakRule {

    static final Duration SIX_HOURS = Duration.ofHours(6);
    static final Duration NINE_HOURS = Duration.ofHours(9);

    Duration calculate(Duration workingTime) {
        if (workingTime.compareTo(NINE_HOURS) > 0) {
            return Duration.ofMinutes(45);
        }
        if (workingTime.compareTo(SIX_HOURS) > 0) {
            return Duration.ofMinutes(30);
        }
        return Duration.ZERO;
    }
}
