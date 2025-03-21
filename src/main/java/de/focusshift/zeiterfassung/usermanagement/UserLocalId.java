package de.focusshift.zeiterfassung.usermanagement;

import static org.springframework.util.Assert.notNull;

/**
 * Value identifying a user of the zeiterfassung application. This value is only unique for one exact tenant.
 */
public record UserLocalId(Long value) {

    public UserLocalId {
        notNull(value, "expected userLocalId value not to be null.");
    }
}
