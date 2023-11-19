package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.util.Set;

public record User(UserIdComposite idComposite, String givenName, String familyName, EMailAddress email, Set<SecurityRole> authorities) {

    public UserId id() {
        return idComposite.id();
    }

    public UserLocalId localId() {
        return idComposite.localId();
    }

    public String fullName() {
        return givenName + " " + familyName;
    }

    public boolean hasAuthority(SecurityRole authority) {
        return authorities().contains(authority);
    }
}
