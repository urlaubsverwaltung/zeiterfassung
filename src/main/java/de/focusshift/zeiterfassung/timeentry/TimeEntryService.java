package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryService {

    /**
     * {@linkplain TimeEntry}s for the given criteria sorted by {@linkplain TimeEntry#start()}, newest is the first item.
     *
     * @param from first date of interval
     * @param toExclusive last date (exclusive) of interval
     * @param userId to get {@linkplain TimeEntry}s for
     *
     * @return sorted list of {@linkplain TimeEntry}s
     */
    List<TimeEntry> getEntries(LocalDate from, LocalDate toExclusive, UserId userId);

    List<TimeEntry> getEntriesForAllUsers(LocalDate from, LocalDate toExclusive);

    List<TimeEntry> getEntriesByUserLocalIds(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds);

    TimeEntryWeekPage getEntryWeekPage(UserId userId, int year, int weekOfYear);

    TimeEntry saveTimeEntry(TimeEntry timeEntry);

    void deleteTimeEntry(long timeEntryId);
}
