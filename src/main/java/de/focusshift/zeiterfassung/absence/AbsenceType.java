package de.focusshift.zeiterfassung.absence;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Describes the absence-type of an {@linkplain Absence}. Absence types are not managed by Zeiterfassung, but by an
 * external system.
 *
 * <p>
 * Therefore, existing labels are unknown. There should be at least one, actually, but you have to handle {@code null}
 * values yourself. Or {@linkplain Locale locales} that are not existing in {@linkplain #label}.
 *
 * @param category {@linkplain AbsenceTypeCategory} of this {@linkplain AbsenceType}
 * @param sourceId external id of this {@linkplain AbsenceType}
 * @param label label supplier of this {@linkplain AbsenceType} for a given {@linkplain Locale}, may return {@code null}
 */
public record AbsenceType(
    AbsenceTypeCategory category,
    Long sourceId,
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
        AbsenceType that = (AbsenceType) o;
        return Objects.equals(sourceId, that.sourceId) && color == that.color && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, sourceId, color);
    }
}
