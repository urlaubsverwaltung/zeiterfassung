package de.focusshift.zeiterfassung.tenantuser;

import de.focusshift.zeiterfassung.security.SecurityRoles;

import java.util.Set;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail, Set<SecurityRoles> authorities) {
}
