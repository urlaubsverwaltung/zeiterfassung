package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.usermanagement.User;

import java.time.Duration;
import java.time.ZonedDateTime;

record ReportDayEntry(User user, String comment, ZonedDateTime start, ZonedDateTime end, boolean isBreak) {

    public WorkDuration workDuration() {
        return new WorkDuration(isBreak ? Duration.ZERO : Duration.between(start, end));
    }
}
