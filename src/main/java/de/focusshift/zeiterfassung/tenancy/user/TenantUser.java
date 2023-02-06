package de.focusshift.zeiterfassung.tenancy.user;

import java.time.Instant;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Instant firstLoginAt) {

    public TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail) {
        this(id, localId, givenName, familyName, eMail, null);
    }
}
