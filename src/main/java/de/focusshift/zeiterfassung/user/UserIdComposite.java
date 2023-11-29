package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

/**
 * Composite of {@linkplain UserId} and {@linkplain UserLocalId}.
 *
 * @param id
 * @param localId
 */
public record UserIdComposite(UserId id, UserLocalId localId) {
}
