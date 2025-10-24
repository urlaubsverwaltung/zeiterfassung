package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.workduration.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;

/**
 * Describes the difference between {@linkplain WorkDuration} and {@linkplain ShouldWorkingHours}.
 * Positive and negative values are possible.
 * <p>
 * Example: {@code WorkDuration(7h) - ShouldWorkingHours(8h) = OvertimeHours(-1h)}
 *
 * @param duration duration value of the delta
 */
public record OvertimeHours(Duration duration) implements ZeitDuration {

    public static final OvertimeHours ZERO = new OvertimeHours(Duration.ZERO);
    public static final OvertimeHours EIGHT_POSITIVE = new OvertimeHours(Duration.ofHours(8));
    public static final OvertimeHours EIGHT_NEGATIVE = new OvertimeHours(Duration.ofHours(8).negated());

    public boolean isNegative() {
        return duration.isNegative();
    }

    /**
     * Returns a {@linkplain OvertimeHours} whose value is {@code (this + augend)}.
     *
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param augend value to add, not null
     * @return a {@linkplain OvertimeHours} whose value is {@code (this + augend)}
     * @throws ArithmeticException if numeric overflow occurs
     */
    public OvertimeHours plus(OvertimeHours augend) {
        return new OvertimeHours(duration().plus(augend.duration()));
    }
}
