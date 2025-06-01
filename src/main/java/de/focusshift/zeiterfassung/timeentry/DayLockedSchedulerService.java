package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent;

interface DayLockedSchedulerService {

    /**
     * Checks for locked days and publishes {@link DayLockedEvent}.
     */
    void checkDayLockedAndPublish();
}
