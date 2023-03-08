package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;

import java.util.Set;

public record User(UserId id, UserLocalId localId, String givenName, String familyName, EMailAddress email, Set<SecurityRole> authorities) {

    public String fullName() {
        return givenName + " " + familyName;
    }

    public boolean hasAuthority(SecurityRole authority) {
        return authorities().contains(authority);
    }
}
