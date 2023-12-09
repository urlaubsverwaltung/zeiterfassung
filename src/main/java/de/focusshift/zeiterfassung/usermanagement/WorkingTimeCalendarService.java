package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserIdComposite;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface WorkingTimeCalendarService {

    Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimes(LocalDate from, LocalDate toExclusive);

    WorkingTimeCalendar getWorkingTimes(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId);

    Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimes(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds);
}
