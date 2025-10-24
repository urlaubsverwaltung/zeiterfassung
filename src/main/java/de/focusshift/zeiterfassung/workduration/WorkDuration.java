package de.focusshift.zeiterfassung.workduration;

import de.focusshift.zeiterfassung.timeentry.BreakDuration;
import de.focusshift.zeiterfassung.workingtime.ZeitDuration;

import java.time.Duration;

/**
 * Defines the calculated {@linkplain Duration} of worked time. Different to {@linkplain BreakDuration}.
 *
 * <p>
 * Note that this does not represent the actually booked working time. Breaks and overlapping time entry intervals
 * are considered in the calculation of the {@link WorkDuration}.
 *
 * <p>
 * Examples:
 *
 * <ul>
 *     <li>Two work intervals both starting at 08:00 and ending at 10:00 -> {@link WorkDuration} of 2 hours</li>
 *     <li>One work interval and one break interval starting and ending at the same time -> {@link WorkDuration} of 0 hours</li>
 * </ul>
 *
 * @param duration the exact duration. not rounded up to minutes.
 */
public record WorkDuration(Duration duration) implements ZeitDuration {

    public static final WorkDuration ZERO = new WorkDuration(Duration.ZERO);
    public static final WorkDuration EIGHT = new WorkDuration(Duration.ofHours(8));

    /**
     * Returns a copy of this workDuration with the specified workDuration added.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @param workDuration  the workDuration to add, not null
     * @return a {@code WorkDuration} based on this workDuration with the specified workDuration added, not null
     * @throws ArithmeticException if numeric overflow occurs
     */
    public WorkDuration plus(WorkDuration workDuration) {
        return new WorkDuration(duration().plus(workDuration.duration()));
    }
}
