package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionType;

import java.time.LocalDateTime;

public record TimeEntryHistoryItemDto(
    String username,
    String initials,
    EntityRevisionType revisionType,
    LocalDateTime date,
    TimeEntryDTO timeEntry
) {
}
