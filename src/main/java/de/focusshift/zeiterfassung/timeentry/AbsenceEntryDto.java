package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.AbsenceColor;

import java.time.LocalDate;

record AbsenceEntryDto(LocalDate date, String name, AbsenceColor color) {
}
