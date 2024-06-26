package de.focusshift.zeiterfassung.tenancy.user;

/**
 * This enum represents the status of a user set by an external system (e.g. Keycloak).
 *
 * <p>
 * A deactivated user can no longer log in to Keycloak / our application.
 *
 */
public enum UserStatus {
    ACTIVE,
    DEACTIVATED,
    DELETED,

    /**
     * All users who existed before the introduction of this Enum have the status UNKNOWN.
     * The status will be set to {@linkplain #ACTIVE} on the next login or when a
     * {@linkplain de.focus_shift.urlaubsverwaltung.extension.api.person.PersonUpdatedEventDTO PersonUpdated Message} is received.
     */
    UNKNOWN
}
