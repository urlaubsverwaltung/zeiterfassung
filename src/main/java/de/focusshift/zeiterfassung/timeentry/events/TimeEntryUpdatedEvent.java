package de.focusshift.zeiterfassung.timeentry.events;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.workduration.WorkDuration;

import java.time.LocalDate;

/**
 * Event dispatched after a {@link TimeEntry} has been updated.
 *
 * @param timeEntryId time entry id
 * @param ownerUserIdComposite timeEntry owner {@link UserIdComposite}
 * @param lockedCandidate maybe updated locked value
 * @param dateCandidate maybe updated date
 * @param workDurationCandidate maybe updated work duration
 */
public record TimeEntryUpdatedEvent(
    TimeEntryId timeEntryId,
    UserIdComposite ownerUserIdComposite,
    UpdatedValueCandidate<Boolean> lockedCandidate,
    UpdatedValueCandidate<LocalDate> dateCandidate,
    UpdatedValueCandidate<WorkDuration> workDurationCandidate
) {

    /**
     * This tuple contains the previous and the new value of an entity.
     *
     * @param previous the previous value
     * @param current the new value (could be the same as {@link #previous}, check with {@link #hasChanged()})
     * @param <T> type of the value
     */
    public record UpdatedValueCandidate<T>(T previous, T current) {

        /**
         * Whether the value has changed or not.
         *
         * @return {@code true} when the value has changed, {@code false} otherwise.
         */
        public boolean hasChanged() {
            return !previous.equals(current);
        }
    }
}
