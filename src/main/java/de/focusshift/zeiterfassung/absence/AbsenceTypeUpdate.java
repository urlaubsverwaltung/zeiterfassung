package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;

import java.util.Locale;
import java.util.Map;

public record AbsenceTypeUpdate(
    TenantId tenantId,
    Long sourceId,
    AbsenceTypeCategory category,
    AbsenceColor color,
    Map<Locale, String> labelByLocale
) {
}
