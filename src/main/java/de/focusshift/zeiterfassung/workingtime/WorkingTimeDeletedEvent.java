package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;

/**
 * Event dispatched after a {@link WorkingTime} has been deleted.
 *
 * @param userIdComposite user who owned this working time
 * @param workingTimeId id of the deleted working time
 * @param validFrom start date of the deleted working time's validity
 */
public record WorkingTimeDeletedEvent(
    UserIdComposite userIdComposite,
    WorkingTimeId workingTimeId,
    LocalDate validFrom
) {
}
