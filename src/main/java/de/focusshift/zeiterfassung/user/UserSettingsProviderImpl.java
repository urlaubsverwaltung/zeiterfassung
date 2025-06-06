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
        // once this value becomes dynamic,
        // we need to consider handling of DayLockedEvent and publishing OvertimeRabbitEvent.
        // whether a day is locked or not depends on the zoneId. a date can be locked in Berlin, but not in Los Angeles yet.
        // furthermore check current usage and maybe refactor method signatures since zoneId point ov view is not correct anymore.
        return EUROPE_BERLIN;
    }
}
