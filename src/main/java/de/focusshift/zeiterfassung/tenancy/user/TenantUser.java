package de.focusshift.zeiterfassung.tenancy.user;

public record TenantUser(String id, Long localId, String givenName, String familyName, EMailAddress eMail) {
}
