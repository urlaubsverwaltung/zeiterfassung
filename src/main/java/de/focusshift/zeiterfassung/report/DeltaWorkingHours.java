package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;

/**
 * Describes the difference between {@linkplain WorkDuration} and {@linkplain ShouldWorkingHours}.
 *
 * <p>
 * Example: {@code WorkDuration(7h) - ShouldWorkingHours(8h) = DeltaWorkingHours(-1h)}
 *
 * @param duration duration value of the delta
 */
record DeltaWorkingHours(Duration duration) implements ZeitDuration {

    public static final DeltaWorkingHours ZERO = new DeltaWorkingHours(Duration.ZERO);
    public static final DeltaWorkingHours EIGHT_POSITIVE = new DeltaWorkingHours(Duration.ofHours(8));
    public static final DeltaWorkingHours EIGHT_NEGATIVE = new DeltaWorkingHours(Duration.ofHours(8).negated());

    public boolean isNegative() {
        return duration.isNegative();
    }

    /**
     * Returns a {@linkplain DeltaWorkingHours} whose value is {@code (this + augend)}.
     *
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param augend value to add, not null
     * @return a {@linkplain DeltaWorkingHours} whose value is {@code (this + augend)}
     * @throws ArithmeticException if numeric overflow occurs
     */
    public DeltaWorkingHours plus(DeltaWorkingHours augend) {
        return new DeltaWorkingHours(duration().plus(augend.duration()));
    }
}
