package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WorkingTimeService {

    Optional<WorkingTime> getWorkingTimeById(WorkingTimeId workingTimeId);

    /**
     * Finds all {@linkplain WorkingTime}s for the user and sorts the list by {@linkplain WorkingTime#validFrom()}
     * descending (first element is the newest, last element the oldest).
     *
     * @return sorted list of {@linkplain WorkingTime}s
     */
    List<WorkingTime> getAllWorkingTimesByUser(UserLocalId userLocalId);

    /**
     * Finds all {@linkplain WorkingTime}s for all the given users and sorts the lists by
     * {@linkplain WorkingTime#validFrom()} descending (first element is the newest, last element the oldest).
     *
     * @param from include WorkingTimes greaterOrEqual from date
     * @param toExclusive include WorkingTimes before toExclusive date
     * @return Map of sorted {@linkplain WorkingTime} lists
     */
    Map<UserIdComposite, List<WorkingTime>> getWorkingTimesByUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds);

    /**
     * Finds all {@linkplain WorkingTime}s for all users and sorts the lists by {@linkplain WorkingTime#validFrom()}
     * descending (first element is the newest, last element the oldest).
     *
     * @param from include WorkingTimes greaterOrEqual from date
     * @param toExclusive include WorkingTimes before toExclusive date
     * @return Map of sorted {@linkplain WorkingTime} lists
     */
    Map<UserIdComposite, List<WorkingTime>> getAllWorkingTimes(LocalDate from, LocalDate toExclusive);

    /**
     * Create a new {@linkplain WorkingTime} entry for the {@linkplain User}.
     *
     * @param userLocalId id of the user
     * @param validFrom date the created working time is valid from
     * @param workdays workdays info
     * @return the created {@linkplain WorkingTime}
     */
    WorkingTime createWorkingTime(UserLocalId userLocalId, LocalDate validFrom, EnumMap<DayOfWeek, Duration> workdays);

    /**
     * Update the {@linkplain WorkingTime}
     *
     * @param workingTimeId id of the working time to update
     * @param validFrom new validFrom date, may be {@code null} for the very first {@linkplain WorkingTime}
     * @param workdays workdays info
     * @return the updated {@linkplain WorkingTime}
     */
    WorkingTime updateWorkingTime(WorkingTimeId workingTimeId, LocalDate validFrom, EnumMap<DayOfWeek, Duration> workdays);

    /**
     * Delete the {@linkplain WorkingTime} with the given id if possible.
     *
     * @param workingTimeId id to delete
     * @return {@code true} when {@linkplain WorkingTime} is deleted, {@code false} otherwise
     */
    boolean deleteWorkingTime(WorkingTimeId workingTimeId);
}
