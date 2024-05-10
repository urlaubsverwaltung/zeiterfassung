package de.focusshift.zeiterfassung.absence;

import java.util.Locale;
import java.util.Map;

/**
 * Describes the absence-type of an {@linkplain Absence}. Absence types are not managed by Zeiterfassung, but by an
 * external system.
 *
 * <p>
 * Therefore, existing labels are unknown. There should be at least one, actually, but you have to handle {@code null}
 * values yourself. Or {@linkplain Locale locales} that are not existing in {@linkplain #labelByLocale}.
 *
 * @param category {@linkplain AbsenceTypeCategory} of this {@linkplain AbsenceType}
 * @param sourceId external id of this {@linkplain AbsenceType}
 * @param labelByLocale label of this {@linkplain AbsenceType} for a given {@linkplain Locale}, may be {@code null}
 */
public record AbsenceType(
    AbsenceTypeCategory category,
    Long sourceId,
    Map<Locale, String> labelByLocale,
    AbsenceColor color
) {
}
