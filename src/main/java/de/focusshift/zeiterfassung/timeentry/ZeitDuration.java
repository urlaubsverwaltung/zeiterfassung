package de.focusshift.zeiterfassung.timeentry;

import java.math.BigDecimal;
import java.time.Duration;

import static java.math.RoundingMode.CEILING;

/**
 * Container of a {@linkplain Duration} to provide calculation stuff like {@code durationInMinutes}.
 *
 * <p>
 * This can be used within {@linkplain WorkDuration} or {@linkplain BreakDuration} for instance
 * as these objects provide more context about the actual duration.
 *
 * @param duration
 */
record ZeitDuration(Duration duration) {

    public Duration durationInMinutes() {
        final long seconds = duration.toSeconds();

        return seconds % 60 == 0
            ? duration
            : Duration.ofMinutes(duration.toMinutes() + 1);
    }

    public double hoursDoubleValue() {
        final long minutes = durationInMinutes().toMinutes();
        return minutesToHours(minutes);
    }

    private static double minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 3, CEILING).doubleValue();
    }
}
