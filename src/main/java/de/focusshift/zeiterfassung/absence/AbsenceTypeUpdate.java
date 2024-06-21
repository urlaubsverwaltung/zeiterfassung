package de.focusshift.zeiterfassung.absence;

import java.util.Locale;
import java.util.Map;

/**
 * Update {@linkplain AbsenceType} to the given values.
 *
 * @param sourceId external system source identifier of the absence type
 * @param category next {@linkplain AbsenceTypeCategory}
 * @param color next {@linkplain AbsenceColor}
 * @param labelByLocale next labels
 */
public record AbsenceTypeUpdate(
    Long sourceId,
    AbsenceTypeCategory category,
    AbsenceColor color,
    Map<Locale, String> labelByLocale
) {
}
