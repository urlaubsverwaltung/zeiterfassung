package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of worked time. Different to {@linkplain BreakDuration}.
 */
public final class WorkDuration implements ZeitDuration {

    public static final WorkDuration ZERO = new WorkDuration(Duration.ZERO);

    private final Duration duration;

    public WorkDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public Duration duration() {
        return duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkDuration that = (WorkDuration) o;
        return Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration);
    }

    @Override
    public String toString() {
        return "WorkDuration{" +
            "duration=" + duration +
            '}';
    }
}
