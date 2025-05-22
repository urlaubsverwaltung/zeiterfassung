package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.overtime.events.UserHasWorkedOvertimeEvent;

interface DayLockedSchedulerService {

    /**
     * Checks for locked days and publishes {@link UserHasWorkedOvertimeEvent} for locked days.
     */
    void checkLockedAndPublishOvertime();
}
