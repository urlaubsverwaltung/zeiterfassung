package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;

public record TimeEntryUpdatedHistoryItem(
    EntityRevisionMetadata metadata,
    TimeEntry timeEntry,
    boolean commentModified,
    boolean startModified,
    boolean endModified,
    boolean isBreakModified
) {
}
