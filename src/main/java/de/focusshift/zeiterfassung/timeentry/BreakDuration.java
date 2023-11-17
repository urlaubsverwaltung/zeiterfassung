package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;

/**
 * Defines a {@linkplain Duration} of break time. Different to {@linkplain WorkDuration}.
 *
 * @param duration the exact duration. not rounded up to minutes.
 */
public record BreakDuration(Duration duration) implements ZeitDuration {

    public static final BreakDuration ZERO = new BreakDuration(Duration.ZERO);

    /**
     * Returns a copy of this breakDuration with the specified breakDuration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param breakDuration  the breakDuration to add, not null
     * @return a {@code BreakDuration} based on this breakDuration with the specified breakDuration added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public BreakDuration plus(BreakDuration breakDuration) {
        return new BreakDuration(duration().plus(breakDuration.duration()));
    }
}
