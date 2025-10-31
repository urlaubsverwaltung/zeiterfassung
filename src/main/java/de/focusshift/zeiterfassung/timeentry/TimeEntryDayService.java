package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TimeEntryDayService {

    /**
     * {@linkplain TimeEntryDay}s for the given criteria sorted by date, newest is the first item.
     *
     * @param from        first date of interval
     * @param toExclusive last date (exclusive) of interval
     * @param userLocalId to get entries for
     * @return sorted list of {@linkplain TimeEntryDay}s
     */
    List<TimeEntryDay> getTimeEntryDays(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId);

    /**
     * {@linkplain TimeEntryDay}s for all given users and interval.
     *
     * @param from         first date of interval
     * @param toExclusive  last date (exclusive) of interval
     * @param userLocalIds {@linkplain UserLocalId}s of desired users
     * @return unsorted list of {@linkplain TimeEntry}s grouped by user
     */
    Map<UserIdComposite, List<TimeEntryDay>> getTimeEntryDays(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds);

    /**
     * {@linkplain TimeEntryDay}s for all users and the given interval.
     *
     * @param from        first date of interval
     * @param toExclusive last date (exclusive) of interval
     * @return unsorted list of entries grouped by user
     */
    Map<UserIdComposite, List<TimeEntryDay>> getTimeEntryDaysForAllUsers(LocalDate from, LocalDate toExclusive);

    /**
     * {@linkplain TimeEntryWeekPage}s for the given user and week of year with sorted {@linkplain TimeEntry}s
     * by {@linkplain TimeEntry#start()}, newest is the first item.
     *
     * @param userLocalId to get the {@linkplain TimeEntryWeekPage} for
     * @param year        given year
     * @param weekOfYear  given week of year
     * @return {@linkplain TimeEntryWeekPage} with sorted {@linkplain TimeEntry}s.
     */
    TimeEntryWeekPage getEntryWeekPage(UserLocalId userLocalId, int year, int weekOfYear);
}
