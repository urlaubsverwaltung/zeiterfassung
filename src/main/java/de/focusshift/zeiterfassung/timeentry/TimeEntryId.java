package de.focusshift.zeiterfassung.timeentry;

import static org.springframework.util.Assert.notNull;

/**
 * {@link TimeEntry} identifier
 *
 * @param value
 */
public record TimeEntryId(Long value) {

    public TimeEntryId {
        notNull(value, "expected value not to be null.");
    }
}
