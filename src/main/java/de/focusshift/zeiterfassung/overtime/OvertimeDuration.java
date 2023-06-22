package de.focusshift.zeiterfassung.overtime;

import de.focusshift.zeiterfassung.timeentry.TimeEntryDuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

public final class OvertimeDuration implements TimeEntryDuration {

    public static OvertimeDuration ZERO = new OvertimeDuration(Duration.ZERO);

    private final Duration value;

    public OvertimeDuration(Duration value) {
        this.value = value;
    }

    @Override
    public Duration value() {
        return value;
    }

    @Override
    public Duration minutes() {
        final long seconds = value.toSeconds();

        return seconds % 60 == 0
            ? value
            : Duration.ofMinutes(value.toMinutes() + 1);
    }

    @Override
    public double hoursDoubleValue() {
        final long minutes = minutes().toMinutes();
        return minutesToHours(minutes);
    }

    public OvertimeDuration plus(Duration duration) {
        return new OvertimeDuration(value.plus(duration));
    }

    public OvertimeDuration plus(OvertimeDuration overtimeDuration) {
        return new OvertimeDuration(value.plus(overtimeDuration.value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OvertimeDuration that = (OvertimeDuration) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OvertimeDuration{" +
            "value=" + value +
            '}';
    }

    private static double minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING).doubleValue();
    }
}
