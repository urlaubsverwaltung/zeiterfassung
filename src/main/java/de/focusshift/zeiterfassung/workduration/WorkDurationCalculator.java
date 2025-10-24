package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

import java.util.List;

/**
 * Base interface for various {@link WorkDuration} calculation strategies.
 */
interface WorkDurationCalculator {

    WorkDuration calculateWorkDuration(List<TimeEntry> timeEntries);
}
