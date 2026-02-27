package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;

/**
 * Event dispatched after a {@link WorkingTime} has been updated.
 *
 * @param userIdComposite user who owns this working time
 * @param workingTimeId id of the updated working time
 * @param validFrom start date of this working time's validity
 * @param federalState federal state for public holiday calculation
 * @param worksOnPublicHoliday whether the user works on public holidays
 * @param workdays planned working hours per day of week
 */
public record WorkingTimeUpdatedEvent(
    UserIdComposite userIdComposite,
    WorkingTimeId workingTimeId,
    LocalDate validFrom,
    FederalState federalState,
    Boolean worksOnPublicHoliday,
    EnumMap<DayOfWeek, Duration> workdays
) {
}
