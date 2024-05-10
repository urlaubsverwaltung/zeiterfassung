package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import jakarta.annotation.Nullable;

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
 * @param absenceTypeCategory
 * @param absenceTypeSourceId absence type source id or {@code null} for {@linkplain AbsenceTypeCategory#SICK}
 */
public record AbsenceWrite(
    TenantId tenantId,
    Long sourceId,
    UserId userId,
    Instant startDate,
    Instant endDate,
    DayLength dayLength,
    AbsenceTypeCategory absenceTypeCategory,
    @Nullable AbsenceTypeSourceId absenceTypeSourceId
) {

    public AbsenceWrite(TenantId tenantId, Long sourceId, UserId userId, Instant startDate, Instant endDate, DayLength dayLength, AbsenceTypeCategory absenceTypeCategory) {
        this(tenantId, sourceId, userId, startDate, endDate, dayLength, absenceTypeCategory, null);
    }
}
