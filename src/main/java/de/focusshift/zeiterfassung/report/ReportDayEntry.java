package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.workduration.WorkDuration;

import java.time.ZonedDateTime;

/**
 * Provides {@linkplain TimeEntry} information for reports.
 *
 * @param timeEntryId id of the corresponding {@linkplain TimeEntry}
 * @param user user the entry belongs to
 * @param comment comment of the day entry, never {@code null}
 * @param start start timestamp fo the entry
 * @param end end timestamp of the entry
 * @param workDuration workDuration of the entry
 * @param isBreak whether the entry is a break or not
 */
public record ReportDayEntry(
    TimeEntryId timeEntryId,
    User user,
    String comment,
    ZonedDateTime start,
    ZonedDateTime end,
    WorkDuration workDuration,
    boolean isBreak
) {
}
