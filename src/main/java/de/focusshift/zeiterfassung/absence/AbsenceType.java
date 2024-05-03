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
 * @param labelByLocale label of this {@linkplain AbsenceType} for a given {@linkplain Locale}
 */
public record AbsenceType(
    AbsenceTypeCategory category,
    Long sourceId,
    Map<Locale, String> labelByLocale
) {

    public static AbsenceType SICK = new AbsenceType(AbsenceTypeCategory.SICK, null, Map.of());

    /**
     * implicitly known HOLIDAY absence to ease testing.
     * note: do not use in production code since sourceId is not ensured to be correct in runtime.
     */
    public static AbsenceType absenceTypeHoliday() {
        return absenceTypeHoliday(Map.of());
    }

    /**
     * implicitly known HOLIDAY absence to ease testing.
     * note: do not use in production code since sourceId is not ensured to be correct in runtime.
     */
    public static AbsenceType absenceTypeHoliday(Map<Locale, String> labelByLocale) {
        return new AbsenceType(AbsenceTypeCategory.HOLIDAY, 1000L, labelByLocale);
    }

    /**
     * implicitly known SPECIALLEAVE absence to ease testing.
     * note: do not use in production code since sourceId is not ensured to be correct in runtime.
     */
    public static AbsenceType absenceTypeSpecialLeave() {
        return absenceTypeSpecialLeave(Map.of());
    }

    /**
     * implicitly known SPECIALLEAVE absence to ease testing.
     * note: do not use in production code since sourceId is not ensured to be correct in runtime.
     */
    public static AbsenceType absenceTypeSpecialLeave(Map<Locale, String> labelByLocale) {
        return new AbsenceType(AbsenceTypeCategory.SPECIALLEAVE, 2000L, labelByLocale);
    }
}
