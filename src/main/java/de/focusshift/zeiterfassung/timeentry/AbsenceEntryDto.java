package de.focusshift.zeiterfassung.timeentry;

import java.time.LocalDate;

record AbsenceEntryDto(LocalDate date, String nameMessageKey) {
}
