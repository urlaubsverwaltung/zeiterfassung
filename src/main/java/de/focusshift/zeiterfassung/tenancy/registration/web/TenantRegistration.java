package de.focusshift.zeiterfassung.tenancy.registration.web;

public record TenantRegistration(
    String tenantId,
    String oidcClientSecret
) {

}
