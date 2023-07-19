package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;

import java.time.Instant;

/**
 * Describes the write model of an absence like holiday or sick.
 *
 * @param tenantId
 * @param sourceId
 * @param userId
 * @param startDate
 * @param endDate
 * @param dayLength
 * @param type
 * @param color selected by the user to render the absence type
 */
public record AbsenceWrite(
    TenantId tenantId,
    Long sourceId,
    UserId userId,
    Instant startDate,
    Instant endDate,
    DayLength dayLength,
    AbsenceType type,
    AbsenceColor color
) {
}
