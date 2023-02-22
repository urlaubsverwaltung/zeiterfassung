package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides information about the {@link de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours} on a given {@link LocalDate} including publicHolidays.
 * For instance:
 * <ul>
 *     <li>2022-12-26 - 0h (publicHoliday, monday)</li>
 *     <li>2022-12-27 - 8h (tuesday)</li>
 *     <li>2022-12-28 - 8h (wednesday)</li>
 *     <li>2022-12-29 - 8h (thursday)</li>
 *     <li>2022-12-30 - 4h (friday)</li>
 * </ul>
 *
 * Should be used in combination with a {@link Map} to keep relation to a {@link User} for example.
 */
public final class WorkingTimeCalendar {

    private final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate;

    public WorkingTimeCalendar(Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate) {
        this.plannedWorkingHoursByDate = plannedWorkingHoursByDate;
    }

    public Optional<PlannedWorkingHours> plannedWorkingHours(LocalDate date) {
        return Optional.ofNullable(plannedWorkingHoursByDate.get(date));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingTimeCalendar that = (WorkingTimeCalendar) o;
        return plannedWorkingHoursByDate.equals(that.plannedWorkingHoursByDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plannedWorkingHoursByDate);
    }

    @Override
    public String toString() {
        return "WorkingTimeCalendar{" +
            "plannedWorkingHoursByDate=" + plannedWorkingHoursByDate +
            '}';
    }
}