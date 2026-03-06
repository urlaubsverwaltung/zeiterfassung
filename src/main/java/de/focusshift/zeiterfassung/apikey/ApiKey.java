package de.focusshift.zeiterfassung.apikey;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.Instant;
import java.util.List;

public record ApiKey(
    Long id,
    TenantId tenantId,
    UserLocalId userId,
    String label,
    Instant createdAt,
    Instant lastUsedAt,
    Instant expiresAt,
    boolean active
) {
}
