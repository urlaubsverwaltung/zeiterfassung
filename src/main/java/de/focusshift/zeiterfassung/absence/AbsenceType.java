package de.focusshift.zeiterfassung.absence;

import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Map;

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

    public AbsenceType(AbsenceTypeCategory category, @Nullable Long sourceId, @Nullable Map<Locale, String> labelByLocale) {
        this.category = category;
        this.sourceId = sourceId;
        this.labelByLocale = labelByLocale;
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
