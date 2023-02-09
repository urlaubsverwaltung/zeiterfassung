package de.focusshift.zeiterfassung.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

public enum SecurityRoles {

    ZEITERFASSUNG_OPERATOR, // used by fss employees only!
    ZEITERFASSUNG_USER,
    ZEITERFASSUNG_VIEW_REPORT_ALL,
    ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;

    private GrantedAuthority authority;

    public GrantedAuthority authority() {
        if (authority == null) {
            authority = new SimpleGrantedAuthority("ROLE_" + this.name());
        }
        return authority;
    }

    public static Optional<SecurityRoles> fromAuthority(GrantedAuthority authority) {
        try {
            final SecurityRoles role = SecurityRoles.valueOf(authority.getAuthority().substring("ROLE_".length()));
            return Optional.of(role);
        } catch(IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
