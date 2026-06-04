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
 * Global working-time defaults. Used to seed new users' working times.
 * Can be overridden for an individual person via their working-time entries.
 *
 * @param workdays default working hours per day of week
 */
public record WorkingTimeSettings(EnumMap<DayOfWeek, Duration> workdays) {

    static final Duration EIGHT_HOURS = Duration.ofHours(8);

    public static final WorkingTimeSettings DEFAULT = new WorkingTimeSettings(defaultWorkdays());

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
