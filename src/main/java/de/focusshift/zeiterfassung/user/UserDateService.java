package de.focusshift.zeiterfassung.user;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjuster;

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
     * @param localDate  date to adjust
     * @return the adjusted {@link LocalDate} which matches the user's desired firstDayOfWeek
     */
    LocalDate localDateToFirstDateOfWeek(LocalDate localDate);

    /**
     * Adjust the given date into the user's firstDayOfWeek.
     * This method should not be used while iterating over dates. Use {@link #localDateToFirstDateOfWeekAdjuster} instead.
     *
     * @param year  the year to represent, not null
     * @param weekOfYear  the week-of-week-based-year to represent, from 1 to 53
     * @return the adjusted {@link LocalDate} which matches the user's desired firstDayOfWeek
     *
     * @throws DateTimeException if the weekOfYear value is invalid
     */
    LocalDate firstDayOfWeek(Year year, int weekOfYear);
}
