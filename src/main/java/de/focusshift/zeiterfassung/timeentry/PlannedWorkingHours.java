package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of planned working hours.
 */
public final class PlannedWorkingHours {

    public static final PlannedWorkingHours ZERO = new PlannedWorkingHours(Duration.ZERO);
    public static final PlannedWorkingHours EIGHT = new PlannedWorkingHours(Duration.ofHours(8));

    private final ZeitDuration zeitDuration;

    public PlannedWorkingHours(Duration value) {
        this(new ZeitDuration(value));
    }

    PlannedWorkingHours(ZeitDuration zeitDuration) {
        this.zeitDuration = zeitDuration;
    }

    public Duration duration() {
        return zeitDuration.duration();
    }

    /**
     * @return work value rounded up to full minutes.
     */
    public Duration minutes() {
        return zeitDuration.durationInMinutes();
    }

    public double hoursDoubleValue() {
        return zeitDuration.hoursDoubleValue();
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
        return new PlannedWorkingHours(this.duration().plus(plannedWorkingHours.minutes()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlannedWorkingHours that = (PlannedWorkingHours) o;
        return zeitDuration.equals(that.zeitDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zeitDuration);
    }

    @Override
    public String toString() {
        return "PlannedWorkingHours{" +
            "value=" + zeitDuration.duration() +
            '}';
    }
}
