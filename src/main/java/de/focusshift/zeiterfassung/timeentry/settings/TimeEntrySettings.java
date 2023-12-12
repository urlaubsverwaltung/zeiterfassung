package de.focusshift.zeiterfassung.timeentry.settings;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

/**
 * Application wide {@linkplain TimeEntry} settings that ca nbe configured by users with permission.
 *
 * @param timeEntryFreeze
 */
public record TimeEntrySettings(TimeEntryFreeze timeEntryFreeze) {
}
