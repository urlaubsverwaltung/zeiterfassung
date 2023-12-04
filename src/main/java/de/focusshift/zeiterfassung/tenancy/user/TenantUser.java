package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRole;

import java.time.Instant;
import java.util.Set;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Instant firstLoginAt, Set<SecurityRole> authorities) {

    public TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Set<SecurityRole> authorities) {
        this(id, localId, givenName, familyName, eMail, null, authorities);
    }

    @Override
    public String toString() {
        return "TenantUser{" +
            "id='" + id + '\'' +
            ", localId=" + localId +
            '}';
    }
}
