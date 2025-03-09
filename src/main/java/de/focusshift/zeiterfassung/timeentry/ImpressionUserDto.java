package de.focusshift.zeiterfassung.timeentry;

/**
 * Represents a user the currently logged-in user is watching a page for. For instance the time-entries start page.
 *
 * @param localId {@link de.focusshift.zeiterfassung.usermanagement.UserLocalId} of the user impression
 * @param firstName first name
 * @param lastName last name
 * @param fullName full name
 * @param email email
 */
record ImpressionUserDto(long localId, String firstName, String lastName, String fullName, String email) {
}
