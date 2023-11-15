package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;

/**
 * Defines a {@linkplain Duration} of worked time. Different to {@linkplain BreakDuration}.
 */
public record WorkDuration(Duration duration) implements ZeitDuration {

    public static final WorkDuration ZERO = new WorkDuration(Duration.ZERO);
}
