package de.focusshift.zeiterfassung.overtime.events;

import de.focusshift.zeiterfassung.overtime.OvertimeHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Describes an event that a person has made overtime at a day.
 *
 * @param userIdComposite
 * @param date
 * @param overtimeHours
 */
public record UserHasWorkedOvertimeEvent(
    UserIdComposite userIdComposite,
    LocalDate date,
    OvertimeHours overtimeHours
) {
}
