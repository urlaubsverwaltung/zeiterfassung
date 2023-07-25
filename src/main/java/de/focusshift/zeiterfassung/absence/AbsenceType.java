package de.focusshift.zeiterfassung.absence;

public enum AbsenceType {

    HOLIDAY,
    SPECIALLEAVE,
    UNPAIDLEAVE,
    OTHER,

    SICK;

    public String getMessageKey() {
        return "absence.type." + this.name();
    }
}
