package de.focusshift.zeiterfassung.timeentry;

public record TimeEntryHistoryItemDto(
    String username,
    String status,
    String date,
    TimeEntryDTO timeEntry
) {
}
