package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.timeentry.PlannedWorkingHours;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
class WorkingTimeCalendarServiceImpl implements WorkingTimeCalendarService {

    private final WorkingTimeService workingTimeService;

    WorkingTimeCalendarServiceImpl(WorkingTimeService workingTimeService) {
        this.workingTimeService = workingTimeService;
    }

    @Override
    public WorkingTimeCalendar getWorkingTimeCalender(LocalDate from, LocalDate toExclusive, UserLocalId userLocalId) {
        return getWorkingTimeCalendarForUsers(from, toExclusive, List.of(userLocalId)).values().stream().toList().getFirst();
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForAllUsers(LocalDate from, LocalDate toExclusive) {
        return entitiesToWorkingTime(from, toExclusive, workingTimeService.getAllWorkingTimeByUsers());
    }

    @Override
    public Map<UserIdComposite, WorkingTimeCalendar> getWorkingTimeCalendarForUsers(LocalDate from, LocalDate toExclusive, Collection<UserLocalId> userLocalIds) {
        return entitiesToWorkingTime(from, toExclusive, workingTimeService.getWorkingTimeByUsers(userLocalIds));
    }

    private Map<UserIdComposite, WorkingTimeCalendar> entitiesToWorkingTime(LocalDate from, LocalDate toExclusive, Map<UserIdComposite, WorkingTime> workingTimes) {

        final HashMap<UserIdComposite, WorkingTimeCalendar> result = new HashMap<>();

        for (Map.Entry<UserIdComposite, WorkingTime> entry : workingTimes.entrySet()) {
            final WorkingTimeCalendar workingTimeCalendar = toWorkingTimeCalendar(from, toExclusive, entry.getValue());
            result.put(entry.getKey(), workingTimeCalendar);
        }

        return result;
    }

    private WorkingTimeCalendar toWorkingTimeCalendar(LocalDate from, LocalDate toExclusive, WorkingTime workingTime) {

        final Map<LocalDate, PlannedWorkingHours> plannedWorkingHoursByDate = new HashMap<>();

        LocalDate date = from;
        while (date.isBefore(toExclusive)) {
            final Duration duration = workingTime.getForDayOfWeek(date.getDayOfWeek()).map(WorkDay::duration).orElse(Duration.ZERO);
            plannedWorkingHoursByDate.put(date, new PlannedWorkingHours(duration));
            date = date.plusDays(1);
        }

        return new WorkingTimeCalendar(plannedWorkingHoursByDate);
    }
}
