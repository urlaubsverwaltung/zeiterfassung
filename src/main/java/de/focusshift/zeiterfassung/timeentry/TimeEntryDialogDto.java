package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

public record TimeEntryDialogDto(
    String owner,
    List<TimeEntryHistoryItemDto> historyItems,
    String editTimeEntryFormAction,
    String dialogCloseFormAction
) {
}
