package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.timeentry.TimeEntryId;

@FunctionalInterface
interface TimeEntryIdLinkBuilder {

    /**
     * Create an url for the given {@linkplain de.focusshift.zeiterfassung.timeentry.TimeEntryId}.
     *
     * @param timeEntryId id of a time entry
     * @return url for the given {@linkplain de.focusshift.zeiterfassung.timeentry.TimeEntryId}
     */
    String getTimeEntryIdUrl(TimeEntryId timeEntryId);
}
