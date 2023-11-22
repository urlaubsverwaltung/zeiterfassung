package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.usermanagement.User;

public record ReportDayAbsence(User user, Absence absence) {
}
