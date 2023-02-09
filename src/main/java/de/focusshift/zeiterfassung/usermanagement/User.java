package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;

import java.util.Set;

public record User(UserId id, UserLocalId localId, String givenName, String familyName, EMailAddress email, Set<SecurityRoles> authorities) {

    public String fullName() {
        return givenName + " " + familyName;
    }

    public boolean hasAuthority(SecurityRoles authority) {
        return authorities().contains(authority);
    }
}
