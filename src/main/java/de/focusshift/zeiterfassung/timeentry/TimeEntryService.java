package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TimeEntryService {

    /**
     * Find a {@linkplain TimeEntry} by id.
     *
     * @param id of the time entry
     * @return the {@linkplain TimeEntry} or empty optional.
     */
    Optional<TimeEntry> findTimeEntry(TimeEntryId id);

    /**
     * Find the {@linkplain TimeEntryHistory} for the given time entry.
     *
     * @param id id of the time entry
     * @return the {@linkplain TimeEntryHistory} or empty optional when the time entry is unknown
     */
    Optional<TimeEntryHistory> findTimeEntryHistory(TimeEntryId id);

    /**
     * {@linkplain TimeEntry}s for the given criteria sorted by {@linkplain TimeEntry#start()}, newest is the first item.
     *
     * @param from        first date of interval
     * @param toExclusive last date (exclusive) of interval
     * @param userLocalId to get {@linkplain TimeEntry}s for
     * @return sorted list of {@linkplain TimeEntry}s
     */
    List<TimeEntry> getEntries(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId);

    /**
     * {@linkplain TimeEntry}s for all given users and interval.
     *
     * @param from         first date of interval
     * @param toExclusive  last date (exclusive) of interval
     * @param userLocalIds {@linkplain UserLocalId}s of desired users
     * @return unsorted list of {@linkplain TimeEntry}s grouped by user
     */
    Map<UserIdComposite, List<TimeEntry>> getEntries(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds);

    /**
     * {@linkplain TimeEntry}s for all users and the given interval.
     *
     * @param from        first date of interval
     * @param toExclusive last date (exclusive) of interval
     * @return unsorted list of {@linkplain TimeEntry}s grouped by user
     */
    Map<UserIdComposite, List<TimeEntry>> getEntriesForAllUsers(LocalDate from, LocalDate toExclusive);

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

    /**
     * Creates a new {@linkplain TimeEntry}.
     *
     * <p>
     * Note that this method does not check if it is allowed to create a new {@link TimeEntry} for the given timespan.
     * You have to check yourself whether these days are locked or not!
     *
     * @param userLocalId id of the linked user
     * @param comment     optional comment
     * @param start       start of the time entry.
     * @param end         end of the time entry.
     * @param isBreak     whether it is a break or not.
     * @return the created {@linkplain TimeEntry} with an id.
     * @throws IllegalArgumentException when given timeEntry already has an id.
     */
    TimeEntry createTimeEntry(UserLocalId userLocalId, @Nullable String comment, ZonedDateTime start, ZonedDateTime end, boolean isBreak);

    /**
     * Updates the existing {@linkplain TimeEntry}
     *
     * <p>
     * Note that this method does not check if it is allowed to update the {@link TimeEntry}.
     * You have to check yourself whether the {@link TimeEntry#start()} is locked or not!
     *
     * @param id       of the {@linkplain TimeEntry} to update
     * @param comment  optional comment. not that {@code null} overrides the existing comment.
     * @param start    new start. may be {@code null} when end and duration is given.
     * @param end      new end. may be {@code null} when start and duration is given.
     * @param duration new value. may be {@code null} when start and end is given.
     * @param isBreak  new isBreak
     * @return the updated {@linkplain TimeEntry}.
     * @throws IllegalStateException                when there is no {@linkplain TimeEntry} with the given id.
     * @throws TimeEntryUpdateNotPlausibleException when {@code start}, {@code end} and {@code duration} has been changed. only a selection of two is possible.
     */
    TimeEntry updateTimeEntry(TimeEntryId id, @Nullable String comment, @Nullable ZonedDateTime start, @Nullable ZonedDateTime end, @Nullable Duration duration, boolean isBreak) throws TimeEntryUpdateNotPlausibleException;

    /**
     * Deletes the {@link TimeEntry} with the given id.
     *
     * @param id {@link TimeEntry} id to delete
     */
    void deleteTimeEntry(TimeEntryId id);
}
