package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionMetadata;

public interface TimeEntryHistoryItem {

    EntityRevisionMetadata metadata();

    TimeEntry timeEntry();

    boolean commentModified();

    boolean startModified();

    boolean endModified();

    boolean isBreakModified();
}
