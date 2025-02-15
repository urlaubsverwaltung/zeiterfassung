package de.focusshift.zeiterfassung.timeentry;

public class TimeEntryNotFoundException extends RuntimeException {

    TimeEntryNotFoundException(TimeEntryId timeEntryId) {
        super("TimeEntry id=%s not found.".formatted(timeEntryId));
    }
}
