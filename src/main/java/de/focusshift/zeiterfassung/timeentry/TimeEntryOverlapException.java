package de.focusshift.zeiterfassung.timeentry;

public class TimeEntryOverlapException extends RuntimeException {

    TimeEntryOverlapException() {
        super("The time entry overlaps an existing time entry of the same type.");
    }
}
