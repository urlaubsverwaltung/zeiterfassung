package de.focusshift.zeiterfassung.timeentry.settings;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

/**
 * Duration when {@linkplain TimeEntry}s are not allowed to be edited anymore by users without permission.
 *
 * <p>
 * (e.g. end of last day, or end of last week)
 *
 * @param enabled
 * @param value
 * @param unit
 */
public record TimeEntryFreeze(boolean enabled, int value, Unit unit) {

    public enum Unit {
        NONE,
        DAY,
        WEEK,
        MONTH
    }
}
