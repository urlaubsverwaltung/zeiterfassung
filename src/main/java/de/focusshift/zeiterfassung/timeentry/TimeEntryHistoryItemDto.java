package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.data.history.EntityRevisionType;

import java.time.LocalDate;

public record TimeEntryHistoryItemDto(
    String username,
    EntityRevisionType revisionType,
    LocalDate date,
    TimeEntryDTO timeEntry
) {
}
