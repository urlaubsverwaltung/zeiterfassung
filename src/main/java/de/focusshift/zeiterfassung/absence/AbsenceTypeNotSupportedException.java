package de.focusshift.zeiterfassung.absence;

public class AbsenceTypeNotSupportedException extends IllegalArgumentException {
    public AbsenceTypeNotSupportedException(String name) {
        super("absence type=%s not supported.".formatted(name));
    }
}
