package de.focusshift.zeiterfassung.usermanagement;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;

import static java.math.RoundingMode.CEILING;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

/**
 * Defines working hours on a dayOfWeek. May be used with {@linkplain WorkingTime} to benefit from a user context.
 *
 * @param dayOfWeek
 * @param duration the exact duration. not rounded up to minutes.
 */
public record WorkDay(DayOfWeek dayOfWeek, Duration duration) {

    public BigDecimal hours() {
        return BigDecimal.valueOf(duration.toMinutes()).divide(BigDecimal.valueOf(60), 2, CEILING);
    }

    public static WorkDay monday(Duration hours) {
        return new WorkDay(MONDAY, hours);
    }

    public static WorkDay tuesday(Duration hours) {
        return new WorkDay(TUESDAY, hours);
    }

    public static WorkDay wednesday(Duration hours) {
        return new WorkDay(WEDNESDAY, hours);
    }

    public static WorkDay thursday(Duration hours) {
        return new WorkDay(THURSDAY, hours);
    }

    public static WorkDay friday(Duration hours) {
        return new WorkDay(FRIDAY, hours);
    }

    public static WorkDay saturday(Duration hours) {
        return new WorkDay(SATURDAY, hours);
    }

    public static WorkDay sunday(Duration hours) {
        return new WorkDay(SUNDAY, hours);
    }
}
