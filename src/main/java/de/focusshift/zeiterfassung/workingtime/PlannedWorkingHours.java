package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.timeentry.ShouldWorkingHours;

import java.time.Duration;

/**
 * Defines a {@linkplain Duration} of planned working hours. This does not include {@linkplain Absence}s.
 * e.g. the employment contract of 40h a week.
 *
 * <p>
 * see {@linkplain ShouldWorkingHours} if you are interested in a value that includes sick days for instance.
 *
 * @param duration the exact duration. not rounded up to minutes.
 */
public record PlannedWorkingHours(Duration duration) implements ZeitDuration {

    public static final PlannedWorkingHours ZERO = new PlannedWorkingHours(Duration.ZERO);
    public static final PlannedWorkingHours EIGHT = new PlannedWorkingHours(Duration.ofHours(8));

    /**
     * Returns a copy of this plannedWorkingHours with the specified plannedWorkingHours added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param plannedWorkingHours  the plannedWorkingHours to add, not null
     * @return a {@code PlannedWorkingHours} based on this plannedWorkingHours with the specified plannedWorkingHours added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public PlannedWorkingHours plus(PlannedWorkingHours plannedWorkingHours) {
        return new PlannedWorkingHours(duration().plus(plannedWorkingHours.duration()));
    }
}
