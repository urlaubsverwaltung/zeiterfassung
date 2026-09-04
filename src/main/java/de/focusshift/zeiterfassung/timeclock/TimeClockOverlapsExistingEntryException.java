package de.focusshift.zeiterfassung.timeclock;

class TimeClockOverlapsExistingEntryException extends RuntimeException {

    TimeClockOverlapsExistingEntryException() {
        super("The stopped time clock would overlap an existing time entry.");
    }
}
