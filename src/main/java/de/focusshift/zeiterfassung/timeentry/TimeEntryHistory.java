package de.focusshift.zeiterfassung.timeentry;

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Audited History of a {@link TimeEntry}.
 *
 * @param timeEntryId id of the related {@link TimeEntry}
 * @param revisions sorted revisions. first element is the initially created instance.
 *                  while the last element is the last/current modification.
 */
public record TimeEntryHistory(TimeEntryId timeEntryId, List<TimeEntryUpdatedHistoryItem> revisions) {

    @Override
    public List<TimeEntryUpdatedHistoryItem> revisions() {
        return unmodifiableList(revisions);
    }
}
