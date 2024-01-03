package de.focusshift.zeiterfassung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.focus_shift.jollyday.core.HolidayManager.getInstance;
import static de.focus_shift.jollyday.core.ManagerParameters.create;

@Configuration
class PublicHolidayConfiguration {

    private static final List<String> COUNTRIES = List.of("de", "at", "ch", "gb", "gr", "mt", "it", "hr", "es", "nl");

    @Bean
    Map<String, HolidayManager> holidayManagerMap() {
        final Map<String, HolidayManager> countryMap = new HashMap<>();
        COUNTRIES.forEach(country -> countryMap.put(country, getInstance(create(country))));
        return countryMap;
    }
}
