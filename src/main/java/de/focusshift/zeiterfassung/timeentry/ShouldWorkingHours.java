package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.absence.Absence;
import de.focusshift.zeiterfassung.workingtime.PlannedWorkingHours;
import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;

/**
 * Hours that should be worked.
 *
 * <p>
 * (e.g. {@linkplain PlannedWorkingHours} 40h - {@linkplain Absence} 8h = ShouldWorkingHours 32h)
 *
 * @param duration the exact duration. not rounded up to minutes.
 */
public record ShouldWorkingHours(Duration duration) implements ZeitDuration {

    public static final ShouldWorkingHours ZERO = new ShouldWorkingHours(Duration.ZERO);
    public static final ShouldWorkingHours EIGHT = new ShouldWorkingHours(Duration.ofHours(8));

    /**
     * Returns a copy of this shouldWorkingHours with the specified shouldWorkingHours added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param shouldWorkingHours  the shouldWorkingHours to add, not null
     * @return a {@linkplain  ShouldWorkingHours} based on this shouldWorkingHours with the specified shouldWorkingHours added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public ShouldWorkingHours plus(ShouldWorkingHours shouldWorkingHours) {
        return new ShouldWorkingHours(duration().plus(shouldWorkingHours.duration()));
    }
}
