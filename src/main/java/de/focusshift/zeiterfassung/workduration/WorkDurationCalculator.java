package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

import java.util.Collection;

/**
 * Base interface for various {@link WorkDuration} calculation strategies.
 */
interface WorkDurationCalculator {

    WorkDuration calculateWorkDuration(Collection<TimeEntry> timeEntries);
}
