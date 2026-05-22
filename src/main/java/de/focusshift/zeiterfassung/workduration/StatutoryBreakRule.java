package de.focusshift.zeiterfassung.workduration;

import java.time.Duration;

class StatutoryBreakRule {

    Duration calculate(Duration grossWorkingTime) {
        if (grossWorkingTime.compareTo(Duration.ofHours(9)) > 0) {
            return Duration.ofMinutes(45);
        }
        if (grossWorkingTime.compareTo(Duration.ofHours(6)) > 0) {
            return Duration.ofMinutes(30);
        }
        return Duration.ZERO;
    }
}
