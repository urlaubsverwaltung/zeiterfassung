package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenantuser.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;

public record User(UserId id, UserLocalId localId, String givenName, String familyName, EMailAddress email) {
}
