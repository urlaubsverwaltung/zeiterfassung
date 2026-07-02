package de.focusshift.zeiterfassung.user;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjuster;
import java.util.List;

public interface UserDateService {

    /**
     * Create a {@link TemporalAdjuster} to adjust a temporal into the user's first day of week.
     * This method is useful when iterating over dates that have to be adjusted since the user setting is requested only once.
     *
     * @return a {@link TemporalAdjuster}
     */
    TemporalAdjuster localDateToFirstDateOfWeekAdjuster();

    /**
     * Adjust the given date into the user's firstDayOfWeek.
     * This method should not be used while iterating over dates. Use {@link #localDateToFirstDateOfWeekAdjuster} instead.
     *
     * @param localDate date to adjust
     * @return the adjusted {@link LocalDate} which matches the user's desired firstDayOfWeek
     */
    LocalDate localDateToFirstDateOfWeek(LocalDate localDate);

    /**
     * Adjust the given date into the user's firstDayOfWeek.
     * This method should not be used while iterating over dates. Use {@link #localDateToFirstDateOfWeekAdjuster} instead.
     *
     * @param year       the year to represent, not null
     * @param weekOfYear the week-of-week-based-year to represent, from 1 to 53
     * @return the adjusted {@link LocalDate} which matches the user's desired firstDayOfWeek
     * @throws DateTimeException if the weekOfYear value is invalid
     */
    LocalDate firstDayOfWeek(Year year, int weekOfYear);

    /**
     * Returns the dates that start a week within the given month, based on the user's firstDayOfWeek.
     *
     * <p>
     * The first element is always the first day of the month, even when it is not the user's firstDayOfWeek (e.g. monday).
     * Each following element is a date matching the user's firstDayOfWeek that still falls within the month.
     *
     * <p>
     * Weeks are therefore clipped to the month boundaries and not extended into the previous or next month.
     *
     * @param yearMonth the month to compute the start-of-week dates for, not null
     * @return ordered list of start-of-week {@link LocalDate}s within the given {@code yearMonth}
     */
    List<LocalDate> getStartOfWeekDatesForMonth(YearMonth yearMonth);

    /**
     * The current date from the user's perspective, resolved with the user's {@link java.time.ZoneId}.
     *
     * <p>
     * The application {@link java.time.Clock} runs in UTC. This method projects "now" into the user's timezone,
     * so a user in {@code Europe/Berlin} shortly after midnight sees the already-started day and not the previous
     * UTC day.
     *
     * @return today's {@link LocalDate} in the user's timezone
     */
    LocalDate today();
}
