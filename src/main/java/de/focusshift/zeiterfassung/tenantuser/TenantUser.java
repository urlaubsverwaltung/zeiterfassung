package de.focusshift.zeiterfassung.tenantuser;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail) {
}
