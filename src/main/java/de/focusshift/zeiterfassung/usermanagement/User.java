package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.HasUserIdComposite;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;

public record User(
    UserIdComposite userIdComposite,
    String givenName,
    String familyName,
    EMailAddress email,
    Set<SecurityRole> authorities
) implements HasUserIdComposite {

    public String fullName() {
        return (givenName.strip() + " " + familyName.strip()).strip();
    }

    public boolean hasRole(SecurityRole role) {
        return authorities.contains(role);
    }

    public String initials() {
        return generateInitials(fullName());
    }

    public static String generateInitials(final String name) {

        final String nameTrimmed = name.strip();
        if (nameTrimmed.isBlank()) {
            return "??";
        }

        final int idxLastWhitespace = nameTrimmed.lastIndexOf(' ');
        if (idxLastWhitespace == -1) {
            return nameTrimmed.substring(0, 1).toUpperCase();
        }

        return (nameTrimmed.charAt(0) + nameTrimmed.substring(idxLastWhitespace + 1, idxLastWhitespace + 2)).toUpperCase();
    }

    @Override
    public String toString() {
        return "User{" +
            "userIdComposite=" + userIdComposite +
            '}';
    }

    public List<GrantedAuthority> grantedAuthorities() {
        return authorities.stream()
            .map(SecurityRole::authority)
            .toList();
    }
}
