package de.focusshift.zeiterfassung.settings;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumMap;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

/**
 * Global working-time defaults. Used to seed new users' working times and to configure
 * how the GitHub Activity time suggestion is calculated.
 *
 * @param workdays              default working hours per day of week
 * @param timeRoundingMinutes   suggested time is rounded up to the nearest multiple of this value (1–60)
 * @param minSuggestedMinutes   floor applied to the suggested time after rounding (1–480)
 */
public record WorkingTimeSettings(EnumMap<DayOfWeek, Duration> workdays,
                                   int timeRoundingMinutes,
                                   int minSuggestedMinutes) {

    static final Duration EIGHT_HOURS = Duration.ofHours(8);

    public static final int DEFAULT_TIME_ROUNDING_MINUTES = 5;
    public static final int DEFAULT_MIN_SUGGESTED_MINUTES = 15;

    public static final WorkingTimeSettings DEFAULT = new WorkingTimeSettings(
        defaultWorkdays(),
        DEFAULT_TIME_ROUNDING_MINUTES,
        DEFAULT_MIN_SUGGESTED_MINUTES
    );

    /** Mon–Fri 8 h, Sat–Sun 0 h. */
    public static EnumMap<DayOfWeek, Duration> defaultWorkdays() {
        final EnumMap<DayOfWeek, Duration> map = new EnumMap<>(DayOfWeek.class);
        map.put(MONDAY,    EIGHT_HOURS);
        map.put(TUESDAY,   EIGHT_HOURS);
        map.put(WEDNESDAY, EIGHT_HOURS);
        map.put(THURSDAY,  EIGHT_HOURS);
        map.put(FRIDAY,    EIGHT_HOURS);
        map.put(SATURDAY,  Duration.ZERO);
        map.put(SUNDAY,    Duration.ZERO);
        return map;
    }
}
