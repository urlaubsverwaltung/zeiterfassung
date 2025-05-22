package de.focusshift.zeiterfassung.overtime.events;

import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Describes an event that a person has updated overtime at a day.
 *
 * <p>
 * Difference to {@link UserHasWorkedOvertimeEvent} is, that the value can be ZERO here.
 *
 * @param userIdComposite
 * @param date
 * @param overtimeHours
 */
public record UserHasWorkedOvertimeUpdatedEvent(
    UserIdComposite userIdComposite,
    LocalDate date,
    OvertimeHours overtimeHours
) {
}
