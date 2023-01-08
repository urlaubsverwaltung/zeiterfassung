package de.focusshift.zeiterfassung.tenancy.tenant;

import java.time.Instant;

public record Tenant(String tenantId, Instant createdAt, Instant updatedAt, TenantStatus status) {
}
