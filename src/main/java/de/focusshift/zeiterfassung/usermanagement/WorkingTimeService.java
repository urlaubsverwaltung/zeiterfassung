package de.focusshift.zeiterfassung.usermanagement;

public interface WorkingTimeService {

    /**
     * Get the users {@linkplain WorkingTime}. If nothing has been set up yet, a default {@linkplain WorkingTime}
     * will be returned with 8 hours from mondeay to friday.
     *
     * @param userLocalId user id
     * @return {@linkplain WorkingTime} for the user, never {@code null}.
     */
    WorkingTime getWorkingTimeByUser(UserLocalId userLocalId);

    /**
     * Update the {@linkplain WorkingTime}
     *
     * @param workingTime {@linkplain WorkingTime} to update
     * @return the updated {@linkplain WorkingTime}
     */
    WorkingTime updateWorkingTime(WorkingTime workingTime);
}
