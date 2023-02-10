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

    /**
     * {@linkplain TimeEntry}s for all users and the given interval.
     *
     * @param from first date of interval
     * @param toExclusive last date (exclusive) of interval
     *
     * @return unsorted list of {@linkplain TimeEntry}s.
     */
    List<TimeEntry> getEntriesForAllUsers(LocalDate from, LocalDate toExclusive);

    /**
     * {@linkplain TimeEntry}s for all given users and interval.
     *
     * @param from first date of interval
     * @param toExclusive last date (exclusive) of interval
     * @param userLocalIds {@linkplain UserLocalId}s of desired users
     *
     * @return unsorted list of {@linkplain TimeEntry}s.
     */
    List<TimeEntry> getEntriesByUserLocalIds(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds);

    /**
     * {@linkplain TimeEntryWeekPage}s for the given user and week of year with sorted {@linkplain TimeEntry}s
     * by {@linkplain TimeEntry#start()}, newest is the first item.
     *
     * @param userId to get the {@linkplain TimeEntryWeekPage} for
     * @param year given year
     * @param weekOfYear given week of year
     *
     * @return {@linkplain TimeEntryWeekPage} with sorted {@linkplain TimeEntry}s.
     */
    TimeEntryWeekPage getEntryWeekPage(UserId userId, int year, int weekOfYear);

    TimeEntry saveTimeEntry(TimeEntry timeEntry);

    void deleteTimeEntry(long timeEntryId);
}
