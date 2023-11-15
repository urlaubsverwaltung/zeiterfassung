package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;

/**
 * Defines a {@linkplain Duration} of break time. Different to {@linkplain WorkDuration}.
 */
public record BreakDuration(Duration duration) implements ZeitDuration {

}
