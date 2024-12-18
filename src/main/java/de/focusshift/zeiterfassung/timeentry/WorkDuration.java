package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;

/**
 * Defines a {@linkplain Duration} of worked time. Different to {@linkplain BreakDuration}.
 *
 * @param duration the exact duration. not rounded up to minutes.
 */
public record WorkDuration(Duration duration) implements ZeitDuration {

    public static final WorkDuration ZERO = new WorkDuration(Duration.ZERO);
    public static final WorkDuration EIGHT = new WorkDuration(Duration.ofHours(8));

    /**
     * Returns a copy of this workDuration with the specified workDuration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param workDuration  the workDuration to add, not null
     * @return a {@code WorkDuration} based on this workDuration with the specified workDuration added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public WorkDuration plus(WorkDuration workDuration) {
        return new WorkDuration(duration().plus(workDuration.duration()));
    }
}
