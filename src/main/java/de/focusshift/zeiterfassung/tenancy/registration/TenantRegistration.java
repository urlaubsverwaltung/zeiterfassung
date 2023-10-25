package de.focusshift.zeiterfassung.tenancy.registration;

public record TenantRegistration(String tenantId, String oidcClientSecret) {
}
