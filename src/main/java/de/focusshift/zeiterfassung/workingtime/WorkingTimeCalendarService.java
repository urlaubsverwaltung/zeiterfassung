package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface WorkingTimeCalendarService {

    WorkingTimeCalendar getWorkingTimeCalender(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId);

    Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForAllUsers(LocalDate from, LocalDate toExclusive);

    Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds);
}
