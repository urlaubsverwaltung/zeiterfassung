package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WorkingTimeService {

    /**
     * Get the users {@linkplain WorkingTime}. If nothing has been set up yet, a default {@linkplain WorkingTime}
     * will be returned with 8 hours from monday to friday.
     *
     * @param userLocalId user id
     * @return {@linkplain WorkingTime} for the user, never {@code null}.
     */
    WorkingTime getWorkingTimeByUser(UserLocalId userLocalId);

    Optional<WorkingTime> getWorkingTimeById(WorkingTimeId workingTimeId);

    List<WorkingTime> getAllWorkingTimesByUser(UserLocalId userLocalId);

    Map<UserIdComposite, WorkingTime> getWorkingTimeByUsers(Collection<UserLocalId> userLocalIds);

    Map<UserIdComposite, WorkingTime> getAllWorkingTimeByUsers();

    /**
     * Get {@linkplain PlannedWorkingHours}. Note that public holidays and other absences are not considered.
     *
     * @param userLocalId user id
     * @param year year
     * @param weekOfYear week of year
     * @return {@linkplain PlannedWorkingHours} for every day.
     */
    Map<LocalDate, PlannedWorkingHours> getWorkingHoursByUserAndYearWeek(UserLocalId userLocalId, Year year, int weekOfYear);

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
     * @param workWeekUpdate new working time values
     * @return the updated {@linkplain WorkingTime}
     */
    WorkingTime updateWorkingTime(WorkingTimeId workingTimeId, WorkWeekUpdate workWeekUpdate);
}
