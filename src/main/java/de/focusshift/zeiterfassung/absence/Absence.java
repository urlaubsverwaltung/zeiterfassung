package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Describes an absence like holiday or sick.
 *
 * @param userId
 * @param startDate
 * @param endDate
 * @param dayLength
 * @param label
 * @param color
 */
public record Absence(
    UserId userId,
    ZonedDateTime startDate,
    ZonedDateTime endDate,
    DayLength dayLength,
    Function<Locale, String> label,
    AbsenceColor color
) {

    public String label(Locale locale) {
        return label.apply(locale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Absence absence = (Absence) o;
        return Objects.equals(userId, absence.userId)
            && Objects.equals(color, absence.color)
            && dayLength == absence.dayLength
            && Objects.equals(endDate, absence.endDate)
            && Objects.equals(startDate, absence.startDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, startDate, endDate, dayLength, color);
    }
}
