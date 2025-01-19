package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

public record TimeEntryHistory(TimeEntryId timeEntryId, List<TimeEntryHistoryItem> revisions) {
}
