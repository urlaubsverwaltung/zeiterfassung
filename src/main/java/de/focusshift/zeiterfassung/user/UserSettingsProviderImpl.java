package de.focusshift.zeiterfassung.user;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.ZoneId;

@Service
class UserSettingsProviderImpl implements UserSettingsProvider {

    private static final ZoneId EUROPE_BERLIN = ZoneId.of("Europe/Berlin");

    @Override
    public DayOfWeek firstDayOfWeek() {
        return DayOfWeek.MONDAY;
    }

    @Override
    public ZoneId zoneId() {
        return EUROPE_BERLIN;
    }
}
