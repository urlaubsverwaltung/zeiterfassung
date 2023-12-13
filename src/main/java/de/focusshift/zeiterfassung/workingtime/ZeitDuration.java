package de.focusshift.zeiterfassung.workingtime;

import java.math.BigDecimal;
import java.time.Duration;

import static java.math.RoundingMode.CEILING;

/**
 * Container of a {@linkplain Duration} to provide calculation stuff like {@linkplain  ZeitDuration#hoursDoubleValue()}
 * which has to have a prevision of three digits.
 *
 * <p>
 * This interface can be used with more specialized Duration objects like {@linkplain PlannedWorkingHours} as these
 * objects provide more context about the actual duration.
 */
public interface ZeitDuration {

    /**
     * Return the exact duration. (e.g {@code "PT50M42S"})
     * Consider {@linkplain ZeitDuration#durationInMinutes()} if you are interested in a value rounded to minutes.
     *
     * @return the exact duration value
     */
    Duration duration();

    /**
     * Returns a new Duration rounded up to full minutes (e.g. {@code "PT30S"} -> {@code "PT1M"}).
     *
     * @return duration rounded up to full minutes
     */
    default Duration durationInMinutes() {
        final Duration duration = duration();
        final long seconds = duration.toSeconds();

        return seconds % 60 == 0
            ? duration
            : Duration.ofMinutes(duration.toMinutes() + 1);
    }

    /**
     * Calculate the hour representation of this duration as double value with the precision of three digits.
     * The duration will be rounded up to full minutes.
     *
     * <p>
     *     e.g. {@code "PT2H20S"} -> {@code "PT2H1M"} -> {@code 2.017}
     * </p>
     *
     * @return duration as double value considering hours as base.
     */
    default double hoursDoubleValue() {
        final long minutes = durationInMinutes().toMinutes();
        return minutesToHours(minutes);
    }

    /**
     * Create an anonymous instance of {@linkplain ZeitDuration} without context of anything. Use this in tests
     * or when just a calculation to minutes is required for instance.
     *
     * @param duration to use for calculation
     * @return anonymous instance of {@linkplain ZeitDuration} of the given duration.
     */
    static ZeitDuration of(Duration duration) {
        return () -> duration;
    }

    private static double minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 3, CEILING).doubleValue();
    }
}
