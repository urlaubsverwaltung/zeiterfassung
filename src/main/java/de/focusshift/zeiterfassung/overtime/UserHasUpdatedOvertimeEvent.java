package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Describes an event that a person has updated overtime at a day.
 *
 * <p>
 * Difference to {@link UserHasMadeOvertimeEvent} is, that the value can be ZERO here.
 *
 * @param userIdComposite
 * @param date
 * @param overtimeHours
 */
public record UserHasUpdatedOvertimeEvent(
    UserIdComposite userIdComposite,
    LocalDate date,
    OvertimeHours overtimeHours
) {
}
