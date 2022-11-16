package de.focusshift.zeiterfassung.registration.tenant;

import de.focusshift.zeiterfassung.tenant.Tenant;

public record TenantRegisteredEvent(Tenant tenant, String oidcClientSecret) {
}
