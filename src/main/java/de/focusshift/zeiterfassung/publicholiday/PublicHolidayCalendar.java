package de.focusshift.zeiterfassung.publicholiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides information about {@linkplain PublicHoliday} for a {@linkplain FederalState} and a certain date range.
 *
 * @param federalState
 * @param publicHolidays
 */
public record PublicHolidayCalendar(FederalState federalState, Map<LocalDate, List<PublicHoliday>> publicHolidays) {

    public boolean isPublicHoliday(LocalDate localDate) {
        return publicHolidays.containsKey(localDate);
    }

    public Optional<List<PublicHoliday>> getPublicHoliday(LocalDate localDate) {
        return Optional.ofNullable(publicHolidays.get(localDate));
    }
}
