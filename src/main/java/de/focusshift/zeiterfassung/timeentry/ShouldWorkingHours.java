package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Hours that should be worked. (e.g. PlannedWorkingHours 40h - Absence 8h = ShouldWorkingHours 32h)
 */
public final class ShouldWorkingHours {

    public static final ShouldWorkingHours ZERO = new ShouldWorkingHours(Duration.ZERO);
    public static final ShouldWorkingHours EIGHT = new ShouldWorkingHours(Duration.ofHours(8));

    private final ZeitDuration zeitDuration;

    public ShouldWorkingHours(Duration duration) {
        this.zeitDuration = new ZeitDuration(duration);
    }

    public Duration duration() {
        return zeitDuration.duration();
    }

    public Duration minutes() {
        return zeitDuration.durationInMinutes();
    }

    public double hoursDoubleValue() {
        return zeitDuration.hoursDoubleValue();
    }

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
        return new ShouldWorkingHours(this.duration().plus(shouldWorkingHours.minutes()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShouldWorkingHours that = (ShouldWorkingHours) o;
        return Objects.equals(zeitDuration, that.zeitDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zeitDuration);
    }

    @Override
    public String toString() {
        return "ShouldWorkingHours{" +
            "zeitDuration=" + zeitDuration +
            '}';
    }
}
