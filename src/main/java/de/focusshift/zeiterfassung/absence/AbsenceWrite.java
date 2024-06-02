package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;
import jakarta.annotation.Nullable;

import java.time.Instant;

/**
 * Describes the write model of an absence like holiday or sick.
 *
 * @param sourceId
 * @param userId
 * @param startDate
 * @param endDate
 * @param dayLength
 * @param absenceTypeCategory
 * @param absenceTypeSourceId absence type source id or {@code null} for {@linkplain AbsenceTypeCategory#SICK}
 */
public record AbsenceWrite(
    Long sourceId,
    UserId userId,
    Instant startDate,
    Instant endDate,
    DayLength dayLength,
    AbsenceTypeCategory absenceTypeCategory,
    @Nullable AbsenceTypeSourceId absenceTypeSourceId
) {

    /**
     * constructor for absences without a absenceType sourceId (e.g. {@linkplain AbsenceTypeCategory#SICK}).
     *
     * @param sourceId
     * @param userId
     * @param startDate
     * @param endDate
     * @param dayLength
     * @param absenceTypeCategory
     */
    public AbsenceWrite(Long sourceId, UserId userId, Instant startDate, Instant endDate, DayLength dayLength, AbsenceTypeCategory absenceTypeCategory) {
        this(sourceId, userId, startDate, endDate, dayLength, absenceTypeCategory, null);
    }
}
