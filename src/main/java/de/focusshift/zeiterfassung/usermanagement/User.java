package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import de.focusshift.zeiterfassung.tenantuser.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;

import java.util.Set;

public record User(UserId id, UserLocalId localId, String givenName, String familyName, EMailAddress email, Set<SecurityRoles> authorities) {
}
