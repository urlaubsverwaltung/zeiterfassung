package de.focusshift.zeiterfassung.timeentry;

/**
 * Provides one {@link TimeEntryWeek} object and information about all time entries.
 *
 * @param timeEntryWeek    one time entry week
 * @param totalTimeEntries number of total existing time entries
 */
record TimeEntryWeekPage(TimeEntryWeek timeEntryWeek, long totalTimeEntries) {

    public long timeEntryWeekTimeEntries() {
        return timeEntryWeek.timeEntries().size();
    }
}
