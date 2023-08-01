package de.focusshift.zeiterfassung.absence;

import java.util.Set;

public record AbsenceType(String name, Integer sourceId) {

    private static final String VACATION_CATEGORY_HOLIDAY = "HOLIDAY";
    private static final String VACATION_CATEGORY_SPECIALLEAVE = "SPECIALLEAVE";
    private static final String VACATION_CATEGORY_UNPAIDLEAVE = "UNPAIDLEAVE";
    private static final String VACATION_CATEGORY_OTHER = "OTHER";
    private static final String VACATION_CATEGORY_SICK = "SICK";
    private static final Set<String> VALID_VACATION_CATEGORIES = Set.of(VACATION_CATEGORY_HOLIDAY, VACATION_CATEGORY_SPECIALLEAVE, VACATION_CATEGORY_UNPAIDLEAVE, VACATION_CATEGORY_OTHER, VACATION_CATEGORY_SICK);

    public static boolean isValidVacationTypeCategory(String vacationTypeCategory) {
        return VALID_VACATION_CATEGORIES.contains(vacationTypeCategory);
    }


    public String getMessageKey() {
        if(this.sourceId == null) {
            return "absence.type.%s".formatted(this.name);
        } else {
            return "absence.type.%s.%s".formatted(this.name, this.sourceId);
        }
    }

    public static AbsenceType HOLIDAY = new AbsenceType(VACATION_CATEGORY_HOLIDAY, 1000);
    public static AbsenceType SPECIALLEAVE = new AbsenceType(VACATION_CATEGORY_SPECIALLEAVE, 2000);
    public static AbsenceType UNPAIDLEAVE = new AbsenceType(VACATION_CATEGORY_UNPAIDLEAVE, 3000);
    public static AbsenceType SICK = new AbsenceType(VACATION_CATEGORY_SICK, null);
}
