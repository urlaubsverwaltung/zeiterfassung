package de.focusshift.zeiterfassung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Service
class PublicHolidaysServiceImpl implements PublicHolidaysService {

    private final Map<String, HolidayManager> holidayManagers;

    PublicHolidaysServiceImpl(Map<String, HolidayManager> holidayManagers) {
        this.holidayManagers = holidayManagers;
    }

    @Override
    public Map<FederalState, PublicHolidayCalendar> getPublicHolidays(LocalDate from, LocalDate toExclusive, Collection<FederalState> federalStates) {

        final LocalDate to = toExclusive.minusDays(1);
        final Map<FederalState, PublicHolidayCalendar> calendar = new EnumMap<>(FederalState.class);

        for (FederalState federalState : federalStates) {
            final Map<LocalDate, List<PublicHoliday>> holidays;
            if (federalState == FederalState.NONE) {
                holidays = Map.of();
            } else {
                final HolidayManager holidayManager = holidayManagers.get(federalState.getCountry());
                holidays = holidayManager.getHolidays(from, to, federalState.getCodes())
                    .stream()
                    .map(holiday -> new PublicHoliday(holiday.getDate(), holiday::getDescription))
                    .collect(groupingBy(PublicHoliday::date));
            }
            calendar.put(federalState, new PublicHolidayCalendar(federalState, holidays));
        }

        return calendar;
    }
}
