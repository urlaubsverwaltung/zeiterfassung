package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.usermanagement.User;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Provides {@linkplain de.focusshift.zeiterfassung.timeentry.TimeEntry} information for reports.
 *
 * @param user user the entry belongs to
 * @param comment comment of the day entry, never {@code null}
 * @param start start timestamp fo the entry
 * @param end end timestamp of the entry
 * @param isBreak whether the entry is a break or not
 */
record ReportDayEntry(
    User user,
    String comment,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isBreak
) {

    public WorkDuration workDuration() {
        return new WorkDuration(isBreak ? Duration.ZERO : Duration.between(start, end));
    }
}
