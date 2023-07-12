package de.focusshift.zeiterfassung.absence;

public enum AbsenceType {

    HOLIDAY,
    SPECIALLEAVE,
    UNPAIDLEAVE,
    SICK;

    public String getMessageKey() {
        return "absence.type." + this.name();
    }
}
