package de.focusshift.zeiterfassung.timeentry.events;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Event dispatched when a day has been locked for the given zoneId.
 *
 * <p>
 * Users are not allowed to edit timeEntries for that day anymore.
 *
 * @param date date that has been locked, note to consider the zoneId, too
 * @param zoneId zoneId the locked day event occurred
 */
public record DayLockedEvent(LocalDate date, ZoneId zoneId) {
}
