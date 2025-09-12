package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

public record TimeEntryDialogDto(
    boolean allowedToEdit,
    String owner,
    String ownerInitials,
    List<TimeEntryHistoryItemDto> historyItems,
    String editTimeEntryFormAction,
    String dialogCloseFormAction
) {
}
