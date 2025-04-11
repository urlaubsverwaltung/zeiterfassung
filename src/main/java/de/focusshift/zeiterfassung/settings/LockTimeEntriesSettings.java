package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;

/**
 * Global setting for locking time entries.
 *
 * <p>
 * Example for {@link LockTimeEntriesSettings#lockTimeEntriesDaysInPast}:
 * <ul>
 *     <li>{@code 0} - allowed to create, update or delete {@link TimeEntry} only for today.</li>
 *     <li>{@code 1} - allowed to create, update or delete {@link TimeEntry} today and yesterday, but not the day before yesterday.</li>
 * </ul>
 *
 * <p>
 * Note that privileged persons are allowed to bypass this restriction.
 *
 * @param lockingIsActive           whether this feature is enabled or not
 * @param lockTimeEntriesDaysInPast number of days time entries in past get locked. can be negative when
 *                                  {@link LockTimeEntriesSettings#lockingIsActive} is set to {@code false}. must be
 *                                  zero or positive when {@link LockTimeEntriesSettings#lockingIsActive} is set to
 *                                  {@code true}.
 */
public record LockTimeEntriesSettings(boolean lockingIsActive, int lockTimeEntriesDaysInPast) {

    /**
     * Default LockTimeSettings.
     *
     * <ul>
     *     <li>{@link LockTimeEntriesSettings#lockingIsActive()} = {@code false}</li>
     *     <li>{@link LockTimeEntriesSettings#lockTimeEntriesDaysInPast()} = {@code 2}</li>
     * </ul>
     */
    public static final LockTimeEntriesSettings DEFAULT = new LockTimeEntriesSettings(false, 2);
}
