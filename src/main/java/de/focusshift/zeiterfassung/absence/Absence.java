package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;

/**
 * Describes an absence like holiday or sick.
 *
 * @param userId
 * @param startDate
 * @param endDate
 * @param dayLength
 * @param type
 * @param color     selected by the user to render the absence type
 */
public record Absence(
    UserId userId,
    ZonedDateTime startDate,
    ZonedDateTime endDate,
    DayLength dayLength,
    AbsenceType type,
    AbsenceColor color
) {

    public String getMessageKey() {
        if(this.type.sourceId() == null) {
            return "absence.%s.%s".formatted(type.category(), dayLength.name());
        } else {
            return "absence.%s.%s.%s".formatted(type.category(), this.type.sourceId(), dayLength.name());
        }
    }
}
