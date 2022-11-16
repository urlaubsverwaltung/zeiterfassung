package de.focusshift.zeiterfassung.registration.tenant;

public record TenantRegistration(
    String tenantId,
    String oidcClientSecret
) {

}
