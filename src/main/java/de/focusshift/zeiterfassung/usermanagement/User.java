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

    public String initials() {

        final String niceName = fullName();

        final int idxLastWhitespace = niceName.lastIndexOf(' ');
        if (idxLastWhitespace == -1) {
            return niceName.substring(0, 1).toUpperCase();
        }

        return (niceName.charAt(0) + niceName.substring(idxLastWhitespace + 1, idxLastWhitespace + 2)).toUpperCase();
    }

    @Override
    public String toString() {
        return "User{" +
            "userIdComposite=" + userIdComposite +
            '}';
    }
}
