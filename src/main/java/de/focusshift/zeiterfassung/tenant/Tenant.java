package de.focusshift.zeiterfassung.tenant;

import java.time.Instant;

public record Tenant(String tenantId, Instant createdAt, Instant updatedAt, TenantStatus status) {
}
