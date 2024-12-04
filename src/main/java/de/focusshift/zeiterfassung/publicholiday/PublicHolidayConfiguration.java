package de.focusshift.zeiterfassung.publicholiday;

import de.focus_shift.jollyday.core.HolidayManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static de.focus_shift.jollyday.core.HolidayManager.getInstance;
import static de.focus_shift.jollyday.core.ManagerParameters.create;
import static java.util.stream.Collectors.toMap;

@Configuration
class PublicHolidayConfiguration {

    private static final List<String> COUNTRIES = List.of("de", "at", "ch", "gb", "gr", "mt", "it", "hr", "es", "nl", "lt", "be", "pl", "us");

    @Bean
    Map<String, HolidayManager> holidayManagerMap() {
        return COUNTRIES.stream()
            .map(country -> getInstance(create(country)))
            .collect(toMap(holidayManager -> holidayManager.getManagerParameter().getDisplayName(), Function.identity()));
    }
}
