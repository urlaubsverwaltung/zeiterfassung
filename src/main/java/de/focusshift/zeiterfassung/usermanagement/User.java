package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.HasUserIdComposite;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.util.Set;

public record User(
    UserIdComposite userIdComposite,
    String givenName,
    String familyName,
    EMailAddress email,
    Set<SecurityRole> authorities
) implements HasUserIdComposite {

    public String fullName() {
        return givenName + " " + familyName;
    }

    @Override
    public String toString() {
        return "User{" +
            "userIdComposite=" + userIdComposite +
            '}';
    }
}
