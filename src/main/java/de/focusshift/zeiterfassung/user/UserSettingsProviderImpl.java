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
        // once this value becomes dynamic, we need to consider handling of DayLockedEvent and publishing OvertimeRabbitEvent.
        // whether a day is locked or not depends on the zoneId. a date can be locked in Berlin, but not in Los Angeles yet.
        //
        // furthermore check current usage and maybe refactor method signatures since zoneId point of view is not correct anymore.
        // there are two options to show dates to the user:
        // - A: "09:00 means 09:00 forever." Historical record is faithful to entry moment.
        //      But two entries made in different zones can look overlapping/out-of-order on the clock even though instants are fine.
        //      (maybe show TZ next to TimeEntry?)
        // - B: "everything shown in my current zone." Consistent single timeline, good for a traveler comparing across trips
        //      but rewrites the wall-clock of past entries and can move them across day boundaries.
        //
        return EUROPE_BERLIN;
    }
}
