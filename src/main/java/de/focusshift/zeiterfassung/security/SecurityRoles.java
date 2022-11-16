package de.focusshift.zeiterfassung.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum SecurityRoles {

    ZEITERFASSUNG_OPERATOR, // used by fss employees only!
    ZEITERFASSUNG_USER,
    ZEITERFASSUNG_VIEW_REPORT_ALL;

    private GrantedAuthority authority;

    public GrantedAuthority authority() {
        if (authority == null) {
            authority = new SimpleGrantedAuthority("ROLE_" + this.name());
        }
        return authority;
    }
}
