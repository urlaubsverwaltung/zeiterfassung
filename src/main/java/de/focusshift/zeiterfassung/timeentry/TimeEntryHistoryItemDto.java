package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionType;

import java.time.LocalDateTime;

public record TimeEntryHistoryItemDto(
    String username,
    EntityRevisionType revisionType,
    LocalDateTime date,
    TimeEntryDTO timeEntry
) {
}
