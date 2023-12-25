package de.focusshift.zeiterfassung.publicholiday;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface PublicHolidaysService {

     Map<FederalState, PublicHolidayCalendar> getPublicHolidays(LocalDate from, LocalDate toExclusive, Collection<FederalState> federalStates);
}
