package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.Instant;
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
 * @param absenceTypeCategory
 */
public record Absence(
    UserId userId,
    Instant startDate,
    Instant endDate,
    DayLength dayLength,
    Function<Locale, String> label,
    AbsenceColor color,
    AbsenceTypeCategory absenceTypeCategory
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
            && color == absence.color
            && dayLength == absence.dayLength
            && Objects.equals(endDate, absence.endDate)
            && Objects.equals(startDate, absence.startDate)
            && Objects.equals(absenceTypeCategory, absence.absenceTypeCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, startDate, endDate, dayLength, color, absenceTypeCategory);
    }
}
