package de.focusshift.zeiterfassung.workduration;

import java.util.Collection;
import java.util.Optional;

import static de.focusshift.zeiterfassung.workduration.BreakViolationType.CONTINUITY;
import static de.focusshift.zeiterfassung.workduration.BreakViolationType.DAILY;

/**
 * Message keys describing the violated break rules of a day.
 */
public final class BreakViolationHint {

    private BreakViolationHint() {
    }

    /**
     * @param violations violated break rules of a day
     * @return message key describing all given violations, empty when there is none
     */
    public static Optional<String> messageKey(Collection<BreakViolation> violations) {

        final boolean daily = violations.stream().anyMatch(violation -> violation.type() == DAILY);
        final boolean continuity = violations.stream().anyMatch(violation -> violation.type() == CONTINUITY);

        if (daily && continuity) {
            return Optional.of("break-violation.hint.daily-and-continuity");
        } else if (daily) {
            return Optional.of("break-violation.hint.daily");
        } else if (continuity) {
            return Optional.of("break-violation.hint.continuity");
        }
        return Optional.empty();
    }
}
