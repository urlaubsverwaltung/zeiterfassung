package de.focusshift.zeiterfassung.importer.model;

import java.time.Instant;
import java.util.List;

public record TenantExport(String tenantId, Instant exportedAt, List<UserExport> users) {
}
