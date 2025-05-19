package de.focusshift.zeiterfassung.timeentry.events;

import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.WorkDuration;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Event dispatched after a {@link TimeEntry} has been created.
 */
public record TimeEntryCreatedEvent(
    TimeEntryId timeEntryId,
    UserIdComposite ownerUserIdComposite,
    boolean locked,
    LocalDate date,
    WorkDuration workDuration
) {
}
