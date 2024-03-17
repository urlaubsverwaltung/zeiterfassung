package de.focusshift.zeiterfassung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import de.focusshift.zeiterfassung.CachedSupplier;
import de.focusshift.zeiterfassung.settings.FederalStateSettingsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;

@Service
class PublicHolidaysServiceImpl implements PublicHolidaysService {

    private final Map<String, HolidayManager> holidayManagers;
    private final FederalStateSettingsService federalStateSettingsService;

    PublicHolidaysServiceImpl(Map<String, HolidayManager> holidayManagers, FederalStateSettingsService federalStateSettingsService) {
        this.holidayManagers = holidayManagers;
        this.federalStateSettingsService = federalStateSettingsService;
    }

    @Override
    public Map<FederalState, PublicHolidayCalendar> getPublicHolidays(LocalDate from, LocalDate toExclusive, Collection<FederalState> federalStates) {

        final LocalDate to = toExclusive.minusDays(1);
        final Map<FederalState, PublicHolidayCalendar> calendar = new EnumMap<>(FederalState.class);

        final Supplier<FederalState> globalFederalStateSupplier =
            new CachedSupplier<>(() -> federalStateSettingsService.getFederalStateSettings().federalState());

        for (FederalState federalState : federalStates) {
            federalState = FederalState.GLOBAL.equals(federalState) ? globalFederalStateSupplier.get() : federalState;
            calendar.put(federalState, new PublicHolidayCalendar(federalState, holidays(from, to, federalState)));
        }

        return calendar;
    }

    private Map<LocalDate, List<PublicHoliday>> holidays(LocalDate from, LocalDate to, FederalState federalState) {

        if (FederalState.NONE.equals(federalState)) {
            return Map.of();
        }

        final HolidayManager holidayManager = holidayManagers.get(federalState.getCountry());
        return holidayManager.getHolidays(from, to, federalState.getCodes())
            .stream()
            .map(holiday -> new PublicHoliday(holiday.getDate(), holiday::getDescription))
            .collect(groupingBy(PublicHoliday::date));
    }
}
