package de.focusshift.zeiterfassung.usermanagement;

/**
 * Value identifying a user of the zeiterfassung application. This value is only unique for one exact tenant.
 */
public record UserLocalId(Long value) {
}
