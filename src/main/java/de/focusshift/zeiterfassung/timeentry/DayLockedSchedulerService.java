package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.overtime.events.UserHasMadeOvertimeEvent;

interface DayLockedSchedulerService {

    /**
     * Checks for locked days and publishes {@link UserHasMadeOvertimeEvent} for locked days.
     */
    void checkLockedAndPublishOvertime();
}
