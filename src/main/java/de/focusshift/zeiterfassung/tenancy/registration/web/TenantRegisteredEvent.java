package de.focusshift.zeiterfassung.tenancy.registration.web;

import de.focusshift.zeiterfassung.tenancy.tenant.Tenant;

public record TenantRegisteredEvent(Tenant tenant, String oidcClientSecret) {
}
