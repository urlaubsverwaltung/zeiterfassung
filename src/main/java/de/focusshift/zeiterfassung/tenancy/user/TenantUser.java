package de.focusshift.zeiterfassung.tenancy.user;

import de.focusshift.zeiterfassung.security.SecurityRoles;

import java.time.Instant;
import java.util.Set;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Instant firstLoginAt, Set<SecurityRoles> authorities) {

    public TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Set<SecurityRoles> authorities) {
        this(id, localId, givenName, familyName, eMail, null, authorities);
    }
}
