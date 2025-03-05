package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

public record TimeEntryDialogDto(
    String owner,
    String ownerInitials,
    List<TimeEntryHistoryItemDto> historyItems,
    String dialogCloseFormAction
) {
}
