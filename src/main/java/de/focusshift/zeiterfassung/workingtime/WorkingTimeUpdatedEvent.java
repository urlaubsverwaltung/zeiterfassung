package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;

/**
 * Event dispatched after a {@link WorkingTime} has been updated.
 *
 * @param userIdComposite user who owns this working time
 * @param workingTimeId id of the updated working time
 * @param validFrom start date of this working time's validity ({@code null} for the very first working time)
 * @param previousValidFrom start date of this working time's validity <em>before</em> the update
 *                          ({@code null} for the very first working time). Together with {@code validFrom}
 *                          this spans the date range affected by the update, regardless of whether
 *                          {@code validFrom} was moved into the past or into the future.
 * @param federalState federal state for public holiday calculation
 * @param worksOnPublicHoliday whether the user works on public holidays
 * @param workdays planned working hours per day of week
 */
public record WorkingTimeUpdatedEvent(
    UserIdComposite userIdComposite,
    WorkingTimeId workingTimeId,
    @Nullable LocalDate validFrom,
    @Nullable LocalDate previousValidFrom,
    FederalState federalState,
    Boolean worksOnPublicHoliday,
    EnumMap<DayOfWeek, Duration> workdays
) {
}
