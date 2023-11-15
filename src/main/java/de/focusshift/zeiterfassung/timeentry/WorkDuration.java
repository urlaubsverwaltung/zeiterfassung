package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of worked time. Different to {@linkplain BreakDuration}.
 */
public final class WorkDuration {

    public static final WorkDuration ZERO = new WorkDuration(Duration.ZERO);

    private final ZeitDuration zeitDuration;

    public WorkDuration(Duration value) {
        this(new ZeitDuration(value));
    }

    WorkDuration(ZeitDuration zeitDuration) {
        this.zeitDuration = zeitDuration;
    }

    public Duration duration() {
        return zeitDuration.duration();
    }

    public Duration durationInMinutes() {
        return zeitDuration.durationInMinutes();
    }

    public double hoursDoubleValue() {
        return zeitDuration.hoursDoubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkDuration that = (WorkDuration) o;
        return Objects.equals(zeitDuration, that.zeitDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zeitDuration);
    }

    @Override
    public String toString() {
        return "WorkDuration{" +
            "zeitDuration=" + zeitDuration +
            '}';
    }
}
