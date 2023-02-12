package de.focusshift.zeiterfassung.timeentry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Defines the {@linkplain Duration} of a {@linkplain TimeEntry} without context. Could be working time or a break.
 * Use {@linkplain WorkDuration} or {@linkplain BreakDuration} for context.
 *
 * @param value
 */
record SimpleTimeEntryDuration(Duration value) implements TimeEntryDuration {

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

    private static double minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING).doubleValue();
    }
}
