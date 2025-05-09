package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Describes an event that a person has made overtime at a day.
 *
 * @param userIdComposite
 * @param date
 * @param overtimeHours
 */
public record UserHasMadeOvertimeEvent(
    UserIdComposite userIdComposite,
    LocalDate date,
    OvertimeHours overtimeHours
) {
}
