package de.focusshift.zeiterfassung.usermanagement;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface WorkingTimeCalendarService {

    Map<UserLocalId, WorkingTimeCalendar> getWorkingTimes(LocalDate from, LocalDate toExclusive);

    Map<UserLocalId, WorkingTimeCalendar> getWorkingTimes(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds);
}
