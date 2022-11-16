package de.focusshift.zeiterfassung.timeentry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public record WorkDuration(Duration duration) {

    /**
     * @return work duration rounded up to full minutes.
     */
    public WorkDuration minutes() {
        final long seconds = duration.toSeconds();

        final Duration durationMinutes = seconds % 60 == 0
            ? duration
            : Duration.ofMinutes(duration.toMinutes() + 1);

        return new WorkDuration(durationMinutes);
    }

    public double hoursDoubleValue() {
        final long minutes = minutes().duration().toMinutes();
        return minutesToHours(minutes);
    }

    private static double minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING).doubleValue();
    }
}
