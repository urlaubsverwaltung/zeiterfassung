package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.security.SecurityRole;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Optional;

public interface TimeEntryLockService {

    /**
     * Checks whether the given timespan is locked or not. This can be configured by privileged persons with
     * {@link de.focusshift.zeiterfassung.settings.LockTimeEntriesSettings}
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
