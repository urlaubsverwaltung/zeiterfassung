package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.ZonedDateTime;

/**
 * Event dispatched after a {@link TimeEntry} has been updated.
 *
 * @param timeEntryId time entry id
 * @param locked whether the time entry is locked or not
 * @param ownerUserIdComposite timeEntry owner {@link UserIdComposite}
 * @param newStartDate the new start date
 */
public record TimeEntryUpdatedEvent(
    TimeEntryId timeEntryId,
    boolean locked,
    UserIdComposite ownerUserIdComposite,
    ZonedDateTime newStartDate
) {
}
