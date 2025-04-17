package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Optional;

public interface TimeEntryLockService {

    LockTimeEntriesSettings getLockTimeEntriesSettings();

    /**
     * Checks whether the given temporal is locked or not. This can be configured by privileged persons with
     * {@link de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings}.
     *
     * <p>
     * This method should not be called in a loop over multiple days. Settings must be known to calculate whether
     * the temporal is locked or not. When iterating, please consider using {@link #isLocked(Temporal, LockTimeEntriesSettings)}.
     *
     * <p>
     * Note that this does not consider whether the current AuthenticationPrincipal is privileged to bypass the lock
     * or not. Use {@link #isUserAllowedToBypassLock(Collection)} if required.
     *
     * @param temporal temporal to check
     * @return {@code true} when the temporal is locked, {@code false} otherwise
     */
    default boolean isLocked(Temporal temporal) {
        return isLocked(temporal, getLockTimeEntriesSettings());
    }

    /**
     * Checks whether the given temporal is locked or not. This can be configured by privileged persons with
     * {@link de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings}.
     *
     * <p>
     * Note that this does not consider whether the current AuthenticationPrincipal is privileged to bypass the lock
     * or not. Use {@link #isUserAllowedToBypassLock(Collection)} if required.
     *
     * @param temporal temporal to check
     * @param lockTimeEntriesSettings current user settings
     * @return {@code true} when the temporal is locked, {@code false} otherwise
     */
    boolean isLocked(Temporal temporal, LockTimeEntriesSettings lockTimeEntriesSettings);

    /**
     * Checks whether the given timespan is locked or not. This can be configured by privileged persons with
     * {@link de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings}
     *
     * <p>
     * Note that this does not consider whether the current AuthenticationPrincipal is privileged to bypass the lock
     * or not. Use {@link #isUserAllowedToBypassLock(Collection)} if required.
     *
     * @param start start date of the timespan
     * @param end end date of the timespan
     * @return {@code true} when the timespan is locked, {@code false} otherwise
     */
    boolean isTimespanLocked(Temporal start, Temporal end);

    /**
     * Checks whether the given user is allowed to create, edit or delete {@link TimeEntry} despite a locked timespan.
     *
     * @param roles user roles to check
     * @return {@code true} when the user is allowed to bypass the lock, {@link false} otherwise
     */
    boolean isUserAllowedToBypassLock(Collection<SecurityRole> roles);

    /**
     * Returns the minimum valid date in the past a {@link TimeEntry} can be created for, if there is one.
     *
     * @return Optional#empty when locking {@link TimeEntry} is disabled, the min valid date otherwise.
     */
    Optional<LocalDate> getMinValidTimeEntryDate();
}
