package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of break time. Different to {@linkplain WorkDuration}.
 */
public final class BreakDuration implements TimeEntryDuration {

    private final SimpleTimeEntryDuration timeEntryDuration;

    public BreakDuration(Duration value) {
        this(new SimpleTimeEntryDuration(value));
    }

    BreakDuration(SimpleTimeEntryDuration timeEntryDuration) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakDuration that = (BreakDuration) o;
        return Objects.equals(timeEntryDuration, that.timeEntryDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeEntryDuration);
    }

    @Override
    public String toString() {
        return "BreakDuration{" +
            "timeEntryDuration=" + timeEntryDuration +
            '}';
    }
}
