package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;

public record User(UserId id, UserLocalId localId, String givenName, String familyName, EMailAddress email) {

    public String fullName() {
        return givenName + " " + familyName;
    }
}
