package de.focusshift.zeiterfassung.usermanagement;

/**
 * Value identifying a user of the zeiterfassung application.
 */
public record UserLocalId(Long value) {

    public static UserLocalId ofValue(String value) {
        return new UserLocalId(Long.valueOf(value));
    }
}
