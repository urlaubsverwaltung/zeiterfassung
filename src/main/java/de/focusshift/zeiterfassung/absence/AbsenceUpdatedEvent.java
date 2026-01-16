package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.DateRange;
import de.focusshift.zeiterfassung.user.UserId;

public record AbsenceUpdatedEvent(UserId userId, DateRange oldDateRange, DateRange newDateRange) {
}
