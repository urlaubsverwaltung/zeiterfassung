package de.focusshift.zeiterfassung.timeentry;

public class TimeEntryNotFoundException extends RuntimeException {

    TimeEntryNotFoundException(TimeEntryId timeEntryId) {
        super("TimeEntry with %s not found.".formatted(timeEntryId));
    }
}
