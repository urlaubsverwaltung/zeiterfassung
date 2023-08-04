package de.focusshift.zeiterfassung.absence;

import org.springframework.lang.Nullable;

public record AbsenceType(AbsenceTypeCategory category, @Nullable Long sourceId) {

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

    public static AbsenceType SICK = new AbsenceType(AbsenceTypeCategory.SICK, null);
}
