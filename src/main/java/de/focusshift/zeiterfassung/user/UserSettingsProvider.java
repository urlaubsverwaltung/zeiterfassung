package de.focusshift.zeiterfassung.user;

import java.time.DayOfWeek;
import java.time.ZoneId;

public interface UserSettingsProvider {

    DayOfWeek firstDayOfWeek();

    ZoneId zoneId();
}
