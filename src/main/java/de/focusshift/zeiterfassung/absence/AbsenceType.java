package de.focusshift.zeiterfassung.absence;

import org.springframework.lang.Nullable;

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
    @Nullable Long sourceId,
    @Nullable Map<Locale, String> labelByLocale
) {

    public AbsenceType(AbsenceTypeCategory category) {
        this(category, null, null);
    }

    public AbsenceType(AbsenceTypeCategory category, @Nullable Long sourceId) {
        this(category, sourceId, null);
    }

    /**
     * implicitly known HOLIDAY absence to ease testing.
     * note: do not use in production code since sourceId is not ensured to be correct in runtime.
     */
    public static AbsenceType HOLIDAY = new AbsenceType(AbsenceTypeCategory.HOLIDAY, 1000L);

    /**
     * implicitly known SPECIALLEAVE absence to ease testing.
     * note: do not use in production code since sourceId is not ensured to be correct in runtime.
     */
    public static AbsenceType SPECIALLEAVE = new AbsenceType(AbsenceTypeCategory.SPECIALLEAVE, 2000L);

    public static AbsenceType SICK = new AbsenceType(AbsenceTypeCategory.SICK);
}
