package de.focusshift.zeiterfassung.timeentry;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a {@linkplain Duration} of worked time. Different to {@linkplain BreakDuration}.
 */
public final class WorkDuration {

    public static final WorkDuration ZERO = new WorkDuration(Duration.ZERO);

    private final SimpleTimeEntryDuration timeEntryDuration;

    public WorkDuration(Duration value) {
        this(new SimpleTimeEntryDuration(value));
    }

    WorkDuration(SimpleTimeEntryDuration timeEntryDuration) {
        this.timeEntryDuration = timeEntryDuration;
    }

    public Duration value() {
        return timeEntryDuration.value();
    }

    public Duration minutes() {
        return timeEntryDuration.minutes();
    }

    public double hoursDoubleValue() {
        return timeEntryDuration.hoursDoubleValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkDuration that = (WorkDuration) o;
        return Objects.equals(timeEntryDuration, that.timeEntryDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeEntryDuration);
    }

    @Override
    public String toString() {
        return "WorkDuration{" +
            "timeEntryDuration=" + timeEntryDuration +
            '}';
    }
}
