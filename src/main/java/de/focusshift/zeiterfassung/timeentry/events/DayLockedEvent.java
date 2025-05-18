package de.focusshift.zeiterfassung.timeentry.events;

import java.time.LocalDate;

/**
 * Event dispatched when a day has been locked and users are not allowed to edit timeEntries for that day.
 */
public record DayLockedEvent(LocalDate date) {
}
