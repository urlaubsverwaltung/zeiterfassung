package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of break time. Different to {@linkplain WorkDuration}.
 */
public final class BreakDuration {

    private final ZeitDuration zeitDuration;

    public BreakDuration(Duration value) {
        this(new ZeitDuration(value));
    }

    BreakDuration(ZeitDuration zeitDuration) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakDuration that = (BreakDuration) o;
        return Objects.equals(zeitDuration, that.zeitDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zeitDuration);
    }

    @Override
    public String toString() {
        return "BreakDuration{" +
            "zeitDuration=" + zeitDuration +
            '}';
    }
}
