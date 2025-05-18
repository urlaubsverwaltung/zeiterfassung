package de.focusshift.zeiterfassung.timeentry.events;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Event dispatched after a {@link TimeEntry} has been deleted.
 *
 * @param timeEntryId deleted time entry id
 * @param locked whether the time entry was locked or not
 * @param ownerUserIdComposite timeEntry owner {@link UserIdComposite}
 * @param date start date of the deleted timeEntry
 * @param workDuration {@link WorkDuration} of the deleted timeEntry
 */
public record TimeEntryDeletedEvent(
    TimeEntryId timeEntryId,
    UserIdComposite ownerUserIdComposite,
    boolean locked,
    LocalDate date,
    WorkDuration workDuration
) {
}
