package de.focusshift.zeiterfassung.absence;

import static org.springframework.util.Assert.notNull;

/**
 * Global identifier of an {@linkplain AbsenceType} used by the platform.
 *
 * @param value
 */
public record AbsenceTypeSourceId(Long value) {

    public AbsenceTypeSourceId {
        notNull(value, "expected value not to be null.");
    }
}
