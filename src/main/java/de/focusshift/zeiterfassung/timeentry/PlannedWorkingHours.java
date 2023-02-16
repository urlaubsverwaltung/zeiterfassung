package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of planned working hours.
 */
public final class PlannedWorkingHours implements TimeEntryDuration {

    public static final PlannedWorkingHours ZERO = new PlannedWorkingHours(Duration.ZERO);
    public static final PlannedWorkingHours EIGHT = new PlannedWorkingHours(Duration.ofHours(8));

    private final SimpleTimeEntryDuration timeEntryDuration;

    public PlannedWorkingHours(Duration value) {
        this(new SimpleTimeEntryDuration(value));
    }

    PlannedWorkingHours(SimpleTimeEntryDuration timeEntryDuration) {
        this.timeEntryDuration = timeEntryDuration;
    }

    @Override
    public Duration value() {
        return timeEntryDuration.value();
    }

    /**
     * @return work value rounded up to full minutes.
     */
    @Override
    public Duration minutes() {
        return timeEntryDuration.minutes();
    }

    @Override
    public double hoursDoubleValue() {
        return timeEntryDuration.hoursDoubleValue();
    }

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
        return new PlannedWorkingHours(this.value().plus(plannedWorkingHours.minutes()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlannedWorkingHours that = (PlannedWorkingHours) o;
        return timeEntryDuration.equals(that.timeEntryDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeEntryDuration);
    }

    @Override
    public String toString() {
        return "PlannedWorkingHours{" +
            "value=" + timeEntryDuration.value() +
            '}';
    }
}