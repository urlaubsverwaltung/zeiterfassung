package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of break time. Different to {@linkplain WorkDuration}.
 */
public final class BreakDuration implements ZeitDuration {

    private final Duration duration;

    public BreakDuration(Duration duration) {
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
        BreakDuration that = (BreakDuration) o;
        return Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration);
    }

    @Override
    public String toString() {
        return "BreakDuration{" +
            "duration=" + duration +
            '}';
    }
}
