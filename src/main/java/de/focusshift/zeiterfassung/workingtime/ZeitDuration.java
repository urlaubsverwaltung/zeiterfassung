package de.focusshift.zeiterfassung.workingtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

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
     * Calculate the hour representation of this duration as double value with the precision of three digits and
     * returns this "Industrieminuten".
     *
     * <p>
     * An industrial minute corresponds to 1/100 of a working hour, i.e., 36 seconds.
     * This makes it easier to calculate working times, cycle times, and costs in manufacturing,
     * because you work with decimal numbers instead of hours:minutes.
     *
     * <p>
     * Rounding for smaller fraction than 3 is done with {@linkplain RoundingMode#HALF_UP}.
     *
     * <p>
     * Examples:
     * <ul>
     * <li>0.25 hours (industrial) = PT15M = 15 minutes</li>
     * <li>0.12 hours (industrial) = PT7M2S = 7 minutes and 2 seconds</li>
     * </ul>
     *
     * @return duration as double value considering hours as base known as "Industrieminute"
     *
     * @see <a href="https://de.wikipedia.org/wiki/Industrieminute">Industrieminute</a>
     */
    default double hoursDoubleValue() {
        final double hoursPart = duration().toHours();
        final double minutesPart = new BigDecimal(duration().toMinutesPart())
            .divide(new BigDecimal(60), 3, RoundingMode.HALF_UP)
            .doubleValue();
        final double secondsPart = new BigDecimal(duration().toSecondsPart())
            .divide(new BigDecimal(3600), 3, RoundingMode.HALF_UP)
            .doubleValue();
        return hoursPart + minutesPart + secondsPart;
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
}
