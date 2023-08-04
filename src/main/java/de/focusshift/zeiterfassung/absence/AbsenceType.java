package de.focusshift.zeiterfassung.absence;

public record AbsenceType(AbsenceTypeCategory category, Long sourceId) {

    public static AbsenceType HOLIDAY = new AbsenceType(AbsenceTypeCategory.HOLIDAY, 1000L);
    public static AbsenceType SPECIALLEAVE = new AbsenceType(AbsenceTypeCategory.SPECIALLEAVE, 2000L);
    public static AbsenceType UNPAIDLEAVE = new AbsenceType(AbsenceTypeCategory.UNPAIDLEAVE, 3000L);
    public static AbsenceType SICK = new AbsenceType(AbsenceTypeCategory.SICK, null);
}
